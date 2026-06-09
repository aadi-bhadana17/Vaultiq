package com.kilgore.vaultiq.social.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Preview of a tipster's bet for potential followers.
 * Shows fixture info and bet TYPE (category) but NOT the chosen outcome.
 * This prevents followers from seeing and replicating the bet without paying the cut.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipsterBetPreview {

    private UUID betId;
    private UUID fixtureId;
    private String homeTeamName;
    private String awayTeamName;
    private String betType;         // MATCH_RESULT, OVER_UNDER, BTTS — NOT the specific outcome
    private String fixtureStatus;   // OPEN, FINISHED, etc.
    private String betStatus;       // PENDING, WON, LOST — but outcome remains hidden until finished
}
