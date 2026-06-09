package com.kilgore.vaultiq.automation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoBetRuleResponse {

    private UUID id;
    private UUID teamId;
    private String teamName;
    private String outcome;
    private BigDecimal minOdds;
    private BigDecimal stake;
    private boolean active;
    private LocalDateTime lastTriggeredAt;
    private LocalDateTime createdAt;
}
