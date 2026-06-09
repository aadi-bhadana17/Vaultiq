package com.kilgore.vaultiq.betting.repository;

import com.kilgore.vaultiq.betting.entity.BetSuspicionFlag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BetSuspicionFlagRepository extends JpaRepository<BetSuspicionFlag, UUID> {

    List<BetSuspicionFlag> findByUserIdAndResolvedFalse(UUID userId);

    long countByUserIdAndResolvedFalse(UUID userId);

    Page<BetSuspicionFlag> findByResolvedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<BetSuspicionFlag> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
