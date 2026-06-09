package com.kilgore.vaultiq.betting.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsureBetRequest {

    @NotNull(message = "Bet ID is required")
    private UUID betId;
}
