package com.kilgore.vaultiq.social.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Copy bet request — the follower only specifies which original bet to copy
 * and their stake. The outcome is copied blindly from the tipster's bet.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopyBetRequest {

    @NotNull(message = "Original bet ID is required")
    private UUID originalBetId;

    @NotNull(message = "Stake is required")
    @DecimalMin(value = "0.01", message = "Stake must be at least 0.01")
    private BigDecimal stake;
}
