package com.kilgore.vaultiq.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public tipster profile response.
 * Exposes performance stats but NOT individual bet outcomes —
 * followers can only see win rate, last 20 results, and bet type categories.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipsterProfileResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private int totalBets;
    private int totalWins;
    private BigDecimal winRate;
    private BigDecimal credibilityScore;
    private BigDecimal cutPercentage;
    private boolean eligible;
    private LocalDateTime createdAt;
}
