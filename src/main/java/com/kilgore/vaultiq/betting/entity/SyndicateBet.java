package com.kilgore.vaultiq.betting.entity;

import com.kilgore.vaultiq.league.entity.Fixture;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "syndicate_bets", indexes = {
        @Index(name = "idx_sb_fixture_status", columnList = "fixture_id, status"),
        @Index(name = "idx_sb_syndicate", columnList = "syndicate_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyndicateBet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syndicate_id", nullable = false)
    private Syndicate syndicate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BetOutcome outcome;

    @Column(name = "odds_at_placement", nullable = false, precision = 6, scale = 3)
    private BigDecimal oddsAtPlacement;

    @Column(name = "total_stake", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalStake;

    @Column(name = "potential_payout", nullable = false, precision = 15, scale = 2)
    private BigDecimal potentialPayout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BetStatus status = BetStatus.PENDING;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private LocalDateTime placedAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @PrePersist
    protected void onCreate() {
        placedAt = LocalDateTime.now();
    }
}
