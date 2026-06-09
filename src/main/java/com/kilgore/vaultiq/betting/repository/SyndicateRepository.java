package com.kilgore.vaultiq.betting.repository;

import com.kilgore.vaultiq.betting.entity.Syndicate;
import com.kilgore.vaultiq.betting.entity.SyndicateStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SyndicateRepository extends JpaRepository<Syndicate, UUID> {

    Page<Syndicate> findByCreatedByIdOrderByCreatedAtDesc(UUID creatorId, Pageable pageable);

    Page<Syndicate> findByStatusOrderByCreatedAtDesc(SyndicateStatus status, Pageable pageable);
}
