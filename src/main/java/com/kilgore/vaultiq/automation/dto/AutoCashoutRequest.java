package com.kilgore.vaultiq.automation.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoCashoutRequest {

    @NotNull(message = "Bet ID is required")
    private UUID betId;

    private BigDecimal profitTarget;

    private BigDecimal lossLimit;
}
