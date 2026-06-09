package com.kilgore.vaultiq.automation.service;

import com.kilgore.vaultiq.automation.entity.AutoCashoutRule;
import com.kilgore.vaultiq.automation.repository.AutoCashoutRuleRepository;
import com.kilgore.vaultiq.betting.entity.Bet;
import com.kilgore.vaultiq.league.entity.FixtureStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Auto-Cashout Scheduler — runs every 20 seconds.
 * Monitors active cashout rules and triggers when profit/loss thresholds are hit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoCashoutScheduler {

    private final AutoCashoutRuleRepository ruleRepository;
    private final CashoutService cashoutService;

    @Scheduled(fixedRate = 20000)
    @SchedulerLock(name = "autoCashoutLock", lockAtLeastFor = "10s", lockAtMostFor = "15s")
    @Transactional
    public void scanAndCashout() {
        List<AutoCashoutRule> activeRules = ruleRepository.findActiveRulesWithPendingBets();

        if (activeRules.isEmpty()) return;

        log.debug("Auto-cashout scan: {} active rules", activeRules.size());

        for (AutoCashoutRule rule : activeRules) {
            try {
                processRule(rule);
            } catch (Exception e) {
                log.error("Auto-cashout rule {} failed: {}", rule.getId(), e.getMessage());
            }
        }
    }

    private void processRule(AutoCashoutRule rule) {
        Bet bet = rule.getBet();

        // Skip if fixture is LOCKED (can't cash out during recalc) or FINISHED
        FixtureStatus status = bet.getFixture().getStatus();
        if (status != FixtureStatus.OPEN) return;

        BigDecimal cashoutValue = cashoutService.calculateCashoutValue(bet.getId());
        BigDecimal stake = bet.getStake();

        boolean shouldTrigger = false;
        String reason = "";

        // Check profit target
        if (rule.getProfitTarget() != null) {
            BigDecimal profit = cashoutValue.subtract(stake);
            if (profit.compareTo(rule.getProfitTarget()) >= 0) {
                shouldTrigger = true;
                reason = String.format("Profit target reached: %s >= %s", profit, rule.getProfitTarget());
            }
        }

        // Check loss limit
        if (!shouldTrigger && rule.getLossLimit() != null) {
            BigDecimal loss = stake.subtract(cashoutValue);
            if (loss.compareTo(rule.getLossLimit()) >= 0) {
                shouldTrigger = true;
                reason = String.format("Loss limit reached: %s >= %s", loss, rule.getLossLimit());
            }
        }

        if (shouldTrigger) {
            cashoutService.executeCashout(bet.getId());

            rule.setActive(false);
            rule.setTriggeredAt(LocalDateTime.now());
            ruleRepository.save(rule);

            log.info("Auto-cashout triggered: rule {} — bet {} — {} — cashout value {}",
                    rule.getId(), bet.getId(), reason, cashoutValue);
        }
    }
}
