package com.kilgore.vaultiq.league.controller;

import com.kilgore.vaultiq.league.dto.SeasonRequest;
import com.kilgore.vaultiq.league.dto.SeasonResponse;
import com.kilgore.vaultiq.league.service.SeasonService;
import com.kilgore.vaultiq.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/seasons")
@RequiredArgsConstructor
public class SeasonController {

    private final SeasonService seasonService;

    @PostMapping
    @PreAuthorize("hasAnyRole('LEAGUE_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<SeasonResponse>> createSeason(@Valid @RequestBody SeasonRequest request) {
        SeasonResponse response = seasonService.createSeason(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Season created successfully", response));
    }

    @GetMapping("/league/{leagueId}")
    public ResponseEntity<ApiResponse<List<SeasonResponse>>> getSeasonsByLeague(@PathVariable UUID leagueId) {
        List<SeasonResponse> seasons = seasonService.getSeasonsByLeague(leagueId);
        return ResponseEntity.ok(ApiResponse.success(seasons));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SeasonResponse>> getSeasonById(@PathVariable UUID id) {
        SeasonResponse response = seasonService.getSeasonById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/league/{leagueId}/active")
    public ResponseEntity<ApiResponse<SeasonResponse>> getActiveSeason(@PathVariable UUID leagueId) {
        SeasonResponse response = seasonService.getActiveSeasonByLeague(leagueId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('LEAGUE_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<SeasonResponse>> deactivateSeason(@PathVariable UUID id) {
        SeasonResponse response = seasonService.deactivateSeason(id);
        return ResponseEntity.ok(ApiResponse.success("Season deactivated", response));
    }
}
