package com.kilgore.vaultiq.odds.entity;

import com.kilgore.vaultiq.league.entity.Fixture;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fixture_odds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FixtureOdds {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", unique = true, nullable = false)
    private Fixture fixture;

    // ── Match Winner Odds ──

    @Column(name = "home_win_odds", nullable = false, precision = 6, scale = 3)
    private BigDecimal homeWinOdds;

    @Column(name = "draw_odds", nullable = false, precision = 6, scale = 3)
    private BigDecimal drawOdds;

    @Column(name = "away_win_odds", nullable = false, precision = 6, scale = 3)
    private BigDecimal awayWinOdds;

    // ── Over/Under Odds ──

    @Column(name = "over_1_5_odds", nullable = false, precision = 6, scale = 3)
    private BigDecimal over15Odds;

    @Column(name = "under_1_5_odds", nullable = false, precision = 6, scale = 3)
    private BigDecimal under15Odds;

    @Column(name = "over_2_5_odds", nullable = false, precision = 6, scale = 3)
    private BigDecimal over25Odds;

    @Column(name = "under_2_5_odds", nullable = false, precision = 6, scale = 3)
    private BigDecimal under25Odds;

    @Column(name = "over_3_5_odds", nullable = false, precision = 6, scale = 3)
    private BigDecimal over35Odds;

    @Column(name = "under_3_5_odds", nullable = false, precision = 6, scale = 3)
    private BigDecimal under35Odds;

    // ── Both Teams To Score Odds ──

    @Column(name = "btts_yes_odds", nullable = false, precision = 6, scale = 3)
    private BigDecimal bttsYesOdds;

    @Column(name = "btts_no_odds", nullable = false, precision = 6, scale = 3)
    private BigDecimal bttsNoOdds;

    // ── Demand Stake Pools (Layer 2 — match-winner only) ──

    @Column(name = "total_home_stake", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalHomeStake = BigDecimal.ZERO;

    @Column(name = "total_draw_stake", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalDrawStake = BigDecimal.ZERO;

    @Column(name = "total_away_stake", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalAwayStake = BigDecimal.ZERO;

    // ── Optimistic Locking ──

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
