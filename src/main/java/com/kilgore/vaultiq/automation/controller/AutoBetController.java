package com.kilgore.vaultiq.automation.controller;

import com.kilgore.vaultiq.automation.dto.AutoBetRuleRequest;
import com.kilgore.vaultiq.automation.dto.AutoBetRuleResponse;
import com.kilgore.vaultiq.automation.service.AutoBetService;
import com.kilgore.vaultiq.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auto-bet-rules")
@RequiredArgsConstructor
public class AutoBetController {

    private final AutoBetService autoBetService;

    @PostMapping
    public ResponseEntity<ApiResponse<AutoBetRuleResponse>> createRule(
            @Valid @RequestBody AutoBetRuleRequest request) {
        AutoBetRuleResponse response = autoBetService.createRule(request);
        return ResponseEntity.ok(ApiResponse.success("Auto-bet rule created", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AutoBetRuleResponse>>> getUserRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<AutoBetRuleResponse> rules = autoBetService.getUserRules(page, size);
        return ResponseEntity.ok(ApiResponse.success(rules));
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<AutoBetRuleResponse>> toggleRule(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        AutoBetRuleResponse response = autoBetService.toggleRule(id, active);
        return ResponseEntity.ok(ApiResponse.success(
                active ? "Rule activated" : "Rule deactivated", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable UUID id) {
        autoBetService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.success("Rule deleted", null));
    }
}
