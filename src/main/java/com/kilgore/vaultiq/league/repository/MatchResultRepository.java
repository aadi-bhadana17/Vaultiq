package com.kilgore.vaultiq.league.repository;

import com.kilgore.vaultiq.league.entity.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchResultRepository extends JpaRepository<MatchResult, UUID> {

    Optional<MatchResult> findByFixtureId(UUID fixtureId);

    boolean existsByFixtureId(UUID fixtureId);

    @org.springframework.data.jpa.repository.Query("SELECT mr FROM MatchResult mr WHERE (mr.fixture.homeTeam.id = :teamId OR mr.fixture.awayTeam.id = :teamId) AND mr.isFinal = true ORDER BY mr.fixture.scheduledAt DESC")
    java.util.List<MatchResult> findLastNMatchesForTeam(@org.springframework.data.repository.query.Param("teamId") UUID teamId, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT mr FROM MatchResult mr WHERE ((mr.fixture.homeTeam.id = :teamAId AND mr.fixture.awayTeam.id = :teamBId) OR (mr.fixture.homeTeam.id = :teamBId AND mr.fixture.awayTeam.id = :teamAId)) AND mr.isFinal = true ORDER BY mr.fixture.scheduledAt DESC")
    java.util.List<MatchResult> findHeadToHeadMatches(@org.springframework.data.repository.query.Param("teamAId") UUID teamAId, @org.springframework.data.repository.query.Param("teamBId") UUID teamBId, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(mr.homeScore + mr.awayScore) * 1.0 / NULLIF(COUNT(mr), 0), 2.6) FROM MatchResult mr WHERE mr.fixture.season.league.id = :leagueId AND mr.isFinal = true")
    Double calculateLeagueAverageGoals(@org.springframework.data.repository.query.Param("leagueId") UUID leagueId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(CASE WHEN mr.fixture.homeTeam.id = :teamId THEN mr.homeScore ELSE mr.awayScore END) * 1.0 / NULLIF(COUNT(mr), 0), 0.0) FROM MatchResult mr WHERE (mr.fixture.homeTeam.id = :teamId OR mr.fixture.awayTeam.id = :teamId) AND mr.isFinal = true")
    Double calculateAvgGoalsScoredByTeam(@org.springframework.data.repository.query.Param("teamId") UUID teamId);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(CASE WHEN mr.fixture.homeTeam.id = :teamId THEN mr.awayScore ELSE mr.homeScore END) * 1.0 / NULLIF(COUNT(mr), 0), 0.0) FROM MatchResult mr WHERE (mr.fixture.homeTeam.id = :teamId OR mr.fixture.awayTeam.id = :teamId) AND mr.isFinal = true")
    Double calculateAvgGoalsConcededByTeam(@org.springframework.data.repository.query.Param("teamId") UUID teamId);
}
