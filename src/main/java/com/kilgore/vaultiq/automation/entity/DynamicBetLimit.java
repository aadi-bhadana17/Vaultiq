package com.kilgore.vaultiq.automation.entity;

import com.kilgore.vaultiq.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dynamic_bet_limits", uniqueConstraints = {
        @UniqueConstraint(name = "uq_limits_user", columnNames = {"user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DynamicBetLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "max_single_bet", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal maxSingleBet = new BigDecimal("10000.00");

    @Column(name = "max_daily_total", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal maxDailyTotal = new BigDecimal("50000.00");

    @Column(name = "risk_score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal riskScore = BigDecimal.ZERO;

    @Column(name = "last_recalculated_at")
    private LocalDateTime lastRecalculatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
