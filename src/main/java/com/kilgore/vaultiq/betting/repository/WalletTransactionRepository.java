package com.kilgore.vaultiq.betting.repository;

import com.kilgore.vaultiq.betting.entity.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Page<WalletTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<WalletTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<WalletTransaction> findByReferenceId(UUID referenceId);
}
