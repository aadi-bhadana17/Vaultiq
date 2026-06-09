package com.kilgore.vaultiq.odds.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Admin-only response DTO for fixture odds.
 * Extends the standard user-facing fields with internal demand pool data
 * (stake pools) that are not exposed to regular users.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixtureOddsAdminResponse {

    private UUID id;
    private UUID fixtureId;
    private String homeTeamName;
    private String awayTeamName;

    // Match Winner
    private BigDecimal homeWinOdds;
    private BigDecimal drawOdds;
    private BigDecimal awayWinOdds;

    // Over/Under
    private BigDecimal over15Odds;
    private BigDecimal under15Odds;
    private BigDecimal over25Odds;
    private BigDecimal under25Odds;
    private BigDecimal over35Odds;
    private BigDecimal under35Odds;

    // Both Teams To Score
    private BigDecimal bttsYesOdds;
    private BigDecimal bttsNoOdds;

    // ── Internal Demand Data (Admin-only) ──

    private BigDecimal totalHomeStake;
    private BigDecimal totalDrawStake;
    private BigDecimal totalAwayStake;

    private LocalDateTime updatedAt;
}
