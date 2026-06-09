package com.kilgore.vaultiq.betting.dto;

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
public class TransactionResponse {

    private UUID id;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private UUID referenceId;
    private String referenceType;
    private String description;
    private LocalDateTime createdAt;
}
