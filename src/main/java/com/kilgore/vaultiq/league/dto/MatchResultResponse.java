package com.kilgore.vaultiq.league.dto;

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
public class MatchResultResponse {

    private UUID id;
    private UUID fixtureId;
    private int homeScore;
    private int awayScore;
    private int homeRedCards;
    private int awayRedCards;
    private int homeYellowCards;
    private int awayYellowCards;
    private boolean isFinal;
    private LocalDateTime updatedAt;
}
