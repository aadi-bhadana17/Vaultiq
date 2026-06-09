package com.kilgore.vaultiq.social.entity;

import com.kilgore.vaultiq.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tipster_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "uq_tipster_user", columnNames = {"user_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipsterProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_bets", nullable = false)
    @Builder.Default
    private int totalBets = 0;

    @Column(name = "total_wins", nullable = false)
    @Builder.Default
    private int totalWins = 0;

    @Column(name = "win_rate", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal winRate = BigDecimal.ZERO;

    @Column(name = "credibility_score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal credibilityScore = BigDecimal.ZERO;

    @Column(name = "cut_percentage", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal cutPercentage = new BigDecimal("5.00");

    @Column(name = "is_eligible", nullable = false)
    @Builder.Default
    private boolean eligible = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
