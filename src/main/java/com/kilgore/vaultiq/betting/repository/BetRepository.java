package com.kilgore.vaultiq.betting.repository;

import com.kilgore.vaultiq.betting.entity.Bet;
import com.kilgore.vaultiq.betting.entity.BetStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BetRepository extends JpaRepository<Bet, UUID> {

    /**
     * Find all bets for a fixture with a specific status.
     * Used by settlement to load all PENDING bets for a finished fixture.
     */
    List<Bet> findByFixtureIdAndStatus(UUID fixtureId, BetStatus status);

    /**
     * Paginated bet history for a user, most recent first.
     */
    Page<Bet> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    long countByUserIdAndStatus(UUID userId, BetStatus status);

    // ── Suspicion Detection Queries ──

    /**
     * Count bets placed by a user after a given time.
     * Used for RAPID_SEQUENTIAL_BETS detection.
     */
    long countByUserIdAndCreatedAtAfter(UUID userId, LocalDateTime after);

    /**
     * Average stake for a user's bets in a time range.
     * Used for SUDDEN_LARGE_BET detection.
     */
    @Query("SELECT COALESCE(AVG(b.stake), 0) FROM Bet b WHERE b.user.id = :userId AND b.createdAt >= :since")
    BigDecimal findAverageStakeByUserIdSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);

    /**
     * Find all bets by source bet ID (copy bets pointing to a tipster's original).
     */
    List<Bet> findBySourceBetId(UUID sourceBetId);
}
