package com.kilgore.vaultiq.league.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "teams", uniqueConstraints = {
        @UniqueConstraint(name = "uq_team_season_name", columnNames = {"season_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(nullable = false, length = 100)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    @Min(100)
    @Max(1000)
    private int strength = 550;

    @Builder.Default
    @Column(name = "avg_goals_scored", nullable = false)
    private double avgGoalsScored = 0.0;

    @Builder.Default
    @Column(name = "avg_goals_conceded", nullable = false)
    private double avgGoalsConceded = 0.0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
