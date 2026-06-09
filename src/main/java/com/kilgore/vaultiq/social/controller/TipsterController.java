package com.kilgore.vaultiq.social.controller;

import com.kilgore.vaultiq.shared.dto.ApiResponse;
import com.kilgore.vaultiq.social.dto.TipsterBetPreview;
import com.kilgore.vaultiq.social.dto.TipsterFollowerResponse;
import com.kilgore.vaultiq.social.dto.TipsterProfileResponse;
import com.kilgore.vaultiq.social.service.CopyBetService;
import com.kilgore.vaultiq.social.service.FollowerService;
import com.kilgore.vaultiq.social.service.TipsterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tipsters")
@RequiredArgsConstructor
public class TipsterController {

    private final TipsterService tipsterService;
    private final FollowerService followerService;
    private final CopyBetService copyBetService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<TipsterProfileResponse>> registerAsTipster() {
        TipsterProfileResponse response = tipsterService.registerAsTipster();
        return ResponseEntity.ok(ApiResponse.success("Registered as tipster", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TipsterProfileResponse>>> browseTipsters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<TipsterProfileResponse> tipsters = tipsterService.browseTipsters(page, size);
        return ResponseEntity.ok(ApiResponse.success(tipsters));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<TipsterProfileResponse>> getTipsterProfile(@PathVariable UUID userId) {
        TipsterProfileResponse response = tipsterService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<TipsterFollowerResponse>> followTipster(@PathVariable UUID userId) {
        TipsterFollowerResponse response = followerService.follow(userId);
        return ResponseEntity.ok(ApiResponse.success("Now following tipster", response));
    }

    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<ApiResponse<Void>> unfollowTipster(@PathVariable UUID userId) {
        followerService.unfollow(userId);
        return ResponseEntity.ok(ApiResponse.success("Unfollowed tipster", null));
    }

    @GetMapping("/following")
    public ResponseEntity<ApiResponse<List<TipsterFollowerResponse>>> getFollowedTipsters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<TipsterFollowerResponse> follows = followerService.getFollowedTipsters(page, size);
        return ResponseEntity.ok(ApiResponse.success(follows));
    }

    /**
     * Get tipster's bet previews — shows bet TYPE but NOT the outcome.
     * Followers use this to decide which bets to copy.
     */
    @GetMapping("/{userId}/bets")
    public ResponseEntity<ApiResponse<List<TipsterBetPreview>>> getTipsterBets(@PathVariable UUID userId) {
        List<TipsterBetPreview> previews = copyBetService.getTipsterBetPreviews(userId);
        return ResponseEntity.ok(ApiResponse.success(previews));
    }
}
