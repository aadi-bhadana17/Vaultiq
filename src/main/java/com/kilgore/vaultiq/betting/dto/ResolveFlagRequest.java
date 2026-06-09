package com.kilgore.vaultiq.betting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolveFlagRequest {

    private boolean unrestrictUser;
}
