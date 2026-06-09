package com.kilgore.vaultiq.league.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "match_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", unique = true, nullable = false)
    private Fixture fixture;

    @Column(name = "home_score", nullable = false)
    @Builder.Default
    private int homeScore = 0;

    @Column(name = "away_score", nullable = false)
    @Builder.Default
    private int awayScore = 0;

    @Column(name = "is_final", nullable = false)
    @Builder.Default
    private boolean isFinal = false;

    @Column(name = "home_red_cards", nullable = false)
    @Builder.Default
    private int homeRedCards = 0;

    @Column(name = "away_red_cards", nullable = false)
    @Builder.Default
    private int awayRedCards = 0;

    @Column(name = "home_yellow_cards", nullable = false)
    @Builder.Default
    private int homeYellowCards = 0;

    @Column(name = "away_yellow_cards", nullable = false)
    @Builder.Default
    private int awayYellowCards = 0;

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
