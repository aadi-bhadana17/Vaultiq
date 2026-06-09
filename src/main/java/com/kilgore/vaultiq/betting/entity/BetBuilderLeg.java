package com.kilgore.vaultiq.betting.entity;

import com.kilgore.vaultiq.league.entity.Fixture;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bet_builder_legs", indexes = {
        @Index(name = "idx_bbl_fixture_result", columnList = "fixture_id, result"),
        @Index(name = "idx_bbl_builder", columnList = "bet_builder_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BetBuilderLeg {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bet_builder_id", nullable = false)
    private BetBuilder betBuilder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixture_id", nullable = false)
    private Fixture fixture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BetOutcome outcome;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome_category", nullable = false, length = 20)
    private OutcomeCategory outcomeCategory;

    @Column(name = "odds_at_placement", nullable = false, precision = 6, scale = 3)
    private BigDecimal oddsAtPlacement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LegResult result = LegResult.PENDING;

    /**
     * Auto-derive outcomeCategory from outcome before persisting.
     */
    @PrePersist
    protected void deriveCategory() {
        if (outcomeCategory == null && outcome != null) {
            outcomeCategory = switch (outcome) {
                case HOME_WIN, DRAW, AWAY_WIN -> OutcomeCategory.MATCH_RESULT;
                case OVER_1_5, UNDER_1_5, OVER_2_5, UNDER_2_5, OVER_3_5, UNDER_3_5 -> OutcomeCategory.OVER_UNDER;
                case BTTS_YES, BTTS_NO -> OutcomeCategory.BTTS;
            };
        }
    }
}
