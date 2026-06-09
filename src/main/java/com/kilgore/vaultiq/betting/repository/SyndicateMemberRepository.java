package com.kilgore.vaultiq.betting.repository;

import com.kilgore.vaultiq.betting.entity.SyndicateMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyndicateMemberRepository extends JpaRepository<SyndicateMember, UUID> {

    List<SyndicateMember> findBySyndicateId(UUID syndicateId);

    boolean existsBySyndicateIdAndUserId(UUID syndicateId, UUID userId);

    Optional<SyndicateMember> findBySyndicateIdAndUserId(UUID syndicateId, UUID userId);
}
