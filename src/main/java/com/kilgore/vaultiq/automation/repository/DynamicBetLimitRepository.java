package com.kilgore.vaultiq.automation.repository;

import com.kilgore.vaultiq.automation.entity.DynamicBetLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DynamicBetLimitRepository extends JpaRepository<DynamicBetLimit, UUID> {

    Optional<DynamicBetLimit> findByUserId(UUID userId);
}
