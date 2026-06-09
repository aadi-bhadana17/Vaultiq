package com.kilgore.vaultiq.betting.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BetBuilderRequest {

    @NotNull(message = "Legs are required")
    @Size(min = 2, message = "Bet builder requires at least 2 legs")
    @Valid
    private List<BetBuilderLegRequest> legs;

    @NotNull(message = "Stake is required")
    @DecimalMin(value = "0.01", message = "Stake must be at least 0.01")
    private BigDecimal stake;
}
