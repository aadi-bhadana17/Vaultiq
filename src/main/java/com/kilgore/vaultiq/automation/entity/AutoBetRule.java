package com.kilgore.vaultiq.automation.entity;

import com.kilgore.vaultiq.betting.entity.BetOutcome;
import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.league.entity.Team;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auto_bet_rules", indexes = {
        @Index(name = "idx_abr_active_team", columnList = "is_active, team_id"),
        @Index(name = "idx_abr_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoBetRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BetOutcome outcome;

    @Column(name = "min_odds", nullable = false, precision = 6, scale = 3)
    private BigDecimal minOdds;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal stake;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_triggered_at")
    private LocalDateTime lastTriggeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
