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
public class TeamResponse {

    private UUID id;
    private UUID seasonId;
    private String seasonName;
    private String name;
    private int strength;
    private LocalDateTime createdAt;
}
