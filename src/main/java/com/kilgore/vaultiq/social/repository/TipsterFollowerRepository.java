package com.kilgore.vaultiq.social.repository;

import com.kilgore.vaultiq.social.entity.TipsterFollower;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TipsterFollowerRepository extends JpaRepository<TipsterFollower, UUID> {

    List<TipsterFollower> findByTipsterId(UUID tipsterId);

    Page<TipsterFollower> findByFollowerIdAndActiveTrue(UUID followerId, Pageable pageable);

    boolean existsByTipsterIdAndFollowerId(UUID tipsterId, UUID followerId);

    Optional<TipsterFollower> findByTipsterIdAndFollowerId(UUID tipsterId, UUID followerId);
}
