package com.kilgore.vaultiq.betting.controller;

import com.kilgore.vaultiq.betting.dto.BetInsuranceResponse;
import com.kilgore.vaultiq.betting.service.BetInsuranceService;
import com.kilgore.vaultiq.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/bets")
@RequiredArgsConstructor
public class BetInsuranceController {

    private final BetInsuranceService insuranceService;

    @PostMapping("/{betId}/insure")
    public ResponseEntity<ApiResponse<BetInsuranceResponse>> insureBet(@PathVariable UUID betId) {
        BetInsuranceResponse response = insuranceService.insureBet(betId);
        return ResponseEntity.ok(ApiResponse.success("Bet insured successfully", response));
    }

    @GetMapping("/{betId}/insurance")
    public ResponseEntity<ApiResponse<BetInsuranceResponse>> getInsurance(@PathVariable UUID betId) {
        BetInsuranceResponse response = insuranceService.getInsurance(betId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
