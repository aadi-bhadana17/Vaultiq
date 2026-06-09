package com.kilgore.vaultiq.identity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "wallet_balance", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal walletBalance = BigDecimal.ZERO;

    @Column(name = "is_betting_restricted", nullable = false)
    @Builder.Default
    private boolean bettingRestricted = false;

    // ── Dynamic Bet Limiting (no scheduler — checked at placement time) ──

    @Column(name = "bet_count_in_period", nullable = false)
    @Builder.Default
    private int betCountInPeriod = 0;

    @Column(name = "period_started_at")
    private LocalDateTime periodStartedAt = LocalDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ── Computed field ──

    @Transient
    public int getAccountAgeDays() {
        if (createdAt == null) return 0;
        return (int) ChronoUnit.DAYS.between(createdAt.toLocalDate(), LocalDate.now());
    }

    // ── Lifecycle callbacks ──

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
