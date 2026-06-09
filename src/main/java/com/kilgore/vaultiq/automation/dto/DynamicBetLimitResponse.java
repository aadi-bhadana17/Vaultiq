package com.kilgore.vaultiq.automation.dto;

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
public class DynamicBetLimitResponse {

    private UUID id;
    private UUID userId;
    private BigDecimal maxSingleBet;
    private BigDecimal maxDailyTotal;
    private BigDecimal riskScore;
    private LocalDateTime lastRecalculatedAt;
}
