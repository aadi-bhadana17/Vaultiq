package com.kilgore.vaultiq.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Copy bet response — shows bet details but outcome is HIDDEN until fixture finishes.
 * Before settlement: outcome = "HIDDEN", status = "PENDING"
 * After settlement: outcome is revealed, status = "WON" or "LOST"
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopyBetResponse {

    private UUID id;
    private UUID copyBetId;
    private UUID originalBetId;
    private String tipsterUsername;
    private UUID fixtureId;
    private String homeTeamName;
    private String awayTeamName;
    private String betType;        // e.g. MATCH_RESULT, OVER_UNDER, BTTS
    private String outcome;        // "HIDDEN" until fixture finishes
    private BigDecimal oddsAtPlacement;
    private BigDecimal stake;
    private BigDecimal potentialPayout;
    private String status;
    private BigDecimal tipsterCut;
    private LocalDateTime createdAt;
}
