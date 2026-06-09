package com.kilgore.vaultiq.automation.controller;

import com.kilgore.vaultiq.automation.dto.DynamicBetLimitResponse;
import com.kilgore.vaultiq.automation.service.DynamicBetLimitService;
import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.service.UserService;
import com.kilgore.vaultiq.shared.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class DynamicBetLimitController {

    private final DynamicBetLimitService limitService;
    private final UserService userService;

    @GetMapping("/api/bet-limits")
    public ResponseEntity<ApiResponse<DynamicBetLimitResponse>> getMyLimits() {
        User user = userService.getCurrentUser();
        DynamicBetLimitResponse response = limitService.getLimits(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/admin/bet-limits/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DynamicBetLimitResponse>> getAdminLimits(@PathVariable UUID userId) {
        DynamicBetLimitResponse response = limitService.getLimits(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
