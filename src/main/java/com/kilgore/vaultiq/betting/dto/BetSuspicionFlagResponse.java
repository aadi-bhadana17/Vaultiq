package com.kilgore.vaultiq.betting.dto;

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
public class BetSuspicionFlagResponse {

    private UUID id;
    private UUID userId;
    private String username;
    private UUID betId;
    private String reason;
    private String details;
    private boolean resolved;
    private String resolvedByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
}
