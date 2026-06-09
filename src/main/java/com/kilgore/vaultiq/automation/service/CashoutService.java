package com.kilgore.vaultiq.automation.service;

import com.kilgore.vaultiq.betting.entity.Bet;
import com.kilgore.vaultiq.betting.entity.BetStatus;
import com.kilgore.vaultiq.betting.entity.TxnType;
import com.kilgore.vaultiq.betting.repository.BetRepository;
import com.kilgore.vaultiq.betting.service.WalletService;
import com.kilgore.vaultiq.league.entity.Fixture;
import com.kilgore.vaultiq.odds.entity.FixtureOdds;
import com.kilgore.vaultiq.odds.service.OddsService;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CashoutService — handles manual and auto-triggered cashout for bets.
 *
 * Cashout value = stake × (currentOdds / oddsAtPlacement)
 * If currentOdds improved → profit, if worsened → partial loss
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CashoutService {

    private final BetRepository betRepository;
    private final OddsService oddsService;
    private final WalletService walletService;

    /**
     * Calculate the current cashout value for a bet.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateCashoutValue(UUID betId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new ResourceNotFoundException("Bet", "id", betId));

        if (bet.getStatus() != BetStatus.PENDING) {
            throw new BadRequestException("Can only cash out PENDING bets");
        }

        FixtureOdds odds = oddsService.findOddsOrThrow(bet.getFixture().getId());
        BigDecimal currentOdds = getOddsForOutcome(odds, bet.getOutcome().name());

        return bet.getStake()
                .multiply(currentOdds)
                .divide(bet.getOddsAtPlacement(), 2, RoundingMode.HALF_UP);
    }

    /**
     * Execute cashout — marks bet as CASHED_OUT and credits wallet.
     */
    @Transactional
    public BigDecimal executeCashout(UUID betId) {
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new ResourceNotFoundException("Bet", "id", betId));

        if (bet.getStatus() != BetStatus.PENDING) {
            throw new BadRequestException("Can only cash out PENDING bets");
        }

        FixtureOdds odds = oddsService.findOddsOrThrow(bet.getFixture().getId());
        BigDecimal currentOdds = getOddsForOutcome(odds, bet.getOutcome().name());

        BigDecimal cashoutAmount = bet.getStake()
                .multiply(currentOdds)
                .divide(bet.getOddsAtPlacement(), 2, RoundingMode.HALF_UP);

        bet.setStatus(BetStatus.CASHED_OUT);
        bet.setCashedOut(true);
        bet.setCashoutAmount(cashoutAmount);
        bet.setSettledAt(LocalDateTime.now());
        betRepository.save(bet);

        Fixture fixture = bet.getFixture();
        walletService.credit(
                bet.getUser().getId(),
                cashoutAmount,
                TxnType.BET_CASHOUT,
                bet.getId(),
                "BET",
                String.format("Cashout: %s on %s vs %s — amount %s",
                        bet.getOutcome().name(),
                        fixture.getHomeTeam().getName(),
                        fixture.getAwayTeam().getName(),
                        cashoutAmount)
        );

        log.info("Bet {} cashed out — amount {} (stake was {})",
                betId, cashoutAmount, bet.getStake());

        return cashoutAmount;
    }

    private BigDecimal getOddsForOutcome(FixtureOdds odds, String outcomeName) {
        return switch (outcomeName) {
            case "HOME_WIN" -> odds.getHomeWinOdds();
            case "DRAW" -> odds.getDrawOdds();
            case "AWAY_WIN" -> odds.getAwayWinOdds();
            case "OVER_1_5" -> odds.getOver15Odds();
            case "UNDER_1_5" -> odds.getUnder15Odds();
            case "OVER_2_5" -> odds.getOver25Odds();
            case "UNDER_2_5" -> odds.getUnder25Odds();
            case "OVER_3_5" -> odds.getOver35Odds();
            case "UNDER_3_5" -> odds.getUnder35Odds();
            case "BTTS_YES" -> odds.getBttsYesOdds();
            case "BTTS_NO" -> odds.getBttsNoOdds();
            default -> throw new IllegalArgumentException("Unknown outcome: " + outcomeName);
        };
    }
}
