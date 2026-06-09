package com.kilgore.vaultiq.betting.repository;

import com.kilgore.vaultiq.betting.entity.BetBuilder;
import com.kilgore.vaultiq.betting.entity.BetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BetBuilderRepository extends JpaRepository<BetBuilder, UUID> {

    Page<BetBuilder> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<BetBuilder> findByStatusAndSettledLegsLessThan(BetStatus status, int totalLegs);
}
