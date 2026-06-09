package com.kilgore.vaultiq.automation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AutoBetRuleRequest {

    @NotNull(message = "Team ID is required")
    private UUID teamId;

    @NotBlank(message = "Outcome is required")
    private String outcome;

    @NotNull(message = "Minimum odds is required")
    @DecimalMin(value = "1.01", message = "Minimum odds must be greater than 1.00")
    private BigDecimal minOdds;

    @NotNull(message = "Stake is required")
    @DecimalMin(value = "0.01", message = "Stake must be at least 0.01")
    private BigDecimal stake;
}
