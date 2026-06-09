package com.kilgore.vaultiq.social.repository;

import com.kilgore.vaultiq.social.entity.TipsterProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TipsterProfileRepository extends JpaRepository<TipsterProfile, UUID> {

    Optional<TipsterProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    Page<TipsterProfile> findByEligibleTrue(Pageable pageable);
}
