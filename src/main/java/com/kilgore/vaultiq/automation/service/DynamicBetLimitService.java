package com.kilgore.vaultiq.automation.service;

import com.kilgore.vaultiq.automation.dto.DynamicBetLimitResponse;
import com.kilgore.vaultiq.automation.entity.DynamicBetLimit;
import com.kilgore.vaultiq.automation.repository.DynamicBetLimitRepository;
import com.kilgore.vaultiq.betting.repository.BetRepository;
import com.kilgore.vaultiq.betting.repository.BetSuspicionFlagRepository;
import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.repository.UserRepository;
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
 * Dynamic Bet Limits — no scheduler.
 *
 * Instead of running a background job to recalculate, limits are checked at bet placement time.
 * The User entity tracks betCountInPeriod and periodStartedAt.
 *
 * Period = 1 hour. If 1 hour has elapsed since periodStartedAt → reset counter.
 * Max bets per period scales with risk score (fewer bets allowed for risky users).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicBetLimitService {

    private final DynamicBetLimitRepository limitRepository;
    private final BetRepository betRepository;
    private final BetSuspicionFlagRepository flagRepository;
    private final UserRepository userRepository;

    private static final int DEFAULT_MAX_BETS_PER_PERIOD = 50;
    private static final BigDecimal DEFAULT_MAX_SINGLE_BET = new BigDecimal("10000.00");
    private static final BigDecimal DEFAULT_MAX_DAILY_TOTAL = new BigDecimal("50000.00");
    private static final long PERIOD_MINUTES = 60;

    /**
     * Check if a bet is within the user's dynamic limits.
     * Called from BettingService before placing a bet.
     */
    @Transactional
    public void checkBetWithinLimits(UUID userId, BigDecimal stake) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        DynamicBetLimit limits = getOrCreateLimits(userId, user);

        // Check single bet limit
        if (stake.compareTo(limits.getMaxSingleBet()) > 0) {
            throw new BadRequestException(
                    String.format("Stake %s exceeds your maximum single bet limit of %s",
                            stake, limits.getMaxSingleBet()));
        }

        // Check period-based rate limiting
        checkAndUpdatePeriodCount(user, limits);
    }

    @Transactional
    public DynamicBetLimit getOrCreateLimits(UUID userId, User user) {
        return limitRepository.findByUserId(userId).orElseGet(() -> {
            DynamicBetLimit newLimit = DynamicBetLimit.builder()
                    .user(user)
                    .maxSingleBet(DEFAULT_MAX_SINGLE_BET)
                    .maxDailyTotal(DEFAULT_MAX_DAILY_TOTAL)
                    .riskScore(BigDecimal.ZERO)
                    .build();
            return limitRepository.save(newLimit);
        });
    }

    /**
     * Recalculate limits for a user based on betting behavior and suspicion flags.
     */
    @Transactional
    public void recalculateLimits(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        DynamicBetLimit limits = getOrCreateLimits(userId, user);

        // Risk score = suspicion flag count × 10 + (account age < 7 days ? 20 : 0)
        long flagCount = flagRepository.countByUserIdAndResolvedFalse(userId);
        BigDecimal riskScore = BigDecimal.valueOf(flagCount * 10);

        if (user.getAccountAgeDays() < 7) {
            riskScore = riskScore.add(new BigDecimal("20"));
        }

        // Average stake from last 30 days
        BigDecimal avgStake = betRepository.findAverageStakeByUserIdSince(
                userId, LocalDateTime.now().minusDays(30));

        // Max single bet: base × (1 - riskScore/100), clamped to [100, 50000]
        BigDecimal riskMultiplier = BigDecimal.ONE.subtract(
                riskScore.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        if (riskMultiplier.compareTo(new BigDecimal("0.2")) < 0) {
            riskMultiplier = new BigDecimal("0.2");
        }

        BigDecimal maxSingle = DEFAULT_MAX_SINGLE_BET.multiply(riskMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal maxDaily = DEFAULT_MAX_DAILY_TOTAL.multiply(riskMultiplier)
                .setScale(2, RoundingMode.HALF_UP);

        limits.setRiskScore(riskScore);
        limits.setMaxSingleBet(maxSingle);
        limits.setMaxDailyTotal(maxDaily);
        limits.setLastRecalculatedAt(LocalDateTime.now());
        limitRepository.save(limits);

        log.info("Limits recalculated for user {} — risk score {}, max single {}, max daily {}",
                user.getUsername(), riskScore, maxSingle, maxDaily);
    }

    @Transactional(readOnly = true)
    public DynamicBetLimitResponse getLimits(UUID userId) {
        DynamicBetLimit limits = limitRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("DynamicBetLimit", "userId", userId));
        return mapToResponse(limits);
    }

    // ── Private ──

    private void checkAndUpdatePeriodCount(User user, DynamicBetLimit limits) {
        LocalDateTime now = LocalDateTime.now();

        // If period expired or never started → reset
        if (user.getPeriodStartedAt() == null
                || user.getPeriodStartedAt().plusMinutes(PERIOD_MINUTES).isBefore(now)) {
            user.setPeriodStartedAt(now);
            user.setBetCountInPeriod(1);
            userRepository.save(user);
            return;
        }

        // Calculate max bets per period based on risk
        int maxBetsPerPeriod = DEFAULT_MAX_BETS_PER_PERIOD;
        if (limits.getRiskScore().compareTo(new BigDecimal("30")) > 0) {
            maxBetsPerPeriod = 20;
        }
        if (limits.getRiskScore().compareTo(new BigDecimal("60")) > 0) {
            maxBetsPerPeriod = 10;
        }

        if (user.getBetCountInPeriod() >= maxBetsPerPeriod) {
            throw new BadRequestException(
                    String.format("You have reached your maximum of %d bets per hour. Try again later.",
                            maxBetsPerPeriod));
        }

        user.setBetCountInPeriod(user.getBetCountInPeriod() + 1);
        userRepository.save(user);
    }

    private DynamicBetLimitResponse mapToResponse(DynamicBetLimit limits) {
        return DynamicBetLimitResponse.builder()
                .id(limits.getId())
                .userId(limits.getUser().getId())
                .maxSingleBet(limits.getMaxSingleBet())
                .maxDailyTotal(limits.getMaxDailyTotal())
                .riskScore(limits.getRiskScore())
                .lastRecalculatedAt(limits.getLastRecalculatedAt())
                .build();
    }
}
