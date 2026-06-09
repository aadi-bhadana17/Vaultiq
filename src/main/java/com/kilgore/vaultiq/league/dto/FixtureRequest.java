package com.kilgore.vaultiq.league.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixtureRequest {

    @NotNull(message = "Season ID is required")
    private UUID seasonId;

    @NotNull(message = "Home team ID is required")
    private UUID homeTeamId;

    @NotNull(message = "Away team ID is required")
    private UUID awayTeamId;

    @NotNull(message = "Scheduled time is required")
    private LocalDateTime scheduledAt;
}
