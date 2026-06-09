package com.kilgore.vaultiq.odds;

import com.kilgore.vaultiq.league.entity.Fixture;
import com.kilgore.vaultiq.league.entity.League;
import com.kilgore.vaultiq.league.entity.Season;
import com.kilgore.vaultiq.league.entity.Team;
import com.kilgore.vaultiq.league.repository.MatchResultRepository;
import com.kilgore.vaultiq.odds.entity.FixtureOdds;
import com.kilgore.vaultiq.odds.repository.FixtureOddsRepository;
import com.kilgore.vaultiq.odds.service.OddsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OddsEngineMathTest {

    @Mock
    private FixtureOddsRepository fixtureOddsRepository;

    @Mock
    private MatchResultRepository matchResultRepository;

    @InjectMocks
    private OddsService oddsService;

    private Fixture testFixture;
    private Team homeTeam;
    private Team awayTeam;

    @BeforeEach
    void setUp() {
        League league = League.builder().seasonAvgGoals(2.6).build();
        Season season = Season.builder().league(league).build();

        homeTeam = Team.builder().id(UUID.randomUUID()).strength(550).avgGoalsScored(1.5).avgGoalsConceded(1.0).build();
        awayTeam = Team.builder().id(UUID.randomUUID()).strength(550).avgGoalsScored(1.0).avgGoalsConceded(1.5).build();

        testFixture = Fixture.builder()
                .id(UUID.randomUUID())
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .season(season)
                .build();
    }

    @Test
    void testSigmoidNormalization_Exactly100Percent() {
        // Mock empty form and H2H
        when(matchResultRepository.findLastNMatchesForTeam(any(), any())).thenReturn(Collections.emptyList());
        when(matchResultRepository.findHeadToHeadMatches(any(), any(), any())).thenReturn(Collections.emptyList());

        // We intercept the save to capture the generated odds
        when(fixtureOddsRepository.save(any(FixtureOdds.class))).thenAnswer(i -> i.getArguments()[0]);

        // Home 550 vs Away 550
        FixtureOdds odds = oddsService.generateBaseOdds(testFixture);

        double homeImplied = 1.0 / odds.getHomeWinOdds().doubleValue();
        double drawImplied = 1.0 / odds.getDrawOdds().doubleValue();
        double awayImplied = 1.0 / odds.getAwayWinOdds().doubleValue();

        double totalImpliedProb = homeImplied + drawImplied + awayImplied;

        // Total implied probability should equal EXACTLY our Margin (1.05) or slightly off due to BigDecimal clamping
        // Allowing minor floating point variance (0.005)
        assertEquals(1.05, totalImpliedProb, 0.005, "Probabilities failed to sum to the 1.05 margin anchor point!");
    }

    @Test
    void testSigmoidBounds_EliteVsBottom() {
        // Set extreme ratings
        homeTeam.setStrength(950);
        awayTeam.setStrength(150);

        when(matchResultRepository.findLastNMatchesForTeam(any(), any())).thenReturn(Collections.emptyList());
        when(matchResultRepository.findHeadToHeadMatches(any(), any(), any())).thenReturn(Collections.emptyList());

        double[] preMatchProbs = oddsService.calculatePreMatchProbabilities(testFixture);

        double pHome = preMatchProbs[0];
        double pDraw = preMatchProbs[1];
        double pAway = preMatchProbs[2];

        // Ensure Home is massively favored but mathematically capped gracefully
        assertTrue(pHome > 0.90, "Home win prob should be > 90% for Elite vs Bottom");
        assertTrue(pAway < 0.05, "Away win prob should be < 5% for Elite vs Bottom");
        
        // Ensure perfect `1.0` sum before margin applied
        assertEquals(1.0, pHome + pDraw + pAway, 0.0001, "Raw probabilities must sum perfectly to 1.0");
    }

    @Test
    void testLiabilityAdjustment_SqueezesOdds() {
        FixtureOdds initialOdds = FixtureOdds.builder()
                .fixture(testFixture)
                .homeWinOdds(new BigDecimal("2.000"))
                .drawOdds(new BigDecimal("3.000"))
                .awayWinOdds(new BigDecimal("4.000"))
                .totalHomeStake(new BigDecimal("500"))
                .totalDrawStake(new BigDecimal("500"))
                .totalAwayStake(new BigDecimal("500"))
                .build();
                
        when(fixtureOddsRepository.findByFixtureId(any())).thenReturn(java.util.Optional.of(initialOdds));
        when(fixtureOddsRepository.save(any(FixtureOdds.class))).thenAnswer(i -> i.getArguments()[0]);

        // Place a massive $100,000 bet on Home
        oddsService.applyDemandAdjustment(testFixture.getId(), "HOME_WIN", new BigDecimal("100000.00"));

        // Home odds should strictly drop to limit exposure
        assertTrue(initialOdds.getHomeWinOdds().compareTo(new BigDecimal("2.000")) < 0, "Home Odds did not squeeze under liability pressure!");
        
        // Away odds should inflate to attract liquidity
        assertTrue(initialOdds.getAwayWinOdds().compareTo(new BigDecimal("4.000")) > 0, "Away Odds did not inflate to attract risk!");
    }
}
