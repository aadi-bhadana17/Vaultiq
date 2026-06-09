package com.kilgore.vaultiq.automation.repository;

import com.kilgore.vaultiq.automation.entity.AutoCashoutRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AutoCashoutRuleRepository extends JpaRepository<AutoCashoutRule, UUID> {

    Optional<AutoCashoutRule> findByBetId(UUID betId);

    @Query("SELECT r FROM AutoCashoutRule r WHERE r.active = true AND r.bet.status = 'PENDING'")
    List<AutoCashoutRule> findActiveRulesWithPendingBets();

    Page<AutoCashoutRule> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
