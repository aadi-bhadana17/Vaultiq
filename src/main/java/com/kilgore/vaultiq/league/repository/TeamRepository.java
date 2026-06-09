package com.kilgore.vaultiq.league.repository;

import com.kilgore.vaultiq.league.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    List<Team> findBySeasonId(UUID seasonId);

    boolean existsBySeasonIdAndName(UUID seasonId, String name);
}
