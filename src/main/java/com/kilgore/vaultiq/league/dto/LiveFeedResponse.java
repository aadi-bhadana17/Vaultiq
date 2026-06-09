package com.kilgore.vaultiq.league.dto;

import com.kilgore.vaultiq.odds.dto.FixtureOddsResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite DTO that bundles a Fixture together with its pre-computed Odds
 * in a single payload, eliminating the N+1 problem on the React dashboard feed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveFeedResponse {

    private FixtureResponse fixture;
    private Object odds; // FixtureOddsResponse or FixtureOddsAdminResponse
}
