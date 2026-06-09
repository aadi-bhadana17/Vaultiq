package com.kilgore.vaultiq.betting.repository;

import com.kilgore.vaultiq.betting.entity.BetStatus;
import com.kilgore.vaultiq.betting.entity.SyndicateBet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyndicateBetRepository extends JpaRepository<SyndicateBet, UUID> {

    Optional<SyndicateBet> findBySyndicateId(UUID syndicateId);

    List<SyndicateBet> findByFixtureIdAndStatus(UUID fixtureId, BetStatus status);
}
