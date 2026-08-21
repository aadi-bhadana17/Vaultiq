package com.kilgore.vaultiq.league.controller;

import com.kilgore.vaultiq.league.dto.LeagueRequest;
import com.kilgore.vaultiq.league.dto.LeagueResponse;
import com.kilgore.vaultiq.league.service.LeagueService;
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
@RequestMapping("/api/leagues")
@RequiredArgsConstructor
public class LeagueController {

    private final LeagueService leagueService;

    @PostMapping
    @PreAuthorize("hasAnyRole('LEAGUE_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<LeagueResponse>> createLeague(@Valid @RequestBody LeagueRequest request) {
        LeagueResponse response = leagueService.createLeague(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("League created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LeagueResponse>>> getAllLeagues() {
        List<LeagueResponse> leagues = leagueService.getAllLeagues();
        return ResponseEntity.ok(ApiResponse.success(leagues));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('LEAGUE_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<LeagueResponse>>> getMyLeagues() {
        List<LeagueResponse> leagues = leagueService.getMyLeagues();
        return ResponseEntity.ok(ApiResponse.success(leagues));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LeagueResponse>> getLeagueById(@PathVariable UUID id) {
        LeagueResponse response = leagueService.getLeagueById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LEAGUE_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<LeagueResponse>> updateLeague(
            @PathVariable UUID id, @Valid @RequestBody LeagueRequest request) {
        LeagueResponse response = leagueService.updateLeague(id, request);
        return ResponseEntity.ok(ApiResponse.success("League updated successfully", response));
    }
}
