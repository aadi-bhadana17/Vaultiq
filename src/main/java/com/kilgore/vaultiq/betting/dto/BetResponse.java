package com.kilgore.vaultiq.betting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetResponse {

    private UUID id;
    private UUID fixtureId;
    private String homeTeamName;
    private String awayTeamName;
    private String betType;
    private String outcome;
    private BigDecimal oddsAtPlacement;
    private BigDecimal stake;
    private BigDecimal potentialPayout;
    private String status;
    private boolean cashedOut;
    private BigDecimal cashoutAmount;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;
}
