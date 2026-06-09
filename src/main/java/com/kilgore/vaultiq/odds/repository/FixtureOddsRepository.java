package com.kilgore.vaultiq.odds.repository;

import com.kilgore.vaultiq.odds.entity.FixtureOdds;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface FixtureOddsRepository extends JpaRepository<FixtureOdds, UUID> {

    Optional<FixtureOdds> findByFixtureId(UUID fixtureId);

    boolean existsByFixtureId(UUID fixtureId);
}
