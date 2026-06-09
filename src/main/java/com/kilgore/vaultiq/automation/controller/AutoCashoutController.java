package com.kilgore.vaultiq.automation.controller;

import com.kilgore.vaultiq.automation.dto.AutoCashoutRequest;
import com.kilgore.vaultiq.automation.dto.AutoCashoutResponse;
import com.kilgore.vaultiq.automation.entity.AutoCashoutRule;
import com.kilgore.vaultiq.automation.repository.AutoCashoutRuleRepository;
import com.kilgore.vaultiq.automation.service.CashoutService;
import com.kilgore.vaultiq.betting.entity.Bet;
import com.kilgore.vaultiq.betting.repository.BetRepository;
import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.service.UserService;
import com.kilgore.vaultiq.shared.dto.ApiResponse;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AutoCashoutController {

    private final AutoCashoutRuleRepository ruleRepository;
    private final BetRepository betRepository;
    private final CashoutService cashoutService;
    private final UserService userService;

    @PostMapping("/api/auto-cashout-rules")
    public ResponseEntity<ApiResponse<AutoCashoutResponse>> createRule(
            @Valid @RequestBody AutoCashoutRequest request) {
        User user = userService.getCurrentUser();

        if (request.getProfitTarget() == null && request.getLossLimit() == null) {
            throw new BadRequestException("At least one of profitTarget or lossLimit must be set");
        }

        Bet bet = betRepository.findById(request.getBetId())
                .orElseThrow(() -> new ResourceNotFoundException("Bet", "id", request.getBetId()));

        if (!bet.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not own this bet");
        }

        if (ruleRepository.findByBetId(request.getBetId()).isPresent()) {
            throw new BadRequestException("An auto-cashout rule already exists for this bet");
        }

        AutoCashoutRule rule = AutoCashoutRule.builder()
                .user(user)
                .bet(bet)
                .profitTarget(request.getProfitTarget())
                .lossLimit(request.getLossLimit())
                .active(true)
                .build();

        rule = ruleRepository.save(rule);

        return ResponseEntity.ok(ApiResponse.success("Auto-cashout rule created", mapToResponse(rule)));
    }

    @GetMapping("/api/auto-cashout-rules")
    public ResponseEntity<ApiResponse<List<AutoCashoutResponse>>> getUserRules(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = userService.getCurrentUser();
        Page<AutoCashoutRule> rules = ruleRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(page, size));
        List<AutoCashoutResponse> responses = rules.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @DeleteMapping("/api/auto-cashout-rules/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(@PathVariable UUID id) {
        User user = userService.getCurrentUser();
        AutoCashoutRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AutoCashoutRule", "id", id));

        if (!rule.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not own this rule");
        }

        ruleRepository.delete(rule);
        return ResponseEntity.ok(ApiResponse.success("Rule deleted", null));
    }

    @GetMapping("/api/bets/{betId}/cashout-value")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getCashoutValue(@PathVariable UUID betId) {
        BigDecimal value = cashoutService.calculateCashoutValue(betId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("cashoutValue", value)));
    }

    @PostMapping("/api/bets/{betId}/cashout")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> manualCashout(@PathVariable UUID betId) {
        BigDecimal amount = cashoutService.executeCashout(betId);
        return ResponseEntity.ok(ApiResponse.success("Bet cashed out", Map.of("cashoutAmount", amount)));
    }

    private AutoCashoutResponse mapToResponse(AutoCashoutRule rule) {
        return AutoCashoutResponse.builder()
                .id(rule.getId())
                .betId(rule.getBet().getId())
                .profitTarget(rule.getProfitTarget())
                .lossLimit(rule.getLossLimit())
                .active(rule.isActive())
                .triggeredAt(rule.getTriggeredAt())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}
