package com.kilgore.vaultiq.league.service;

import com.kilgore.vaultiq.betting.service.BetSettlementService;
import com.kilgore.vaultiq.league.dto.MatchResultResponse;
import com.kilgore.vaultiq.league.dto.ScoreUpdateRequest;
import com.kilgore.vaultiq.league.entity.Fixture;
import com.kilgore.vaultiq.league.entity.FixtureStatus;
import com.kilgore.vaultiq.league.entity.League;
import com.kilgore.vaultiq.league.entity.MatchResult;
import com.kilgore.vaultiq.league.entity.Team;
import com.kilgore.vaultiq.league.repository.FixtureRepository;
import com.kilgore.vaultiq.league.repository.LeagueRepository;
import com.kilgore.vaultiq.league.repository.MatchResultRepository;
import com.kilgore.vaultiq.league.repository.TeamRepository;
import com.kilgore.vaultiq.odds.service.OddsService;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MatchResultService {

    private final MatchResultRepository matchResultRepository;
    private final FixtureRepository fixtureRepository;
    private final TeamRepository teamRepository;
    private final LeagueRepository leagueRepository;
    private final FixtureService fixtureService;
    private final OddsService oddsService;
    private final BetSettlementService betSettlementService;

    @Transactional
    public MatchResultResponse updateScore(UUID fixtureId, ScoreUpdateRequest request) {
        Fixture fixture = fixtureService.findFixtureOrThrow(fixtureId);

        if (fixture.getStatus() == FixtureStatus.SCHEDULED || fixture.getStatus() == FixtureStatus.FINISHED) {
            throw new BadRequestException("Scores can only be updated when fixture is LOCKED or OPEN");
        }

        fixture.setMatchMinute(request.getMatchMinute());
        fixtureRepository.save(fixture);

        MatchResult matchResult = matchResultRepository.findByFixtureId(fixtureId)
                .orElseGet(() -> MatchResult.builder()
                        .fixture(fixture)
                        .build());

        matchResult.setHomeScore(request.getHomeScore());
        matchResult.setAwayScore(request.getAwayScore());
        
        if (request.getHomeRedCards() != null) matchResult.setHomeRedCards(request.getHomeRedCards());
        if (request.getAwayRedCards() != null) matchResult.setAwayRedCards(request.getAwayRedCards());
        if (request.getHomeYellowCards() != null) matchResult.setHomeYellowCards(request.getHomeYellowCards());
        if (request.getAwayYellowCards() != null) matchResult.setAwayYellowCards(request.getAwayYellowCards());

        matchResult = matchResultRepository.save(matchResult);

        if (fixture.getStatus() == FixtureStatus.OPEN) {
            oddsService.recalculateInPlayOdds(fixture, matchResult);
        }

        return mapToResponse(matchResult, fixtureId);
    }

    @Transactional
    public MatchResultResponse finishFixture(UUID fixtureId) {
        Fixture fixture = fixtureService.findFixtureOrThrow(fixtureId);

        if (fixture.getStatus() == FixtureStatus.SCHEDULED) {
            throw new BadRequestException("Cannot finish a SCHEDULED fixture");
        }
        if (fixture.getStatus() == FixtureStatus.FINISHED) {
            throw new BadRequestException("Fixture is already FINISHED");
        }

        MatchResult matchResult = matchResultRepository.findByFixtureId(fixtureId)
                .orElseThrow(() -> new BadRequestException("Cannot finish fixture without a score"));

        matchResult.setFinal(true);
        matchResult = matchResultRepository.save(matchResult);

        fixture.setStatus(FixtureStatus.FINISHED);
        fixtureRepository.save(fixture);

        // Step 1: Update Asymmetric Gravity Elo Strengths & Goal Averages
        updatePostMatchStats(fixture, matchResult);

        // Step 2: Settle Bets
        betSettlementService.settleFixtureBets(fixtureId);

        return mapToResponse(matchResult, fixtureId);
    }

    private void updatePostMatchStats(Fixture fixture, MatchResult result) {
        Team homeTeam = fixture.getHomeTeam();
        Team awayTeam = fixture.getAwayTeam();
        League league = fixture.getSeason().getLeague();

        // 1. Fetch Expected Pre-Match Probabilities
        double[] preMatchProbs = oddsService.calculatePreMatchProbabilities(fixture);
        double expectedHomeWinProb = preMatchProbs[0];
        double expectedAwayWinProb = preMatchProbs[2]; // Index 2 is Away, 1 is Draw

        // 2. Determine Actual Scores (1.0 = Win, 0.5 = Draw, 0.0 = Loss)
        double actualHomeScore = (result.getHomeScore() > result.getAwayScore()) ? 1.0 : (result.getHomeScore() == result.getAwayScore()) ? 0.5 : 0.0;
        double actualAwayScore = (result.getAwayScore() > result.getHomeScore()) ? 1.0 : (result.getHomeScore() == result.getAwayScore()) ? 0.5 : 0.0;

        double BASE_K = 40.0;
        double homeDelta = actualHomeScore - expectedHomeWinProb;
        double awayDelta = actualAwayScore - expectedAwayWinProb;

        // 3. Gravity Elo Update - Home
        double homeK = BASE_K;
        if (homeDelta > 0) {
            double upScale = Math.max(0.1, Math.pow(1.0 - (homeTeam.getStrength() / 1000.0), 1.5));
            homeK = BASE_K * upScale;
        } else {
            double downScale = Math.max(0.2, (Math.log10(homeTeam.getStrength()) / Math.log10(1000.0)) * 1.2);
            homeK = BASE_K * downScale;
        }

        // 4. Gravity Elo Update - Away
        double awayK = BASE_K;
        if (awayDelta > 0) {
            double upScale = Math.max(0.1, Math.pow(1.0 - (awayTeam.getStrength() / 1000.0), 1.5));
            awayK = BASE_K * upScale;
        } else {
            double downScale = Math.max(0.2, (Math.log10(awayTeam.getStrength()) / Math.log10(1000.0)) * 1.2);
            awayK = BASE_K * downScale;
        }

        int newHomeStrength = (int) Math.round(homeTeam.getStrength() + homeK * homeDelta);
        int newAwayStrength = (int) Math.round(awayTeam.getStrength() + awayK * awayDelta);

        homeTeam.setStrength(Math.max(100, Math.min(1000, newHomeStrength)));
        awayTeam.setStrength(Math.max(100, Math.min(1000, newAwayStrength)));

        // 5. Update Goals Averages for Teams
        Double homeScored = matchResultRepository.calculateAvgGoalsScoredByTeam(homeTeam.getId());
        if (homeScored != null) homeTeam.setAvgGoalsScored(homeScored);
        
        Double homeConceded = matchResultRepository.calculateAvgGoalsConcededByTeam(homeTeam.getId());
        if (homeConceded != null) homeTeam.setAvgGoalsConceded(homeConceded);
        
        Double awayScored = matchResultRepository.calculateAvgGoalsScoredByTeam(awayTeam.getId());
        if (awayScored != null) awayTeam.setAvgGoalsScored(awayScored);
        
        Double awayConceded = matchResultRepository.calculateAvgGoalsConcededByTeam(awayTeam.getId());
        if (awayConceded != null) awayTeam.setAvgGoalsConceded(awayConceded);

        teamRepository.saveAll(List.of(homeTeam, awayTeam));

        // 6. Update League Average Goals
        Double lgAvg = matchResultRepository.calculateLeagueAverageGoals(league.getId());
        if (lgAvg != null) {
            league.setSeasonAvgGoals(lgAvg);
            leagueRepository.save(league);
        }
    }

    @Transactional(readOnly = true)
    public MatchResultResponse getMatchResult(UUID fixtureId) {
        fixtureService.findFixtureOrThrow(fixtureId);
        MatchResult matchResult = matchResultRepository.findByFixtureId(fixtureId)
                .orElseThrow(() -> new ResourceNotFoundException("MatchResult", "fixtureId", fixtureId));
        return mapToResponse(matchResult, fixtureId);
    }

    private MatchResultResponse mapToResponse(MatchResult matchResult, UUID fixtureId) {
        return MatchResultResponse.builder()
                .id(matchResult.getId())
                .fixtureId(fixtureId)
                .homeScore(matchResult.getHomeScore())
                .awayScore(matchResult.getAwayScore())
                .homeRedCards(matchResult.getHomeRedCards())
                .awayRedCards(matchResult.getAwayRedCards())
                .homeYellowCards(matchResult.getHomeYellowCards())
                .awayYellowCards(matchResult.getAwayYellowCards())
                .isFinal(matchResult.isFinal())
                .updatedAt(matchResult.getUpdatedAt())
                .build();
    }
}
