package com.kilgore.vaultiq.league.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreUpdateRequest {

    @NotNull(message = "Home score is required")
    @Min(value = 0, message = "Home score cannot be negative")
    private Integer homeScore;

    @NotNull(message = "Away score is required")
    @Min(value = 0, message = "Away score cannot be negative")
    private Integer awayScore;

    @Min(value = 0, message = "Red cards cannot be negative")
    private Integer homeRedCards = 0;

    @Min(value = 0, message = "Red cards cannot be negative")
    private Integer awayRedCards = 0;

    @Min(value = 0, message = "Yellow cards cannot be negative")
    private Integer homeYellowCards = 0;

    @Min(value = 0, message = "Yellow cards cannot be negative")
    private Integer awayYellowCards = 0;

    /**
     * Current match minute (0–90+). Used for in-play tracking.
     */
    @NotNull(message = "Match minute is required")
    @Min(value = 0, message = "Match minute cannot be negative")
    private Integer matchMinute;
}
