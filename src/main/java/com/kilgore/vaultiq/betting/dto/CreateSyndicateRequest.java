package com.kilgore.vaultiq.betting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateSyndicateRequest {

    @NotBlank(message = "Syndicate name is required")
    private String name;

    @NotNull(message = "Target stake is required")
    @DecimalMin(value = "1.00", message = "Target stake must be at least 1.00")
    private BigDecimal targetStake;
}
