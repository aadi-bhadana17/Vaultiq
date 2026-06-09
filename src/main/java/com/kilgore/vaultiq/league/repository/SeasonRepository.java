package com.kilgore.vaultiq.league.repository;

import com.kilgore.vaultiq.league.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeasonRepository extends JpaRepository<Season, UUID> {

    List<Season> findByLeagueId(UUID leagueId);

    @Query("SELECT s FROM Season s WHERE s.league.id = :leagueId AND s.active = true")
    Optional<Season> findActiveSeasonByLeagueId(@Param("leagueId") UUID leagueId);

    boolean existsByLeagueIdAndActiveTrue(UUID leagueId);
}
