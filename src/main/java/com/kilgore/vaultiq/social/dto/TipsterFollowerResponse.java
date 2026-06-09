package com.kilgore.vaultiq.social.dto;

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
public class TipsterFollowerResponse {

    private UUID id;
    private UUID tipsterUserId;
    private String tipsterUsername;
    private boolean active;
    private LocalDateTime followedAt;
}
