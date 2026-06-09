package com.kilgore.vaultiq.betting.entity;

import com.kilgore.vaultiq.identity.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bet_suspicion_flags", indexes = {
        @Index(name = "idx_bsf_user_resolved", columnList = "user_id, is_resolved")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BetSuspicionFlag {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bet_id")
    private Bet bet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SuspicionReason reason;

    @Column(length = 500)
    private String details;

    @Column(name = "is_resolved", nullable = false)
    @Builder.Default
    private boolean resolved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private User resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
