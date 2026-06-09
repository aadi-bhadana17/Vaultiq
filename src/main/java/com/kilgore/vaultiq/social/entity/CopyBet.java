package com.kilgore.vaultiq.social.entity;

import com.kilgore.vaultiq.betting.entity.Bet;
import com.kilgore.vaultiq.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "copy_bets", indexes = {
        @Index(name = "idx_cb_original", columnList = "original_bet_id"),
        @Index(name = "idx_cb_follower", columnList = "follower_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CopyBet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_bet_id", nullable = false)
    private Bet originalBet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "copy_bet_id", nullable = false)
    private Bet copyBet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipster_id", nullable = false)
    private TipsterProfile tipster;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @Column(name = "tipster_cut", precision = 15, scale = 2)
    private BigDecimal tipsterCut;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
