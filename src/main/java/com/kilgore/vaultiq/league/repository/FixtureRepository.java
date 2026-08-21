package com.kilgore.vaultiq.league.repository;

import com.kilgore.vaultiq.league.entity.Fixture;
import com.kilgore.vaultiq.league.entity.FixtureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FixtureRepository extends JpaRepository<Fixture, UUID> {

    List<Fixture> findBySeasonId(UUID seasonId);

    List<Fixture> findByHomeTeamIdOrAwayTeamId(UUID homeTeamId, UUID awayTeamId);

    @Query("SELECT f FROM Fixture f WHERE f.status <> 'FINISHED' AND f.season.active = true ORDER BY f.scheduledAt ASC")
    List<Fixture> findActiveFeedFixtures();

    @Query("SELECT f FROM Fixture f WHERE f.status <> 'FINISHED' AND f.season.id = :seasonId ORDER BY f.scheduledAt ASC")
    List<Fixture> findActiveFeedFixturesBySeasonId(@Param("seasonId") UUID seasonId);

    @Query("SELECT f FROM Fixture f WHERE f.season.id = :seasonId AND f.status = :status ORDER BY f.scheduledAt ASC")
    List<Fixture> findBySeasonIdAndStatus(@Param("seasonId") UUID seasonId, @Param("status") FixtureStatus status);

    List<Fixture> findByStatus(FixtureStatus status);
}
