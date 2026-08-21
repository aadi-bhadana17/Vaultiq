package com.kilgore.vaultiq.betting.controller;

import com.kilgore.vaultiq.betting.dto.BetSuspicionFlagResponse;
import com.kilgore.vaultiq.betting.dto.ResolveFlagRequest;
import com.kilgore.vaultiq.betting.entity.BetSuspicionFlag;
import com.kilgore.vaultiq.betting.repository.BetSuspicionFlagRepository;
import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.repository.UserRepository;
import com.kilgore.vaultiq.identity.service.UserService;
import com.kilgore.vaultiq.shared.dto.ApiResponse;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/suspicion-flags")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class BetSuspicionController {

    private final BetSuspicionFlagRepository flagRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<BetSuspicionFlagResponse>>> getUnresolvedFlags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BetSuspicionFlag> flags = flagRepository.findByResolvedFalseOrderByCreatedAtDesc(
                PageRequest.of(page, size));
        List<BetSuspicionFlagResponse> responses = flags.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/resolved")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<BetSuspicionFlagResponse>>> getResolvedFlags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BetSuspicionFlag> flags = flagRepository.findByResolvedTrueOrderByCreatedAtDesc(
                PageRequest.of(page, size));
        List<BetSuspicionFlagResponse> responses = flags.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/user/{userId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<BetSuspicionFlagResponse>>> getFlagsByUser(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BetSuspicionFlag> flags = flagRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(page, size));
        List<BetSuspicionFlagResponse> responses = flags.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/resolve")
    @Transactional
    public ResponseEntity<ApiResponse<BetSuspicionFlagResponse>> resolveFlag(
            @PathVariable UUID id,
            @RequestBody ResolveFlagRequest request) {
        BetSuspicionFlag flag = flagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BetSuspicionFlag", "id", id));

        User admin = userService.getCurrentUser();
        flag.setResolved(true);
        flag.setResolvedBy(admin);
        flag.setResolvedAt(LocalDateTime.now());
        flagRepository.save(flag);

        // Optionally unrestrict the user
        if (request.isUnrestrictUser()) {
            User flaggedUser = flag.getUser();
            flaggedUser.setBettingRestricted(false);
            userRepository.save(flaggedUser);
        }

        return ResponseEntity.ok(ApiResponse.success("Flag resolved", mapToResponse(flag)));
    }

    private BetSuspicionFlagResponse mapToResponse(BetSuspicionFlag flag) {
        return BetSuspicionFlagResponse.builder()
                .id(flag.getId())
                .userId(flag.getUser().getId())
                .username(flag.getUser().getUsername())
                .betId(flag.getBet() != null ? flag.getBet().getId() : null)
                .reason(flag.getReason().name())
                .details(flag.getDetails())
                .resolved(flag.isResolved())
                .resolvedByUsername(flag.getResolvedBy() != null ? flag.getResolvedBy().getUsername() : null)
                .createdAt(flag.getCreatedAt())
                .resolvedAt(flag.getResolvedAt())
                .build();
    }
}
