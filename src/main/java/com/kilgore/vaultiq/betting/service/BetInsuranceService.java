package com.kilgore.vaultiq.betting.service;

import com.kilgore.vaultiq.betting.dto.BetInsuranceResponse;
import com.kilgore.vaultiq.betting.entity.*;
import com.kilgore.vaultiq.betting.repository.BetInsuranceRepository;
import com.kilgore.vaultiq.betting.repository.BetRepository;
import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.service.UserService;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Bet Insurance — optional insurance on single bets.
 *
 * Premium formula:  premiumRate = min(0.15, 0.03 + (odds - 1.5) * 0.02)
 *                   premium = stake * premiumRate
 *
 * Refund on loss:   refundPercentage = (1 - premiumRate) * 50%
 *                   refundAmount = stake * refundPercentage / 100
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BetInsuranceService {

    private final BetInsuranceRepository insuranceRepository;
    private final BetRepository betRepository;
    private final WalletService walletService;
    private final UserService userService;

    @Transactional
    public BetInsuranceResponse insureBet(UUID betId) {
        User user = userService.getCurrentUser();

        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new ResourceNotFoundException("Bet", "id", betId));

        if (!bet.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not own this bet");
        }

        if (bet.getStatus() != BetStatus.PENDING) {
            throw new BadRequestException("Can only insure PENDING bets");
        }

        if (insuranceRepository.existsByBetId(betId)) {
            throw new BadRequestException("This bet is already insured");
        }

        // Calculate premium based on odds
        BigDecimal odds = bet.getOddsAtPlacement();
        BigDecimal premiumRate = calculatePremiumRate(odds);
        BigDecimal premium = bet.getStake().multiply(premiumRate).setScale(2, RoundingMode.HALF_UP);

        // Refund percentage: higher premium → lower refund, but reasonable protection
        BigDecimal refundPercentage = BigDecimal.ONE.subtract(premiumRate)
                .multiply(new BigDecimal("50"))
                .setScale(2, RoundingMode.HALF_UP);

        // Debit premium from wallet
        walletService.debit(
                user.getId(),
                premium,
                TxnType.INSURANCE_PREMIUM,
                betId,
                "BET_INSURANCE",
                String.format("Insurance premium for bet %s — rate %.2f%%", betId, premiumRate.multiply(new BigDecimal("100")))
        );

        BetInsurance insurance = BetInsurance.builder()
                .bet(bet)
                .premium(premium)
                .refundPercentage(refundPercentage)
                .build();

        insurance = insuranceRepository.save(insurance);

        log.info("Bet {} insured — premium {}, refund percentage {}%",
                betId, premium, refundPercentage);

        return mapToResponse(insurance);
    }

    /**
     * Process insurance refund when a bet is lost.
     * Called from BetSettlementService after marking bet as LOST.
     */
    @Transactional
    public void processInsuranceRefund(UUID betId) {
        insuranceRepository.findByBetId(betId).ifPresent(insurance -> {
            if (insurance.isClaimed()) return;

            Bet bet = insurance.getBet();
            BigDecimal refundAmount = bet.getStake()
                    .multiply(insurance.getRefundPercentage())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            insurance.setRefundAmount(refundAmount);
            insurance.setClaimed(true);
            insuranceRepository.save(insurance);

            walletService.credit(
                    bet.getUser().getId(),
                    refundAmount,
                    TxnType.INSURANCE_REFUND,
                    betId,
                    "BET_INSURANCE",
                    String.format("Insurance refund: %s (%.2f%% of stake %s)",
                            refundAmount, insurance.getRefundPercentage(), bet.getStake())
            );

            log.info("Insurance refund processed for bet {} — amount {}", betId, refundAmount);
        });
    }

    @Transactional(readOnly = true)
    public BetInsuranceResponse getInsurance(UUID betId) {
        BetInsurance insurance = insuranceRepository.findByBetId(betId)
                .orElseThrow(() -> new ResourceNotFoundException("BetInsurance", "betId", betId));
        return mapToResponse(insurance);
    }

    // ── Helpers ──

    private BigDecimal calculatePremiumRate(BigDecimal odds) {
        // premiumRate = min(0.15, 0.03 + (odds - 1.5) * 0.02)
        BigDecimal rate = new BigDecimal("0.03")
                .add(odds.subtract(new BigDecimal("1.5"))
                        .multiply(new BigDecimal("0.02")));

        BigDecimal maxRate = new BigDecimal("0.15");
        BigDecimal minRate = new BigDecimal("0.03");

        if (rate.compareTo(maxRate) > 0) rate = maxRate;
        if (rate.compareTo(minRate) < 0) rate = minRate;

        return rate.setScale(4, RoundingMode.HALF_UP);
    }

    private BetInsuranceResponse mapToResponse(BetInsurance insurance) {
        return BetInsuranceResponse.builder()
                .id(insurance.getId())
                .betId(insurance.getBet().getId())
                .premium(insurance.getPremium())
                .refundPercentage(insurance.getRefundPercentage())
                .refundAmount(insurance.getRefundAmount())
                .claimed(insurance.isClaimed())
                .createdAt(insurance.getCreatedAt())
                .build();
    }
}
