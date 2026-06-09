package com.kilgore.vaultiq.betting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BetBuilderResponse {

    private UUID id;
    private BigDecimal combinedOdds;
    private BigDecimal stake;
    private BigDecimal potentialPayout;
    private String status;
    private int totalLegs;
    private int settledLegs;
    private List<BetBuilderLegResponse> legs;
    private LocalDateTime createdAt;
    private LocalDateTime settledAt;
}
