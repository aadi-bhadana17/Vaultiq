package com.kilgore.vaultiq.automation.repository;

import com.kilgore.vaultiq.automation.entity.AutoBetRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AutoBetRuleRepository extends JpaRepository<AutoBetRule, UUID> {

    List<AutoBetRule> findByActiveTrueAndTeamId(UUID teamId);

    Page<AutoBetRule> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<AutoBetRule> findByActiveTrue();
}
