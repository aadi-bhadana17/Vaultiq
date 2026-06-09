package com.kilgore.vaultiq.social.service;

import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.service.UserService;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import com.kilgore.vaultiq.social.dto.TipsterProfileResponse;
import com.kilgore.vaultiq.social.entity.TipsterProfile;
import com.kilgore.vaultiq.social.repository.TipsterProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TipsterService {

    private final TipsterProfileRepository tipsterRepository;
    private final UserService userService;

    /**
     * Minimum bets to become eligible as a tipster.
     */
    private static final int MIN_BETS_FOR_ELIGIBILITY = 10;
    private static final BigDecimal MIN_WIN_RATE_FOR_ELIGIBILITY = new BigDecimal("40.00");

    @Transactional
    public TipsterProfileResponse registerAsTipster() {
        User user = userService.getCurrentUser();

        if (tipsterRepository.existsByUserId(user.getId())) {
            throw new BadRequestException("You are already registered as a tipster");
        }

        TipsterProfile profile = TipsterProfile.builder()
                .user(user)
                .cutPercentage(new BigDecimal("5.00"))
                .build();

        profile = tipsterRepository.save(profile);

        log.info("User {} registered as tipster", user.getUsername());

        return mapToResponse(profile);
    }

    @Transactional(readOnly = true)
    public TipsterProfileResponse getProfile(UUID userId) {
        TipsterProfile profile = tipsterRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("TipsterProfile", "userId", userId));
        return mapToResponse(profile);
    }

    /**
     * Update tipster stats after one of their bets settles.
     * Called from BetSettlementService.
     */
    @Transactional
    public void updateStats(UUID tipsterUserId, boolean isWin) {
        tipsterRepository.findByUserId(tipsterUserId).ifPresent(profile -> {
            profile.setTotalBets(profile.getTotalBets() + 1);
            if (isWin) {
                profile.setTotalWins(profile.getTotalWins() + 1);
            }

            // Recalculate win rate
            BigDecimal winRate = BigDecimal.valueOf(profile.getTotalWins())
                    .multiply(new BigDecimal("100"))
                    .divide(BigDecimal.valueOf(profile.getTotalBets()), 2, RoundingMode.HALF_UP);
            profile.setWinRate(winRate);

            // Credibility score = winRate * log(totalBets + 1) / 2
            double credibility = winRate.doubleValue()
                    * Math.log(profile.getTotalBets() + 1) / 2;
            profile.setCredibilityScore(BigDecimal.valueOf(credibility)
                    .setScale(2, RoundingMode.HALF_UP));

            // Cut percentage scales with credibility: 5% base + up to 5% more for high performers
            BigDecimal cut = new BigDecimal("5.00")
                    .add(profile.getCredibilityScore()
                            .divide(new BigDecimal("20"), 2, RoundingMode.HALF_UP));
            if (cut.compareTo(new BigDecimal("10.00")) > 0) cut = new BigDecimal("10.00");
            profile.setCutPercentage(cut);

            // Eligibility check
            profile.setEligible(
                    profile.getTotalBets() >= MIN_BETS_FOR_ELIGIBILITY
                            && profile.getWinRate().compareTo(MIN_WIN_RATE_FOR_ELIGIBILITY) >= 0
            );

            tipsterRepository.save(profile);

            log.debug("Tipster {} stats updated — winRate: {}%, eligible: {}",
                    profile.getUser().getUsername(), winRate, profile.isEligible());
        });
    }

    @Transactional(readOnly = true)
    public List<TipsterProfileResponse> browseTipsters(int page, int size) {
        Page<TipsterProfile> profiles = tipsterRepository.findByEligibleTrue(PageRequest.of(page, size));
        return profiles.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TipsterProfileResponse mapToResponse(TipsterProfile profile) {
        return TipsterProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUser().getId())
                .username(profile.getUser().getUsername())
                .totalBets(profile.getTotalBets())
                .totalWins(profile.getTotalWins())
                .winRate(profile.getWinRate())
                .credibilityScore(profile.getCredibilityScore())
                .cutPercentage(profile.getCutPercentage())
                .eligible(profile.isEligible())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}
