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
@Table(name = "syndicates", indexes = {
        @Index(name = "idx_syndicate_creator", columnList = "created_by_id"),
        @Index(name = "idx_syndicate_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Syndicate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "target_stake", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetStake;

    @Column(name = "current_pool", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal currentPool = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SyndicateStatus status = SyndicateStatus.OPEN;

    @OneToMany(mappedBy = "syndicate", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SyndicateMember> members = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
