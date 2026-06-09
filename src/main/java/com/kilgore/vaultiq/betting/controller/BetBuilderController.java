package com.kilgore.vaultiq.betting.controller;

import com.kilgore.vaultiq.betting.dto.BetBuilderRequest;
import com.kilgore.vaultiq.betting.dto.BetBuilderResponse;
import com.kilgore.vaultiq.betting.service.BetBuilderService;
import com.kilgore.vaultiq.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bet-builders")
@RequiredArgsConstructor
public class BetBuilderController {

    private final BetBuilderService betBuilderService;

    @PostMapping
    public ResponseEntity<ApiResponse<BetBuilderResponse>> placeBetBuilder(
            @Valid @RequestBody BetBuilderRequest request) {
        BetBuilderResponse response = betBuilderService.placeBetBuilder(request);
        return ResponseEntity.ok(ApiResponse.success("Bet builder placed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BetBuilderResponse>>> getUserBetBuilders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<BetBuilderResponse> builders = betBuilderService.getUserBetBuilders(page, size);
        return ResponseEntity.ok(ApiResponse.success(builders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BetBuilderResponse>> getBetBuilderById(@PathVariable UUID id) {
        BetBuilderResponse response = betBuilderService.getBetBuilderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
