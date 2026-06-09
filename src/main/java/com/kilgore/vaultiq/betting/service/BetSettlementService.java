package com.kilgore.vaultiq.betting.service;

import com.kilgore.vaultiq.betting.entity.*;
import com.kilgore.vaultiq.betting.repository.*;
import com.kilgore.vaultiq.league.entity.Fixture;
import com.kilgore.vaultiq.league.entity.MatchResult;
import com.kilgore.vaultiq.league.repository.FixtureRepository;
import com.kilgore.vaultiq.league.repository.MatchResultRepository;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import com.kilgore.vaultiq.social.entity.CopyBet;
import com.kilgore.vaultiq.social.entity.TipsterProfile;
import com.kilgore.vaultiq.social.repository.CopyBetRepository;
import com.kilgore.vaultiq.social.repository.TipsterProfileRepository;
import com.kilgore.vaultiq.social.service.TipsterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Settles all PENDING bets for a fixture when it transitions to FINISHED.
 *
 * Settlement pipeline (all called by settleFixtureBets):
 *   1. settleStandardBets()      — single bets
 *   2. settleBetBuilderLegs()     — bet builder legs
 *   3. settleSyndicateBets()      — syndicate pool bets
 *   4. processInsuranceRefunds()  — insurance refunds for lost bets
 *   5. processTipsterCuts()       — tipster cuts for winning copy bets
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BetSettlementService {

    private final BetRepository betRepository;
    private final FixtureRepository fixtureRepository;
    private final MatchResultRepository matchResultRepository;
    private final WalletService walletService;

    // ── Phase 5 dependencies ──
    private final BetBuilderLegRepository betBuilderLegRepository;
    private final BetBuilderRepository betBuilderRepository;
    private final SyndicateBetRepository syndicateBetRepository;
    private final SyndicateMemberRepository syndicateMemberRepository;
    private final BetInsuranceService betInsuranceService;
    private final CopyBetRepository copyBetRepository;
    private final TipsterProfileRepository tipsterProfileRepository;
    private final TipsterService tipsterService;

    /**
     * Main entry point — settle all pending bets for a finished fixture.
     * Called by MatchResultService.finishFixture().
     */
    @Transactional
    public void settleFixtureBets(UUID fixtureId) {
        Fixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixture", "id", fixtureId));

        MatchResult result = matchResultRepository.findByFixtureId(fixtureId)
                .orElseThrow(() -> new BadRequestException("Cannot settle bets — no match result exists"));

        if (!result.isFinal()) {
            throw new BadRequestException("Cannot settle bets — match result is not finalized");
        }

        // Determine all winning outcomes from the final score
        Set<BetOutcome> winningOutcomes = determineWinningOutcomes(
                result.getHomeScore(), result.getAwayScore());

        log.info("Settling bets for fixture {} — Score: {}-{} — Winning outcomes: {}",
                fixtureId, result.getHomeScore(), result.getAwayScore(), winningOutcomes);

        // ── Settlement pipeline ──
        settleStandardBets(fixtureId, fixture, winningOutcomes);
        settleBetBuilderLegs(fixtureId, winningOutcomes);
        settleSyndicateBets(fixtureId, fixture, winningOutcomes);

        log.info("Settlement complete for fixture {}", fixtureId);
    }

    // ── Step 1: Standard single bets ──

    private void settleStandardBets(UUID fixtureId, Fixture fixture, Set<BetOutcome> winningOutcomes) {
        List<Bet> pendingBets = betRepository.findByFixtureIdAndStatus(fixtureId, BetStatus.PENDING);

        int wonCount = 0;
        int lostCount = 0;

        for (Bet bet : pendingBets) {
            if (winningOutcomes.contains(bet.getOutcome())) {
                // ── WON ──
                bet.setStatus(BetStatus.WON);
                bet.setSettledAt(LocalDateTime.now());

                // Credit the user's wallet with the full potential payout
                walletService.credit(
                        bet.getUser().getId(),
                        bet.getPotentialPayout(),
                        TxnType.BET_WON,
                        bet.getId(),
                        "BET",
                        String.format("Bet won: %s on %s vs %s — Payout: %s",
                                bet.getOutcome().name(),
                                fixture.getHomeTeam().getName(),
                                fixture.getAwayTeam().getName(),
                                bet.getPotentialPayout())
                );

                // Process tipster cuts for copy bets
                processTipsterCut(bet, true);

                wonCount++;
            } else {
                // ── LOST ──
                bet.setStatus(BetStatus.LOST);
                bet.setSettledAt(LocalDateTime.now());

                // Process insurance refund if applicable
                processInsuranceRefund(bet);

                // Process tipster cuts (update stats for loss too)
                processTipsterCut(bet, false);

                lostCount++;
            }
        }

        betRepository.saveAll(pendingBets);

        log.info("Standard bets settled for fixture {} — {} won, {} lost",
                fixtureId, wonCount, lostCount);
    }

    // ── Step 2: BetBuilder legs ──

    private void settleBetBuilderLegs(UUID fixtureId, Set<BetOutcome> winningOutcomes) {
        List<BetBuilderLeg> pendingLegs = betBuilderLegRepository.findByFixtureIdAndResult(
                fixtureId, LegResult.PENDING);

        if (pendingLegs.isEmpty()) return;

        for (BetBuilderLeg leg : pendingLegs) {
            BetBuilder builder = leg.getBetBuilder();

            // Skip already settled builders (LOST or WON)
            if (builder.getStatus() != BetStatus.PENDING) continue;

            if (winningOutcomes.contains(leg.getOutcome())) {
                // ── Leg WON ──
                leg.setResult(LegResult.WON);
                builder.setSettledLegs(builder.getSettledLegs() + 1);

                // Check if all legs are settled (all WON)
                if (builder.getSettledLegs() == builder.getTotalLegs()) {
                    builder.setStatus(BetStatus.WON);
                    builder.setSettledAt(LocalDateTime.now());

                    // Credit wallet with full payout
                    walletService.credit(
                            builder.getUser().getId(),
                            builder.getPotentialPayout(),
                            TxnType.BET_WON,
                            builder.getId(),
                            "BET_BUILDER",
                            String.format("Bet builder won: %d legs, payout %s",
                                    builder.getTotalLegs(), builder.getPotentialPayout())
                    );

                    log.info("BetBuilder {} ALL LEGS WON — payout {}",
                            builder.getId(), builder.getPotentialPayout());
                }
            } else {
                // ── Leg LOST — fail-fast: entire builder is LOST ──
                leg.setResult(LegResult.LOST);
                builder.setStatus(BetStatus.LOST);
                builder.setSettledAt(LocalDateTime.now());

                log.info("BetBuilder {} LOST (fail-fast) on leg {} — fixture {}",
                        builder.getId(), leg.getId(), fixtureId);
            }

            betBuilderLegRepository.save(leg);
            betBuilderRepository.save(builder);
        }

        log.info("BetBuilder legs settled for fixture {} — {} legs processed",
                fixtureId, pendingLegs.size());
    }

    // ── Step 3: Syndicate bets ──

    private void settleSyndicateBets(UUID fixtureId, Fixture fixture, Set<BetOutcome> winningOutcomes) {
        List<SyndicateBet> pendingBets = syndicateBetRepository.findByFixtureIdAndStatus(
                fixtureId, BetStatus.PENDING);

        if (pendingBets.isEmpty()) return;

        for (SyndicateBet syndicateBet : pendingBets) {
            syndicateBet.setSettledAt(LocalDateTime.now());
            Syndicate syndicate = syndicateBet.getSyndicate();

            if (winningOutcomes.contains(syndicateBet.getOutcome())) {
                // ── WON — distribute proportional payout to each member ──
                syndicateBet.setStatus(BetStatus.WON);

                List<SyndicateMember> members = syndicateMemberRepository
                        .findBySyndicateId(syndicate.getId());

                BigDecimal totalPayout = syndicateBet.getPotentialPayout();

                for (SyndicateMember member : members) {
                    // Proportional payout: (contribution / pool) × totalPayout
                    BigDecimal proportion = member.getContribution()
                            .divide(syndicate.getCurrentPool(), 6, RoundingMode.HALF_UP);
                    BigDecimal memberPayout = totalPayout.multiply(proportion)
                            .setScale(2, RoundingMode.HALF_UP);

                    member.setPayout(memberPayout);
                    syndicateMemberRepository.save(member);

                    walletService.credit(
                            member.getUser().getId(),
                            memberPayout,
                            TxnType.SYNDICATE_PAYOUT,
                            syndicate.getId(),
                            "SYNDICATE",
                            String.format("Syndicate '%s' won — your payout: %s",
                                    syndicate.getName(), memberPayout)
                    );
                }

                log.info("Syndicate '{}' bet WON — {} members paid, total payout {}",
                        syndicate.getName(), members.size(), totalPayout);
            } else {
                // ── LOST ──
                syndicateBet.setStatus(BetStatus.LOST);
                log.info("Syndicate '{}' bet LOST", syndicate.getName());
            }

            syndicate.setStatus(SyndicateStatus.SETTLED);
            syndicateBetRepository.save(syndicateBet);
        }
    }

    // ── Step 4: Insurance refunds ──

    private void processInsuranceRefund(Bet bet) {
        betInsuranceService.processInsuranceRefund(bet.getId());
    }

    // ── Step 5: Tipster cuts for copy bets ──

    private void processTipsterCut(Bet bet, boolean isWin) {
        // Check if this bet is a tipster's original (has copy bets pointing to it)
        List<CopyBet> copies = copyBetRepository.findByOriginalBetId(bet.getId());
        if (copies.isEmpty()) return;

        // Update tipster stats
        tipsterProfileRepository.findByUserId(bet.getUser().getId())
                .ifPresent(tipster -> {
                    tipsterService.updateStats(bet.getUser().getId(), isWin);

                    if (isWin) {
                        // Process cuts for each winning copy bet
                        for (CopyBet copyBet : copies) {
                            Bet followerBet = copyBet.getCopyBet();

                            // Only process if the follower's bet also won
                            if (followerBet.getStatus() == BetStatus.WON) {
                                processSingleTipsterCut(copyBet, tipster, followerBet);
                            }
                        }
                    }
                });
    }

    private void processSingleTipsterCut(CopyBet copyBet, TipsterProfile tipster, Bet followerBet) {
        // tipsterCut = cutPercentage × (payout - stake)
        BigDecimal profit = followerBet.getPotentialPayout().subtract(followerBet.getStake());
        if (profit.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal cut = tipster.getCutPercentage()
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                .multiply(profit)
                .setScale(2, RoundingMode.HALF_UP);

        copyBet.setTipsterCut(cut);
        copyBetRepository.save(copyBet);

        // Credit tipster
        walletService.credit(
                tipster.getUser().getId(),
                cut,
                TxnType.TIPSTER_CUT,
                copyBet.getId(),
                "COPY_BET",
                String.format("Tipster cut: %s%% of profit %s from copy bet",
                        tipster.getCutPercentage(), profit)
        );

        log.info("Tipster {} received cut {} from copy bet {} (follower: {})",
                tipster.getUser().getUsername(), cut,
                copyBet.getId(), copyBet.getFollower().getUsername());
    }

    /**
     * Determine all winning BetOutcome values from a final scoreline.
     *
     * For score 2-1:
     *   - Match winner: HOME_WIN
     *   - Over/Under: OVER_1_5 (3 > 1.5), UNDER_3_5 (3 < 3.5)
     *     Note: 3 goals = OVER_2_5 (3 > 2.5), but NOT OVER_3_5 (3 < 3.5)
     *   - BTTS: BTTS_YES (both scored)
     *
     * @return Set of all outcomes that are winners for this scoreline
     */
    Set<BetOutcome> determineWinningOutcomes(int homeScore, int awayScore) {
        Set<BetOutcome> winners = EnumSet.noneOf(BetOutcome.class);
        int totalGoals = homeScore + awayScore;

        // ── Match Winner ──
        if (homeScore > awayScore) {
            winners.add(BetOutcome.HOME_WIN);
        } else if (awayScore > homeScore) {
            winners.add(BetOutcome.AWAY_WIN);
        } else {
            winners.add(BetOutcome.DRAW);
        }

        // ── Over/Under 1.5 ──
        if (totalGoals > 1) {
            winners.add(BetOutcome.OVER_1_5);
        } else {
            winners.add(BetOutcome.UNDER_1_5);
        }

        // ── Over/Under 2.5 ──
        if (totalGoals > 2) {
            winners.add(BetOutcome.OVER_2_5);
        } else {
            winners.add(BetOutcome.UNDER_2_5);
        }

        // ── Over/Under 3.5 ──
        if (totalGoals > 3) {
            winners.add(BetOutcome.OVER_3_5);
        } else {
            winners.add(BetOutcome.UNDER_3_5);
        }

        // ── Both Teams To Score ──
        if (homeScore > 0 && awayScore > 0) {
            winners.add(BetOutcome.BTTS_YES);
        } else {
            winners.add(BetOutcome.BTTS_NO);
        }

        return winners;
    }
}
