package com.kilgore.vaultiq.odds.controller;

import com.kilgore.vaultiq.odds.dto.FixtureOddsResponse;
import com.kilgore.vaultiq.odds.service.OddsService;
import com.kilgore.vaultiq.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;


@RestController
@RequestMapping("/api/odds")
@RequiredArgsConstructor
public class OddsController {

    private final OddsService oddsService;

    @GetMapping("/fixture/{fixtureId}")
    public ResponseEntity<ApiResponse<FixtureOddsResponse>> getOddsForFixture(@PathVariable UUID fixtureId) {
        FixtureOddsResponse response = oddsService.getOddsForFixture(fixtureId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
