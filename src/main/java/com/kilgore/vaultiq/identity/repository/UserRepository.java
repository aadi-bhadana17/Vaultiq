package com.kilgore.vaultiq.identity.repository;

import com.kilgore.vaultiq.identity.entity.Role;
import com.kilgore.vaultiq.identity.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    Optional<User> findFirstByRole(Role role);

    /**
     * Acquires a PESSIMISTIC_WRITE lock on the User row.
     * MUST be used for all wallet balance mutations to prevent concurrent modification.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForWalletUpdate(@Param("id") UUID id);
}
