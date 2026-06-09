package com.kilgore.vaultiq.league.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonResponse {

    private UUID id;
    private UUID leagueId;
    private String leagueName;
    private String name;
    private boolean active;
    private LocalDate startDate;
    private LocalDate endDate;
    private int teamCount;
    private int fixtureCount;
    private LocalDateTime createdAt;
}
