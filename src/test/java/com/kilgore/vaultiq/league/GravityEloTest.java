package com.kilgore.vaultiq.league;

import com.kilgore.vaultiq.betting.service.BetSettlementService;
import com.kilgore.vaultiq.league.entity.Fixture;
import com.kilgore.vaultiq.league.entity.FixtureStatus;
import com.kilgore.vaultiq.league.entity.League;
import com.kilgore.vaultiq.league.entity.MatchResult;
import com.kilgore.vaultiq.league.entity.Season;
import com.kilgore.vaultiq.league.entity.Team;
import com.kilgore.vaultiq.league.repository.FixtureRepository;
import com.kilgore.vaultiq.league.repository.LeagueRepository;
import com.kilgore.vaultiq.league.repository.MatchResultRepository;
import com.kilgore.vaultiq.league.repository.TeamRepository;
import com.kilgore.vaultiq.league.service.FixtureService;
import com.kilgore.vaultiq.league.service.MatchResultService;
import com.kilgore.vaultiq.odds.service.OddsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GravityEloTest {

    @Mock private MatchResultRepository matchResultRepository;
    @Mock private FixtureRepository fixtureRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private LeagueRepository leagueRepository;
    @Mock private FixtureService fixtureService;
    @Mock private OddsService oddsService;
    @Mock private BetSettlementService betSettlementService;

    @InjectMocks
    private MatchResultService matchResultService;

    private Fixture testFixture;
    private Team eliteHomeTeam;
    private Team underdogAwayTeam;
    private MatchResult testResult;

    @BeforeEach
    void setUp() {
        League league = League.builder().id(UUID.randomUUID()).seasonAvgGoals(2.6).build();
        Season season = Season.builder().league(league).build();

        eliteHomeTeam = Team.builder().id(UUID.randomUUID()).strength(900).build();
        underdogAwayTeam = Team.builder().id(UUID.randomUUID()).strength(300).build();

        testFixture = Fixture.builder()
                .id(UUID.randomUUID())
                .homeTeam(eliteHomeTeam)
                .awayTeam(underdogAwayTeam)
                .season(season)
                .status(FixtureStatus.OPEN)
                .build();

        testResult = MatchResult.builder()
                .fixture(testFixture)
                .homeScore(2)
                .awayScore(0)
                .isFinal(false)
                .build();
    }

    @Test
    void testGravityElo_EliteWins_GainIsCrushed() {
        when(fixtureService.findFixtureOrThrow(any())).thenReturn(testFixture);
        when(matchResultRepository.findByFixtureId(any())).thenReturn(Optional.of(testResult));
        
        // Mock expected probabilities. Elite team expects to win 95% of the time.
        when(oddsService.calculatePreMatchProbabilities(any())).thenReturn(new double[]{0.95, 0.03, 0.02});

        // Mock saves safely returning the saved item
        when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(i -> i.getArguments()[0]);
        when(fixtureRepository.save(any())).thenReturn(null);

        // Run the finish fixture engine
        matchResultService.finishFixture(testFixture.getId());

        // Capture saved teams
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
        verify(teamRepository).saveAll(teamsCaptor.capture());

        Team savedHome = teamsCaptor.getValue().get(0);
        
        // Expected Logic:
        // Delta = 1.0 (win) - 0.95 (expected) = +0.05
        // Upward scale for 900 Elo should be roughly 0.1 pow(1.5) clamped to 0.1 min
        // K = 40.0 * 0.1 = 4.0
        // Shift = 4.0 * 0.05 = +0.2 rounded to +0
        // Ensure new strength is 900 (basically +0 points, brutally crushed gains)
        int diff = savedHome.getStrength() - 900;
        assertTrue(diff <= 1, "Elite team gained too many points! Gravity engine failed to suppress upward mobility.");
    }

    @Test
    void testGravityElo_EliteLoses_SuffersCatastrophicPenalty() {
        // Elite loses 0-1
        testResult.setHomeScore(0);
        testResult.setAwayScore(1);

        when(fixtureService.findFixtureOrThrow(any())).thenReturn(testFixture);
        when(matchResultRepository.findByFixtureId(any())).thenReturn(Optional.of(testResult));
        when(oddsService.calculatePreMatchProbabilities(any())).thenReturn(new double[]{0.95, 0.03, 0.02});
        
        when(matchResultRepository.save(any(MatchResult.class))).thenAnswer(i -> i.getArguments()[0]);

        matchResultService.finishFixture(testFixture.getId());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Team>> teamsCaptor = ArgumentCaptor.forClass(List.class);
        verify(teamRepository).saveAll(teamsCaptor.capture());

        Team savedHome = teamsCaptor.getValue().get(0);
        Team savedAway = teamsCaptor.getValue().get(1);
        
        // Elite Team (900 Elo) loses when 95% favored.
        // Delta = 0.0 - 0.95 = -0.95
        // DownScale = heavily pushed to ~1.2. 
        // K = 40.0 * 1.2 = 48.0
        // Loss = 48.0 * -0.95 = -45.6
        // Output should be roughly 854 (-46 points).
        
        int homeDiff = savedHome.getStrength() - 900;
        assertTrue(homeDiff < -40, "Elite team did not suffer catastrophic rating drop! Downward Gravity failed.");
        
        // Underdog (300 Elo) pulls miracle upset
        // Delta = 1.0 - 0.02 = +0.98. 
        // UpScale for 300 Elo = ~0.6 scale -> K ~24 
        // Gain ~ 24 * 0.98 = +23
        int awayDiff = savedAway.getStrength() - 300;
        assertTrue(awayDiff > 15, "Underdog failed to climb correctly from miracle upset!");
    }
}
