package com.kilgore.vaultiq.betting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinSyndicateRequest {

    @NotNull(message = "Contribution is required")
    @DecimalMin(value = "0.01", message = "Contribution must be at least 0.01")
    private BigDecimal contribution;
}
