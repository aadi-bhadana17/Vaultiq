package com.kilgore.vaultiq.betting.entity;

import com.kilgore.vaultiq.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bet_builders", indexes = {
        @Index(name = "idx_bb_user_created", columnList = "user_id, created_at"),
        @Index(name = "idx_bb_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BetBuilder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "combined_odds", nullable = false, precision = 10, scale = 3)
    private BigDecimal combinedOdds;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal stake;

    @Column(name = "potential_payout", nullable = false, precision = 15, scale = 2)
    private BigDecimal potentialPayout;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BetStatus status = BetStatus.PENDING;

    @Column(name = "total_legs", nullable = false)
    private int totalLegs;

    @Column(name = "settled_legs", nullable = false)
    @Builder.Default
    private int settledLegs = 0;

    @OneToMany(mappedBy = "betBuilder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BetBuilderLeg> legs = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
