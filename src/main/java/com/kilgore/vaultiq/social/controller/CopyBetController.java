package com.kilgore.vaultiq.social.controller;

import com.kilgore.vaultiq.shared.dto.ApiResponse;
import com.kilgore.vaultiq.social.dto.CopyBetRequest;
import com.kilgore.vaultiq.social.dto.CopyBetResponse;
import com.kilgore.vaultiq.social.service.CopyBetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/copy-bets")
@RequiredArgsConstructor
public class CopyBetController {

    private final CopyBetService copyBetService;

    @PostMapping
    public ResponseEntity<ApiResponse<CopyBetResponse>> placeCopyBet(
            @Valid @RequestBody CopyBetRequest request) {
        CopyBetResponse response = copyBetService.placeCopyBet(request);
        return ResponseEntity.ok(ApiResponse.success("Copy bet placed", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CopyBetResponse>>> getCopyBetHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<CopyBetResponse> copyBets = copyBetService.getCopyBetHistory(page, size);
        return ResponseEntity.ok(ApiResponse.success(copyBets));
    }
}
