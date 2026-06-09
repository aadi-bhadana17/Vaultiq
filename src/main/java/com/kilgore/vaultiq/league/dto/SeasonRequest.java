package com.kilgore.vaultiq.league.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonRequest {

    @NotNull(message = "League ID is required")
    private UUID leagueId;

    @NotBlank(message = "Season name is required")
    @Size(max = 100, message = "Season name must not exceed 100 characters")
    private String name;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    private LocalDate endDate;
}
