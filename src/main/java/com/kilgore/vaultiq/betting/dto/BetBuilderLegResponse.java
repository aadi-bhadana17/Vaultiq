package com.kilgore.vaultiq.betting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetBuilderLegResponse {

    private UUID id;
    private UUID fixtureId;
    private String homeTeamName;
    private String awayTeamName;
    private String outcome;
    private String outcomeCategory;
    private BigDecimal oddsAtPlacement;
    private String result;
}
