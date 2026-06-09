package com.kilgore.vaultiq.betting.dto;

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
public class PlaceBetRequest {

    @NotNull(message = "Fixture ID is required")
    private UUID fixtureId;

    @NotBlank(message = "Outcome is required")
    private String outcome;

    @NotNull(message = "Stake is required")
    @DecimalMin(value = "0.01", message = "Stake must be at least 0.01")
    private BigDecimal stake;
}
