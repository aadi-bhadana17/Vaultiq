package com.kilgore.vaultiq.betting.service;

import com.kilgore.vaultiq.betting.entity.Bet;
import com.kilgore.vaultiq.betting.entity.BetSuspicionFlag;
import com.kilgore.vaultiq.betting.entity.SuspicionReason;
import com.kilgore.vaultiq.betting.repository.BetRepository;
import com.kilgore.vaultiq.betting.repository.BetSuspicionFlagRepository;
import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Detects suspicious betting patterns after every bet placement.
 *
 * Detection rules:
 *   - SUDDEN_LARGE_BET: stake > 5× user's average stake in last 30 days
 *   - RAPID_SEQUENTIAL_BETS: 3+ bets within 60 seconds
 *
 * If flagged → creates BetSuspicionFlag + sets User.isBettingRestricted = true
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SuspicionDetectionService {

    private final BetRepository betRepository;
    private final BetSuspicionFlagRepository flagRepository;
    private final UserRepository userRepository;

    @Transactional
    public void checkForSuspicion(Bet bet) {
        User user = bet.getUser();

        checkSuddenLargeBet(bet, user);
        checkRapidSequentialBets(bet, user);
    }

    private void checkSuddenLargeBet(Bet bet, User user) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        BigDecimal avgStake = betRepository.findAverageStakeByUserIdSince(user.getId(), thirtyDaysAgo);

        if (avgStake.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal threshold = avgStake.multiply(new BigDecimal("5"));
        if (bet.getStake().compareTo(threshold) > 0) {
            createFlag(user, bet, SuspicionReason.SUDDEN_LARGE_BET,
                    String.format("Stake %s exceeds 5× average stake %s (threshold: %s)",
                            bet.getStake(), avgStake, threshold));
        }
    }

    private void checkRapidSequentialBets(Bet bet, User user) {
        LocalDateTime sixtySecondsAgo = LocalDateTime.now().minusSeconds(60);
        long recentBetCount = betRepository.countByUserIdAndCreatedAtAfter(user.getId(), sixtySecondsAgo);

        if (recentBetCount >= 3) {
            createFlag(user, bet, SuspicionReason.RAPID_SEQUENTIAL_BETS,
                    String.format("%d bets placed within 60 seconds", recentBetCount));
        }
    }

    private void createFlag(User user, Bet bet, SuspicionReason reason, String details) {
        BetSuspicionFlag flag = BetSuspicionFlag.builder()
                .user(user)
                .bet(bet)
                .reason(reason)
                .details(details)
                .build();

        flagRepository.save(flag);

        // Auto-restrict the user
        user.setBettingRestricted(true);
        userRepository.save(user);

        log.warn("SUSPICION FLAG: User {} flagged for {} — {}", user.getUsername(), reason, details);
    }
}
