package com.kilgore.vaultiq.league.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeagueRequest {

    @NotBlank(message = "League name is required")
    @Size(max = 100, message = "League name must not exceed 100 characters")
    private String name;

    private String description;
}
