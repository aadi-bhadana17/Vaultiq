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

    // Configuration Constants
    private static final BigDecimal MIN_INSURANCE_EDGE = new BigDecimal("1.03"); // 3% margin at ~0% coverage
    private static final BigDecimal MAX_INSURANCE_EDGE = new BigDecimal("1.30"); // 30% margin at 100% coverage
    private static final BigDecimal MIN_COVERAGE_PERCENT = new BigDecimal("10.00"); // 10% minimum
    private static final BigDecimal MAX_COVERAGE_PERCENT = new BigDecimal("100.00"); // 100% maximum

    @Transactional
    public BetInsuranceResponse insureBet(UUID betId, BigDecimal coveragePercentage) {
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

        if (coveragePercentage.compareTo(MIN_COVERAGE_PERCENT) < 0 || coveragePercentage.compareTo(MAX_COVERAGE_PERCENT) > 0) {
            throw new BadRequestException("Coverage must be between 10% and 100%");
        }

        BigDecimal odds = bet.getOddsAtPlacement();
        BigDecimal stake = bet.getStake();

        // 1. Coverage Ratio (C)
        BigDecimal c = coveragePercentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        // 2. True Probability of Losing
        // Reverse engineer true prob by stripping the 1.05 odds engine margin
        BigDecimal oddsWithMargin = odds.multiply(new BigDecimal("1.05"));
        BigDecimal probWin = BigDecimal.ONE.divide(oddsWithMargin, 4, RoundingMode.HALF_UP);
        BigDecimal probLose = BigDecimal.ONE.subtract(probWin);

        // 3. Dynamic Edge: Edge(C) = MIN_EDGE + C * (MAX_EDGE - MIN_EDGE)
        BigDecimal edgeDiff = MAX_INSURANCE_EDGE.subtract(MIN_INSURANCE_EDGE);
        BigDecimal dynamicEdge = MIN_INSURANCE_EDGE.add(c.multiply(edgeDiff));

        // 4. Premium = Expected Payout * Dynamic Edge
        BigDecimal expectedPayout = stake.multiply(c).multiply(probLose);
        BigDecimal premium = expectedPayout.multiply(dynamicEdge).setScale(2, RoundingMode.HALF_UP);

        // 5. Max Refund
        BigDecimal refundAmount = stake.multiply(c).setScale(2, RoundingMode.HALF_UP);

        // Safety cap: Premium should never exceed the refund they get
        if (premium.compareTo(refundAmount) >= 0) {
            throw new BadRequestException("Premium exceeds potential refund at this coverage level due to high mathematical risk. Please lower your coverage.");
        }

        // Debit premium from wallet
        walletService.debit(
                user.getId(),
                premium,
                TxnType.INSURANCE_PREMIUM,
                betId,
                "BET_INSURANCE",
                String.format("Insurance premium for bet %s (%.2f%% coverage)", betId, coveragePercentage)
        );

        BetInsurance insurance = BetInsurance.builder()
                .bet(bet)
                .premium(premium)
                .refundPercentage(coveragePercentage)
                .build();

        insurance = insuranceRepository.save(insurance);

        log.info("Bet {} insured — premium {}, refund percentage {}%",
                betId, premium, coveragePercentage);

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
