package com.kilgore.vaultiq.betting.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceRequest {
    
    @NotNull(message = "Coverage percentage is required")
    @DecimalMin(value = "10.00", message = "Coverage must be at least 10%")
    @DecimalMax(value = "100.00", message = "Coverage cannot exceed 100%")
    private BigDecimal coveragePercentage;
}
