package com.kilgore.vaultiq.automation.service;

import com.kilgore.vaultiq.automation.entity.AutoBetRule;
import com.kilgore.vaultiq.automation.repository.AutoBetRuleRepository;
import com.kilgore.vaultiq.betting.service.BettingService;
import com.kilgore.vaultiq.league.entity.Fixture;
import com.kilgore.vaultiq.league.entity.FixtureStatus;
import com.kilgore.vaultiq.league.repository.FixtureRepository;
import com.kilgore.vaultiq.odds.entity.FixtureOdds;
import com.kilgore.vaultiq.odds.service.OddsService;
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
 * Auto-Bet Scheduler — runs every 20 seconds.
 * Scans active rules, matches them to OPEN fixtures, and auto-places bets.
 * ONE-SHOT: after triggering, the rule is deactivated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoBetScheduler {

    private final AutoBetRuleRepository ruleRepository;
    private final FixtureRepository fixtureRepository;
    private final OddsService oddsService;
    private final BettingService bettingService;

    @Scheduled(fixedRate = 20000)
    @SchedulerLock(name = "autoBetLock", lockAtLeastFor = "10s", lockAtMostFor = "15s")
    @Transactional
    public void scanAndPlaceBets() {
        List<AutoBetRule> activeRules = ruleRepository.findByActiveTrue();

        if (activeRules.isEmpty()) return;

        log.debug("Auto-bet scan: {} active rules", activeRules.size());

        for (AutoBetRule rule : activeRules) {
            try {
                processRule(rule);
            } catch (Exception e) {
                log.error("Auto-bet rule {} failed: {}", rule.getId(), e.getMessage());
            }
        }
    }

    private void processRule(AutoBetRule rule) {
        // Find OPEN fixtures where the rule's team is playing
        List<Fixture> fixtures = fixtureRepository.findByHomeTeamIdOrAwayTeamId(
                rule.getTeam().getId(), rule.getTeam().getId());

        for (Fixture fixture : fixtures) {
            if (fixture.getStatus() != FixtureStatus.OPEN) continue;

            // Check if current odds meet the minimum
            try {
                FixtureOdds odds = oddsService.findOddsOrThrow(fixture.getId());
                BigDecimal currentOdds = getOddsForOutcome(odds, rule.getOutcome().name());

                if (currentOdds.compareTo(rule.getMinOdds()) >= 0) {
                    // Place the bet
                    bettingService.placeBetInternal(
                            rule.getUser().getId(),
                            fixture.getId(),
                            rule.getOutcome().name(),
                            rule.getStake()
                    );

                    // One-shot: deactivate after firing
                    rule.setActive(false);
                    rule.setLastTriggeredAt(LocalDateTime.now());
                    ruleRepository.save(rule);

                    log.info("Auto-bet triggered: rule {} — user {}, team {}, fixture {}, outcome {}, odds {}",
                            rule.getId(), rule.getUser().getUsername(), rule.getTeam().getName(),
                            fixture.getId(), rule.getOutcome(), currentOdds);

                    return; // One-shot — don't check more fixtures
                }
            } catch (Exception e) {
                log.warn("Auto-bet rule {} skipped fixture {}: {}", rule.getId(), fixture.getId(), e.getMessage());
            }
        }
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
