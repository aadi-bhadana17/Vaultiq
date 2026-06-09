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
public class AutoCashoutResponse {

    private UUID id;
    private UUID betId;
    private BigDecimal profitTarget;
    private BigDecimal lossLimit;
    private boolean active;
    private LocalDateTime triggeredAt;
    private LocalDateTime createdAt;
}
