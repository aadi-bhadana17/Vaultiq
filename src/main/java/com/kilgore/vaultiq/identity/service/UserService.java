package com.kilgore.vaultiq.identity.service;

import com.kilgore.vaultiq.identity.dto.UserProfileResponse;
import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.repository.UserRepository;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Get the currently authenticated User entity.
     * Used across all domains to identify the acting user.
     */
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    public UserProfileResponse getProfile() {
        User user = getCurrentUser();
        return mapToProfileResponse(user);
    }

    public UserProfileResponse getProfileById(UUID userId) {
        User user = getUserById(userId);
        return mapToProfileResponse(user);
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .walletBalance(user.getWalletBalance())
                .bettingRestricted(user.isBettingRestricted())
                .accountAgeDays(user.getAccountAgeDays())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
