package com.kilgore.vaultiq.betting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceSyndicateBetRequest {

    @NotNull(message = "Fixture ID is required")
    private UUID fixtureId;

    @NotBlank(message = "Outcome is required")
    private String outcome;
}
