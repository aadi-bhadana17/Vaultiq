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
public class FixtureResponse {

    private UUID id;
    private UUID seasonId;
    private String homeTeamName;
    private UUID homeTeamId;
    private String awayTeamName;
    private UUID awayTeamId;
    private int matchMinute;
    private String status;
    private LocalDateTime scheduledAt;
    private MatchResultResponse matchResult;
    private java.math.BigDecimal platformProfit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
