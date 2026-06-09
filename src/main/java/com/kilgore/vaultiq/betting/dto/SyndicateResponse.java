package com.kilgore.vaultiq.betting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyndicateResponse {

    private UUID id;
    private String name;
    private String creatorUsername;
    private BigDecimal targetStake;
    private BigDecimal currentPool;
    private String status;
    private List<SyndicateMemberResponse> members;
    private SyndicateBetResponse bet;
    private LocalDateTime createdAt;
}
