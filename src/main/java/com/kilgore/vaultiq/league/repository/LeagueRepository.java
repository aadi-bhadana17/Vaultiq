package com.kilgore.vaultiq.league.repository;

import com.kilgore.vaultiq.league.entity.League;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeagueRepository extends JpaRepository<League, UUID> {

    Optional<League> findByName(String name);

    boolean existsByName(String name);
}
