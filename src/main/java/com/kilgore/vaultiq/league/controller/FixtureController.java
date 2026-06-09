package com.kilgore.vaultiq.league.controller;

import com.kilgore.vaultiq.league.dto.FixtureRequest;
import com.kilgore.vaultiq.league.dto.FixtureResponse;
import com.kilgore.vaultiq.league.dto.LiveFeedResponse;
import com.kilgore.vaultiq.league.dto.MatchResultResponse;
import com.kilgore.vaultiq.league.dto.ScoreUpdateRequest;
import com.kilgore.vaultiq.league.entity.FixtureStatus;
import com.kilgore.vaultiq.league.service.FixtureService;
import com.kilgore.vaultiq.league.service.MatchResultService;
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
@RequestMapping("/api/fixtures")
@RequiredArgsConstructor
public class FixtureController {

    private final FixtureService fixtureService;
    private final MatchResultService matchResultService;

    // ── Live Feed (public — used by React Dashboard) ──

    @GetMapping("/feed")
    public ResponseEntity<ApiResponse<List<LiveFeedResponse>>> getLiveFeed() {
        List<LiveFeedResponse> feed = fixtureService.getLiveFeed();
        return ResponseEntity.ok(ApiResponse.success(feed));
    }

    @GetMapping("/feed/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LiveFeedResponse>>> getAdminFeed() {
        List<LiveFeedResponse> feed = fixtureService.getAdminLiveFeed();
        return ResponseEntity.ok(ApiResponse.success(feed));
    }

    // ── Fixture CRUD ──

    @PostMapping
    @PreAuthorize("hasAnyRole('LEAGUE_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<FixtureResponse>> createFixture(@Valid @RequestBody FixtureRequest request) {
        FixtureResponse response = fixtureService.createFixture(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fixture created successfully", response));
    }

    @GetMapping("/season/{seasonId}")
    public ResponseEntity<ApiResponse<List<FixtureResponse>>> getFixturesBySeason(@PathVariable UUID seasonId) {
        List<FixtureResponse> fixtures = fixtureService.getFixturesBySeason(seasonId);
        return ResponseEntity.ok(ApiResponse.success(fixtures));
    }

    @GetMapping("/season/{seasonId}/status/{status}")
    public ResponseEntity<ApiResponse<List<FixtureResponse>>> getFixturesBySeasonAndStatus(
            @PathVariable UUID seasonId, @PathVariable FixtureStatus status) {
        List<FixtureResponse> fixtures = fixtureService.getFixturesBySeasonAndStatus(seasonId, status);
        return ResponseEntity.ok(ApiResponse.success(fixtures));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<FixtureResponse>> getFixtureById(@PathVariable UUID id) {
        FixtureResponse response = fixtureService.getFixtureById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Status management ──

    @PatchMapping("/{id}/status/{status}")
    @PreAuthorize("hasAnyRole('LEAGUE_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<FixtureResponse>> updateFixtureStatus(
            @PathVariable UUID id, @PathVariable FixtureStatus status) {
        FixtureResponse response = fixtureService.updateFixtureStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success("Fixture status updated to " + status, response));
    }

    // ── Score management ──

    @PutMapping("/{id}/score")
    @PreAuthorize("hasAnyRole('LEAGUE_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<MatchResultResponse>> updateScore(
            @PathVariable UUID id, @Valid @RequestBody ScoreUpdateRequest request) {
        MatchResultResponse response = matchResultService.updateScore(id, request);
        return ResponseEntity.ok(ApiResponse.success("Score updated", response));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<ApiResponse<MatchResultResponse>> getMatchResult(@PathVariable UUID id) {
        MatchResultResponse response = matchResultService.getMatchResult(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/finish")
    @PreAuthorize("hasAnyRole('LEAGUE_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<MatchResultResponse>> finishFixture(@PathVariable UUID id) {
        MatchResultResponse response = matchResultService.finishFixture(id);
        return ResponseEntity.ok(ApiResponse.success("Fixture finished — result is final", response));
    }
}
