package com.kilgore.vaultiq.odds.service;

import com.kilgore.vaultiq.league.entity.Fixture;
import com.kilgore.vaultiq.league.entity.MatchResult;
import com.kilgore.vaultiq.league.entity.Team;
import com.kilgore.vaultiq.league.repository.MatchResultRepository;
import com.kilgore.vaultiq.odds.dto.FixtureOddsAdminResponse;
import com.kilgore.vaultiq.odds.dto.FixtureOddsResponse;
import com.kilgore.vaultiq.odds.entity.FixtureOdds;
import com.kilgore.vaultiq.odds.repository.FixtureOddsRepository;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Advanced Odds Engine v2.1 (Sigmoids, Gravity Elo Integration, Liability Control)
 */
@Service
@RequiredArgsConstructor
public class OddsService {

    private final FixtureOddsRepository fixtureOddsRepository;
    private final MatchResultRepository matchResultRepository;

    private static final double HOME_ADVANTAGE_PTS = 30.0;
    private static final double MARGIN = 1.05; // 5% Overround
    private static final BigDecimal PHANTOM_LIQUIDITY = new BigDecimal("500.00");
    private static final BigDecimal MIN_ODDS = new BigDecimal("1.010");
    private static final BigDecimal MAX_ODDS = new BigDecimal("25.000");

    // ──────────────────────────────────────────────
    //  LAYER 1 — Base Configuration (Pre-Match Engine)
    // ──────────────────────────────────────────────

    @Transactional
    public FixtureOdds generateBaseOdds(Fixture fixture) {
        // 1. Fetch the raw Match Winner Probabilities using Sigmoid curve over 100-1000 bounds
        double[] probs = calculatePreMatchProbabilities(fixture);
        double pHome = probs[0];
        double pDraw = probs[1];
        double pAway = probs[2];

        // Ensure exactly 1.0 sum, then apply margin
        BigDecimal homeWin = BigDecimal.valueOf((1.0 / pHome) / MARGIN);
        BigDecimal draw = BigDecimal.valueOf((1.0 / pDraw) / MARGIN);
        BigDecimal awayWin = BigDecimal.valueOf((1.0 / pAway) / MARGIN);

        // 2. Compute Context-Normalized Expected Goals (xG) 
        Team home = fixture.getHomeTeam();
        Team away = fixture.getAwayTeam();
        double lgAvg = fixture.getSeason().getLeague().getSeasonAvgGoals();

        // Fallback for new teams with no stats
        double homeAvgS = home.getAvgGoalsScored() > 0 ? home.getAvgGoalsScored() : lgAvg;
        double homeAvgC = home.getAvgGoalsConceded() > 0 ? home.getAvgGoalsConceded() : lgAvg;
        double awayAvgS = away.getAvgGoalsScored() > 0 ? away.getAvgGoalsScored() : lgAvg;
        double awayAvgC = away.getAvgGoalsConceded() > 0 ? away.getAvgGoalsConceded() : lgAvg;

        double homeAttack = homeAvgS / lgAvg;
        double awayDefense = awayAvgC / lgAvg;
        double awayAttack = awayAvgS / lgAvg;
        double homeDefense = homeAvgC / lgAvg;

        double homeXg = homeAttack * awayDefense * lgAvg;
        double awayXg = awayAttack * homeDefense * lgAvg;
        double matchXg = homeXg + awayXg;

        // 3. Goals Markets probabilities via logistic approximation of Poisson
        double over15Prob = 1.0 / (1.0 + Math.exp(-1.5 * (matchXg - 1.5)));
        double over25Prob = 1.0 / (1.0 + Math.exp(-1.5 * (matchXg - 2.5)));
        double over35Prob = 1.0 / (1.0 + Math.exp(-1.5 * (matchXg - 3.5)));
        
        // Ensure values remain within bounds [0.05, 0.95]
        over15Prob = Math.max(0.05, Math.min(0.95, over15Prob));
        over25Prob = Math.max(0.05, Math.min(0.95, over25Prob));
        over35Prob = Math.max(0.05, Math.min(0.95, over35Prob));

        double under15Prob = 1.0 - over15Prob;
        double under25Prob = 1.0 - over25Prob;
        double under35Prob = 1.0 - over35Prob;

        // Both teams scoring: proxy based on xG structure.
        double bttsYesProb = (1.0 - Math.exp(-homeXg)) * (1.0 - Math.exp(-awayXg));
        bttsYesProb = Math.max(0.05, Math.min(0.95, bttsYesProb));
        double bttsNoProb = 1.0 - bttsYesProb;

        FixtureOdds odds = FixtureOdds.builder()
                .fixture(fixture)
                .homeWinOdds(clamp(homeWin))
                .drawOdds(clamp(draw))
                .awayWinOdds(clamp(awayWin))
                .over15Odds(clamp(BigDecimal.valueOf((1.0 / over15Prob) / MARGIN)))
                .under15Odds(clamp(BigDecimal.valueOf((1.0 / under15Prob) / MARGIN)))
                .over25Odds(clamp(BigDecimal.valueOf((1.0 / over25Prob) / MARGIN)))
                .under25Odds(clamp(BigDecimal.valueOf((1.0 / under25Prob) / MARGIN)))
                .over35Odds(clamp(BigDecimal.valueOf((1.0 / over35Prob) / MARGIN)))
                .under35Odds(clamp(BigDecimal.valueOf((1.0 / under35Prob) / MARGIN)))
                .bttsYesOdds(clamp(BigDecimal.valueOf((1.0 / bttsYesProb) / MARGIN)))
                .bttsNoOdds(clamp(BigDecimal.valueOf((1.0 / bttsNoProb) / MARGIN)))
                .totalHomeStake(PHANTOM_LIQUIDITY)
                .totalDrawStake(PHANTOM_LIQUIDITY)
                .totalAwayStake(PHANTOM_LIQUIDITY)
                .build();

        return fixtureOddsRepository.save(odds);
    }

    /**
     * Executes the Sigmoid model to determine pure probabilities.
     * Accessible by MatchResultService for post-match Elo computations.
     * @return [pHome, pDraw, pAway]
     */
    public double[] calculatePreMatchProbabilities(Fixture fixture) {
        Team home = fixture.getHomeTeam();
        Team away = fixture.getAwayTeam();

        // 1. Calculate Form points (Max roughly ~15 points per team = minor offset)
        List<MatchResult> homeLast5 = matchResultRepository.findLastNMatchesForTeam(home.getId(), PageRequest.of(0, 5));
        double homeForm = calculateFormPoints(home.getId(), homeLast5);
        List<MatchResult> awayLast5 = matchResultRepository.findLastNMatchesForTeam(away.getId(), PageRequest.of(0, 5));
        double awayForm = calculateFormPoints(away.getId(), awayLast5);

        // 2. H2H bias (max +/- 15 points shift)
        List<MatchResult> h2h = matchResultRepository.findHeadToHeadMatches(home.getId(), away.getId(), PageRequest.of(0, 5));
        double h2hBias = calculateH2HBias(home.getId(), h2h);

        // Determine effective strength difference on 100-1000 scale
        double effectiveHomeStrength = home.getStrength() + homeForm + h2hBias + HOME_ADVANTAGE_PTS;
        double effectiveAwayStrength = away.getStrength() + awayForm - h2hBias;
        double strengthDiff = effectiveHomeStrength - effectiveAwayStrength;

        // Sigmoid Mapping: k=0.004 gracefully handles rating diffs up to ~600
        double k = 0.004;
        double rawHomeProb = 1.0 / (1.0 + Math.exp(-k * strengthDiff));
        double rawAwayProb = 1.0 / (1.0 + Math.exp(k * strengthDiff)); 
        
        // Draw apexes when teams are perfectly equal
        double rawDrawProb = 0.33 * Math.exp(-0.00005 * Math.pow(strengthDiff, 2));

        // Normalization enforcing the strict sum of 1.0 
        double totalSum = rawHomeProb + rawAwayProb + rawDrawProb;
        return new double[]{
                rawHomeProb / totalSum,  // [0] pHome
                rawDrawProb / totalSum,  // [1] pDraw
                rawAwayProb / totalSum   // [2] pAway
        };
    }

    private double calculateFormPoints(UUID teamId, List<MatchResult> recentMatches) {
        double points = 0;
        for (MatchResult mr : recentMatches) {
            boolean isHome = mr.getFixture().getHomeTeam().getId().equals(teamId);
            if (isHome && mr.getHomeScore() > mr.getAwayScore()) points += 3;
            else if (!isHome && mr.getAwayScore() > mr.getHomeScore()) points += 3;
            else if (mr.getHomeScore() == mr.getAwayScore()) points += 1;
        }
        return points * 1.5; // Scale form up to ~22.5 max points of Elo drift
    }

    private double calculateH2HBias(UUID mainTeamId, List<MatchResult> h2hMatches) {
        double diff = 0;
        for (MatchResult mr : h2hMatches) {
            boolean isHome = mr.getFixture().getHomeTeam().getId().equals(mainTeamId);
            if (isHome && mr.getHomeScore() > mr.getAwayScore()) diff += 1;
            else if (!isHome && mr.getAwayScore() > mr.getHomeScore()) diff += 1;
            else if (mr.getHomeScore() != mr.getAwayScore()) diff -= 1; 
        }
        return diff * 2.5; // scales up to ±12.5 points
    }

    // ──────────────────────────────────────────────
    //  LAYER 2 — Financial Liability Adjustment
    // ──────────────────────────────────────────────

    @Transactional
    public void applyDemandAdjustment(UUID fixtureId, String outcome, BigDecimal betStake) {
        FixtureOdds odds = findOddsOrThrow(fixtureId);

        // 1. Add literal monetary stake
        switch (outcome) {
            case "HOME_WIN" -> odds.setTotalHomeStake(odds.getTotalHomeStake().add(betStake));
            case "DRAW"     -> odds.setTotalDrawStake(odds.getTotalDrawStake().add(betStake));
            case "AWAY_WIN" -> odds.setTotalAwayStake(odds.getTotalAwayStake().add(betStake));
            default -> { return; }
        }

        // 2. Assess Financial Exposures
        double hExposure = odds.getTotalHomeStake().doubleValue() * odds.getHomeWinOdds().doubleValue();
        double dExposure = odds.getTotalDrawStake().doubleValue() * odds.getDrawOdds().doubleValue();
        double aExposure = odds.getTotalAwayStake().doubleValue() * odds.getAwayWinOdds().doubleValue();

        double totalExposure = hExposure + dExposure + aExposure;
        if (totalExposure == 0) return;

        double hRiskRatio = hExposure / totalExposure;
        double dRiskRatio = dExposure / totalExposure;
        double aRiskRatio = aExposure / totalExposure;

        // 3. Shift underlying probabilities based on liability squeeze. Max shift = 0.05
        double BASE = 1.0 / 3.0; // 0.33
        double SHIFT_K = 0.15; 
        
        // We re-derive implied probabilities
        double pHome = (1.0 / odds.getHomeWinOdds().doubleValue()) / MARGIN;
        double pDraw = (1.0 / odds.getDrawOdds().doubleValue()) / MARGIN;
        double pAway = (1.0 / odds.getAwayWinOdds().doubleValue()) / MARGIN;

        pHome += (hRiskRatio - BASE) * SHIFT_K;
        pDraw += (dRiskRatio - BASE) * SHIFT_K;
        pAway += (aRiskRatio - BASE) * SHIFT_K;

        // Re-normalize and secure margins
        double sum = pHome + pDraw + pAway;
        odds.setHomeWinOdds(clamp(BigDecimal.valueOf((1.0 / (pHome / sum)) / MARGIN)));
        odds.setDrawOdds(clamp(BigDecimal.valueOf((1.0 / (pDraw / sum)) / MARGIN)));
        odds.setAwayWinOdds(clamp(BigDecimal.valueOf((1.0 / (pAway / sum)) / MARGIN)));

        fixtureOddsRepository.save(odds);
    }

    // ──────────────────────────────────────────────
    //  LAYER 3 — Dynamic Scaled Time & Disciplinary Decay
    // ──────────────────────────────────────────────

    @Transactional
    public void recalculateInPlayOdds(Fixture fixture, MatchResult result) {
        FixtureOdds odds = findOddsOrThrow(fixture.getId());

        int minute = fixture.getMatchMinute();
        double timeRatio = Math.min((double) minute / 90.0, 1.0);
        double remainingTimeFactor = 1.0 - timeRatio;

        // 1. Fetch pre-match underlying baseline
        double[] baseProbs = calculatePreMatchProbabilities(fixture);
        double pHome = baseProbs[0];
        double pDraw = baseProbs[1];
        double pAway = baseProbs[2];

        // 2. Disciplinary Action (Red Card crushes underlying probability relative to remaining time)
        int hRed = result.getHomeRedCards();
        int aRed = result.getAwayRedCards();
        
        if (hRed > 0 || aRed > 0) {
            // A red card at minute 10 carries huge weight compared to minute 85.
            double cardSeverity = 0.50 * remainingTimeFactor; // Can erase up to half of win prob 
            if (hRed > aRed) {
                pHome = Math.max(0.01, pHome - (cardSeverity * (hRed - aRed)));
                pAway = Math.min(0.99, pAway + (cardSeverity * (hRed - aRed)));
            } else if (aRed > hRed) {
                pAway = Math.max(0.01, pAway - (cardSeverity * (aRed - hRed)));
                pHome = Math.min(0.99, pHome + (cardSeverity * (aRed - hRed)));
            }
            // Normalize before score adjustments
            double sum = pHome + pAway + pDraw;
            pHome /= sum; pAway /= sum; pDraw /= sum;
        }

        // 3. Score-Based Decay
        int hScore = result.getHomeScore();
        int aScore = result.getAwayScore();
        int scoreDiff = hScore - aScore;

        if (scoreDiff != 0) {
            // A goal gets exponentially more decisive as time expires
            double goalWeight = 0.30 + (0.60 * timeRatio); 
            if (scoreDiff > 0) {
                pHome += goalWeight * scoreDiff;
                pDraw -= (goalWeight / 2) * scoreDiff;
                pAway -= (goalWeight / 2) * scoreDiff;
            } else {
                pAway += goalWeight * Math.abs(scoreDiff);
                pDraw -= (goalWeight / 2) * Math.abs(scoreDiff);
                pHome -= (goalWeight / 2) * Math.abs(scoreDiff);
            }
        } else {
            // Draw gets exponentially more decisive as time expires on level scores
            double drawPull = 0.70 * timeRatio;
            pDraw += drawPull;
            pHome -= (drawPull / 2);
            pAway -= (drawPull / 2);
        }

        // Final normalization and clamping bounding
        pHome = Math.max(0.01, pHome);
        pAway = Math.max(0.01, pAway);
        pDraw = Math.max(0.01, pDraw);
        double fSum = pHome + pAway + pDraw;

        odds.setHomeWinOdds(clamp(BigDecimal.valueOf((1.0 / (pHome / fSum)) / MARGIN)));
        odds.setDrawOdds(clamp(BigDecimal.valueOf((1.0 / (pDraw / fSum)) / MARGIN)));
        odds.setAwayWinOdds(clamp(BigDecimal.valueOf((1.0 / (pAway / fSum)) / MARGIN)));

        // Goals O/U and BTTS adjustments are identical to legacy design as they process total goals cleanly
        updateInPlayGoals(odds, hScore + aScore);
        
        fixtureOddsRepository.save(odds);
    }

    private void updateInPlayGoals(FixtureOdds odds, int totalGoals) {
        odds.setOver15Odds(clamp(calculateInPlayOverUnder(totalGoals, 1.5, true)));
        odds.setUnder15Odds(clamp(calculateInPlayOverUnder(totalGoals, 1.5, false)));
        odds.setOver25Odds(clamp(calculateInPlayOverUnder(totalGoals, 2.5, true)));
        odds.setUnder25Odds(clamp(calculateInPlayOverUnder(totalGoals, 2.5, false)));
        odds.setOver35Odds(clamp(calculateInPlayOverUnder(totalGoals, 3.5, true)));
        odds.setUnder35Odds(clamp(calculateInPlayOverUnder(totalGoals, 3.5, false)));
    }

    private BigDecimal calculateInPlayOverUnder(int currentGoals, double threshold, boolean isOver) {
        if (isOver) {
            if (currentGoals > threshold) return new BigDecimal("1.010");
            double goalsNeeded = threshold - currentGoals;
            return new BigDecimal("1.250").add(new BigDecimal(goalsNeeded).multiply(new BigDecimal("0.850")));
        } else {
            if (currentGoals > threshold) return MAX_ODDS;
            double goalsRemaining = threshold - currentGoals;
            return new BigDecimal("2.500").subtract(new BigDecimal(goalsRemaining).multiply(new BigDecimal("0.500")));
        }
    }

    // ── Queries and Mapping ──

    @Transactional(readOnly = true)
    public FixtureOddsResponse getOddsForFixture(UUID fixtureId) {
        return mapToResponse(findOddsOrThrow(fixtureId));
    }

    @Transactional(readOnly = true)
    public FixtureOddsAdminResponse getOddsForFixtureAdmin(UUID fixtureId) {
        return mapToAdminResponse(findOddsOrThrow(fixtureId));
    }

    public FixtureOdds findOddsOrThrow(UUID fixtureId) {
        return fixtureOddsRepository.findByFixtureId(fixtureId)
                .orElseThrow(() -> new ResourceNotFoundException("FixtureOdds", "fixtureId", fixtureId));
    }

    private BigDecimal clamp(BigDecimal value) {
        if (value.compareTo(MIN_ODDS) < 0) return MIN_ODDS;
        if (value.compareTo(MAX_ODDS) > 0) return MAX_ODDS;
        return value.setScale(3, RoundingMode.HALF_UP);
    }

    private FixtureOddsResponse mapToResponse(FixtureOdds odds) {
        return FixtureOddsResponse.builder()
                .id(odds.getId())
                .fixtureId(odds.getFixture().getId())
                .homeTeamName(odds.getFixture().getHomeTeam().getName())
                .awayTeamName(odds.getFixture().getAwayTeam().getName())
                .homeWinOdds(odds.getHomeWinOdds())
                .drawOdds(odds.getDrawOdds())
                .awayWinOdds(odds.getAwayWinOdds())
                .over15Odds(odds.getOver15Odds())
                .under15Odds(odds.getUnder15Odds())
                .over25Odds(odds.getOver25Odds())
                .under25Odds(odds.getUnder25Odds())
                .over35Odds(odds.getOver35Odds())
                .under35Odds(odds.getUnder35Odds())
                .bttsYesOdds(odds.getBttsYesOdds())
                .bttsNoOdds(odds.getBttsNoOdds())
                .updatedAt(odds.getUpdatedAt())
                .build();
    }

    private FixtureOddsAdminResponse mapToAdminResponse(FixtureOdds odds) {
        return FixtureOddsAdminResponse.builder()
                .id(odds.getId())
                .fixtureId(odds.getFixture().getId())
                .homeTeamName(odds.getFixture().getHomeTeam().getName())
                .awayTeamName(odds.getFixture().getAwayTeam().getName())
                .homeWinOdds(odds.getHomeWinOdds())
                .drawOdds(odds.getDrawOdds())
                .awayWinOdds(odds.getAwayWinOdds())
                .over15Odds(odds.getOver15Odds())
                .under15Odds(odds.getUnder15Odds())
                .over25Odds(odds.getOver25Odds())
                .under25Odds(odds.getUnder25Odds())
                .over35Odds(odds.getOver35Odds())
                .under35Odds(odds.getUnder35Odds())
                .bttsYesOdds(odds.getBttsYesOdds())
                .bttsNoOdds(odds.getBttsNoOdds())
                .totalHomeStake(odds.getTotalHomeStake())
                .totalDrawStake(odds.getTotalDrawStake())
                .totalAwayStake(odds.getTotalAwayStake())
                .updatedAt(odds.getUpdatedAt())
                .build();
    }
}
