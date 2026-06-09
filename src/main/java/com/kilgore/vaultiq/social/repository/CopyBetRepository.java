package com.kilgore.vaultiq.social.repository;

import com.kilgore.vaultiq.social.entity.CopyBet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CopyBetRepository extends JpaRepository<CopyBet, UUID> {

    List<CopyBet> findByOriginalBetId(UUID originalBetId);

    Page<CopyBet> findByFollowerIdOrderByCreatedAtDesc(UUID followerId, Pageable pageable);

    boolean existsByOriginalBetIdAndFollowerId(UUID originalBetId, UUID followerId);
}
