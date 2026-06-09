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
public class LeagueResponse {

    private UUID id;
    private String name;
    private String description;
    private String createdByUsername;
    private int seasonCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
