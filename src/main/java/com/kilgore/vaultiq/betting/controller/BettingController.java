package com.kilgore.vaultiq.betting.controller;

import com.kilgore.vaultiq.betting.dto.BetResponse;
import com.kilgore.vaultiq.betting.dto.PlaceBetRequest;
import com.kilgore.vaultiq.betting.service.BettingService;
import com.kilgore.vaultiq.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
public class BettingController {

    private final BettingService bettingService;

    @PostMapping
    public ResponseEntity<ApiResponse<BetResponse>> placeBet(@Valid @RequestBody PlaceBetRequest request) {
        BetResponse response = bettingService.placeBet(request);
        return ResponseEntity.ok(ApiResponse.success("Bet placed successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BetResponse>>> getUserBets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<BetResponse> bets = bettingService.getUserBets(page, size);
        return ResponseEntity.ok(ApiResponse.success(bets));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BetResponse>> getBetById(@PathVariable UUID id) {
        BetResponse response = bettingService.getBetById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
