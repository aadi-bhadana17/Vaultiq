package com.kilgore.vaultiq.identity.dto;

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
public class UserProfileResponse {

    private UUID id;
    private String username;
    private String email;
    private String role;
    private BigDecimal walletBalance;
    private boolean bettingRestricted;
    private int accountAgeDays;
    private LocalDateTime createdAt;
}
