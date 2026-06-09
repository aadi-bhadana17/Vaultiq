package com.kilgore.vaultiq.social.service;

import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.service.UserService;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import com.kilgore.vaultiq.social.dto.TipsterFollowerResponse;
import com.kilgore.vaultiq.social.entity.TipsterFollower;
import com.kilgore.vaultiq.social.entity.TipsterProfile;
import com.kilgore.vaultiq.social.repository.TipsterFollowerRepository;
import com.kilgore.vaultiq.social.repository.TipsterProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FollowerService {

    private final TipsterFollowerRepository followerRepository;
    private final TipsterProfileRepository tipsterRepository;
    private final UserService userService;

    @Transactional
    public TipsterFollowerResponse follow(UUID tipsterUserId) {
        User follower = userService.getCurrentUser();

        if (follower.getId().equals(tipsterUserId)) {
            throw new BadRequestException("You cannot follow yourself");
        }

        TipsterProfile tipster = tipsterRepository.findByUserId(tipsterUserId)
                .orElseThrow(() -> new ResourceNotFoundException("TipsterProfile", "userId", tipsterUserId));

        if (!tipster.isEligible()) {
            throw new BadRequestException("This tipster is not currently eligible for copying");
        }

        // Check if already following (reactivate if inactive)
        var existingFollow = followerRepository.findByTipsterIdAndFollowerId(tipster.getId(), follower.getId());
        if (existingFollow.isPresent()) {
            TipsterFollower existing = existingFollow.get();
            if (existing.isActive()) {
                throw new BadRequestException("You are already following this tipster");
            }
            // Reactivate
            existing.setActive(true);
            followerRepository.save(existing);
            log.info("User {} re-followed tipster {}", follower.getUsername(), tipster.getUser().getUsername());
            return mapToResponse(existing);
        }

        TipsterFollower follow = TipsterFollower.builder()
                .tipster(tipster)
                .follower(follower)
                .active(true)
                .build();

        follow = followerRepository.save(follow);

        log.info("User {} followed tipster {}", follower.getUsername(), tipster.getUser().getUsername());

        return mapToResponse(follow);
    }

    @Transactional
    public void unfollow(UUID tipsterUserId) {
        User follower = userService.getCurrentUser();

        TipsterProfile tipster = tipsterRepository.findByUserId(tipsterUserId)
                .orElseThrow(() -> new ResourceNotFoundException("TipsterProfile", "userId", tipsterUserId));

        TipsterFollower follow = followerRepository
                .findByTipsterIdAndFollowerId(tipster.getId(), follower.getId())
                .orElseThrow(() -> new BadRequestException("You are not following this tipster"));

        follow.setActive(false);
        followerRepository.save(follow);

        log.info("User {} unfollowed tipster {}", follower.getUsername(), tipster.getUser().getUsername());
    }

    @Transactional(readOnly = true)
    public List<TipsterFollowerResponse> getFollowedTipsters(int page, int size) {
        User follower = userService.getCurrentUser();
        Page<TipsterFollower> follows = followerRepository.findByFollowerIdAndActiveTrue(
                follower.getId(), PageRequest.of(page, size));

        return follows.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TipsterFollowerResponse mapToResponse(TipsterFollower follow) {
        return TipsterFollowerResponse.builder()
                .id(follow.getId())
                .tipsterUserId(follow.getTipster().getUser().getId())
                .tipsterUsername(follow.getTipster().getUser().getUsername())
                .active(follow.isActive())
                .followedAt(follow.getFollowedAt())
                .build();
    }
}
