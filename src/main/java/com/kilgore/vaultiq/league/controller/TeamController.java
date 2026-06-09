package com.kilgore.vaultiq.league.controller;

import com.kilgore.vaultiq.league.dto.TeamRequest;
import com.kilgore.vaultiq.league.dto.TeamResponse;
import com.kilgore.vaultiq.league.service.TeamService;
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
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @PreAuthorize("hasAnyRole('LEAGUE_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(@Valid @RequestBody TeamRequest request) {
        TeamResponse response = teamService.createTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Team created successfully", response));
    }

    @GetMapping("/season/{seasonId}")
    public ResponseEntity<ApiResponse<List<TeamResponse>>> getTeamsBySeason(@PathVariable UUID seasonId) {
        List<TeamResponse> teams = teamService.getTeamsBySeason(seasonId);
        return ResponseEntity.ok(ApiResponse.success(teams));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeamById(@PathVariable UUID id) {
        TeamResponse response = teamService.getTeamById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LEAGUE_ADMIN', 'ADMIN')")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
            @PathVariable UUID id, @Valid @RequestBody TeamRequest request) {
        TeamResponse response = teamService.updateTeam(id, request);
        return ResponseEntity.ok(ApiResponse.success("Team updated successfully", response));
    }
}
