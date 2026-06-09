package com.kilgore.vaultiq.betting.repository;

import com.kilgore.vaultiq.betting.entity.BetInsurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BetInsuranceRepository extends JpaRepository<BetInsurance, UUID> {

    Optional<BetInsurance> findByBetId(UUID betId);

    boolean existsByBetId(UUID betId);
}
