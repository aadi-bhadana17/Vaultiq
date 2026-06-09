package com.kilgore.vaultiq.betting.entity;

import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.league.entity.Fixture;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bets", indexes = {
        @Index(name = "idx_bet_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_bet_fixture_status", columnList = "fixture_id, status"),
        @Index(name = "idx_bet_source", columnList = "source_bet_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @Enumerated(EnumType.STRING)
    @Column(name = "bet_type", nullable = false, length = 20)
    @Builder.Default
    private BetType betType = BetType.SINGLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BetOutcome outcome;

    @Column(name = "odds_at_placement", nullable = false, precision = 6, scale = 3)
    private BigDecimal oddsAtPlacement;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal stake;

    @Column(name = "potential_payout", nullable = false, precision = 15, scale = 2)
    private BigDecimal potentialPayout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BetStatus status = BetStatus.PENDING;

    @Column(name = "is_cashed_out", nullable = false)
    @Builder.Default
    private boolean cashedOut = false;

    @Column(name = "cashout_amount", precision = 15, scale = 2)
    private BigDecimal cashoutAmount;

    // Self-referencing FK — links copy bet to tipster's original bet (Phase 5)
    @Column(name = "source_bet_id")
    private UUID sourceBetId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
