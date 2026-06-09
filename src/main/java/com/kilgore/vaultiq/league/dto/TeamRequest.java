package com.kilgore.vaultiq.league.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamRequest {

    @NotNull(message = "Season ID is required")
    private UUID seasonId;

    @NotBlank(message = "Team name is required")
    @Size(max = 100, message = "Team name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Strength is required")
    @Min(value = 100, message = "Strength must be at least 100")
    @Max(value = 1000, message = "Strength must be at most 1000")
    private Integer strength;
}
