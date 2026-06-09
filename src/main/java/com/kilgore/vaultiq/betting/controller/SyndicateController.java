package com.kilgore.vaultiq.betting.controller;

import com.kilgore.vaultiq.betting.dto.*;
import com.kilgore.vaultiq.betting.service.SyndicateService;
import com.kilgore.vaultiq.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/syndicates")
@RequiredArgsConstructor
public class SyndicateController {

    private final SyndicateService syndicateService;

    @PostMapping
    public ResponseEntity<ApiResponse<SyndicateResponse>> createSyndicate(
            @Valid @RequestBody CreateSyndicateRequest request) {
        SyndicateResponse response = syndicateService.createSyndicate(request);
        return ResponseEntity.ok(ApiResponse.success("Syndicate created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SyndicateResponse>>> browseOpenSyndicates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<SyndicateResponse> syndicates = syndicateService.browseOpenSyndicates(page, size);
        return ResponseEntity.ok(ApiResponse.success(syndicates));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SyndicateResponse>> getSyndicateById(@PathVariable UUID id) {
        SyndicateResponse response = syndicateService.getSyndicateById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<SyndicateResponse>> joinSyndicate(
            @PathVariable UUID id,
            @Valid @RequestBody JoinSyndicateRequest request) {
        SyndicateResponse response = syndicateService.joinSyndicate(id, request);
        return ResponseEntity.ok(ApiResponse.success("Joined syndicate", response));
    }

    @PostMapping("/{id}/bet")
    public ResponseEntity<ApiResponse<SyndicateResponse>> placeSyndicateBet(
            @PathVariable UUID id,
            @Valid @RequestBody PlaceSyndicateBetRequest request) {
        SyndicateResponse response = syndicateService.placeSyndicateBet(id, request);
        return ResponseEntity.ok(ApiResponse.success("Syndicate bet placed", response));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SyndicateResponse>> cancelSyndicate(@PathVariable UUID id) {
        SyndicateResponse response = syndicateService.cancelSyndicate(id);
        return ResponseEntity.ok(ApiResponse.success("Syndicate cancelled — members refunded", response));
    }
}
