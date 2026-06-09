package com.kilgore.vaultiq.automation.entity;

import com.kilgore.vaultiq.betting.entity.Bet;
import com.kilgore.vaultiq.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "auto_cashout_rules", uniqueConstraints = {
        @UniqueConstraint(name = "uq_cashout_bet", columnNames = {"bet_id"})
}, indexes = {
        @Index(name = "idx_acr_active", columnList = "is_active"),
        @Index(name = "idx_acr_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoCashoutRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bet_id", nullable = false)
    private Bet bet;

    @Column(name = "profit_target", precision = 15, scale = 2)
    private BigDecimal profitTarget;

    @Column(name = "loss_limit", precision = 15, scale = 2)
    private BigDecimal lossLimit;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
