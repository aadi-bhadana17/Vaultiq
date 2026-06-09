package com.kilgore.vaultiq.social.service;

import com.kilgore.vaultiq.betting.entity.*;
import com.kilgore.vaultiq.betting.repository.BetRepository;
import com.kilgore.vaultiq.betting.service.WalletService;
import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.service.UserService;
import com.kilgore.vaultiq.league.entity.Fixture;
import com.kilgore.vaultiq.league.entity.FixtureStatus;
import com.kilgore.vaultiq.odds.entity.FixtureOdds;
import com.kilgore.vaultiq.odds.service.OddsService;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import com.kilgore.vaultiq.social.dto.CopyBetRequest;
import com.kilgore.vaultiq.social.dto.CopyBetResponse;
import com.kilgore.vaultiq.social.dto.TipsterBetPreview;
import com.kilgore.vaultiq.social.entity.CopyBet;
import com.kilgore.vaultiq.social.entity.TipsterProfile;
import com.kilgore.vaultiq.social.repository.CopyBetRepository;
import com.kilgore.vaultiq.social.repository.TipsterFollowerRepository;
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

/**
 * Copy Betting — Blind Model.
 *
 * Key design:
 *   - Follower CANNOT see the tipster's chosen outcome
 *   - They only see: tipster's win rate, bet type (MATCH_RESULT/OVER_UNDER/BTTS), fixture
 *   - Follower clicks "copy" with their stake → bet placed at tipster's outcome + current odds
 *   - Outcome only revealed after fixture finishes
 *   - This prevents followers from placing the same bet directly to avoid the tipster cut
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CopyBetService {

    private final CopyBetRepository copyBetRepository;
    private final BetRepository betRepository;
    private final TipsterProfileRepository tipsterRepository;
    private final TipsterFollowerRepository followerRepository;
    private final OddsService oddsService;
    private final WalletService walletService;
    private final UserService userService;

    /**
     * Place a copy bet — blind: follower doesn't see the outcome.
     */
    @Transactional
    public CopyBetResponse placeCopyBet(CopyBetRequest request) {
        User follower = userService.getCurrentUser();

        // 1. Load the original bet (tipster's bet)
        Bet originalBet = betRepository.findById(request.getOriginalBetId())
                .orElseThrow(() -> new ResourceNotFoundException("Bet", "id", request.getOriginalBetId()));

        if (originalBet.getStatus() != BetStatus.PENDING) {
            throw new BadRequestException("Can only copy PENDING bets");
        }

        // 2. Verify the original bet belongs to a tipster
        TipsterProfile tipster = tipsterRepository.findByUserId(originalBet.getUser().getId())
                .orElseThrow(() -> new BadRequestException("The original bet owner is not a registered tipster"));

        if (!tipster.isEligible()) {
            throw new BadRequestException("This tipster is not currently eligible");
        }

        // 3. Verify follower is actually following this tipster
        if (!followerRepository.existsByTipsterIdAndFollowerId(tipster.getId(), follower.getId())) {
            throw new BadRequestException("You must follow this tipster to copy their bets");
        }

        // 4. Prevent self-copy
        if (follower.getId().equals(originalBet.getUser().getId())) {
            throw new BadRequestException("You cannot copy your own bet");
        }

        // 5. Prevent duplicate copy
        if (copyBetRepository.existsByOriginalBetIdAndFollowerId(originalBet.getId(), follower.getId())) {
            throw new BadRequestException("You have already copied this bet");
        }

        // 6. Validate fixture is still OPEN
        Fixture fixture = originalBet.getFixture();
        if (fixture.getStatus() != FixtureStatus.OPEN) {
            throw new BadRequestException("Fixture is no longer open for betting");
        }

        // 7. Get CURRENT odds (not the odds the tipster got — follower gets live odds)
        FixtureOdds fixtureOdds = oddsService.findOddsOrThrow(fixture.getId());
        BigDecimal currentOdds = getOddsForOutcome(fixtureOdds, originalBet.getOutcome());

        // 8. Calculate payout with follower's stake + current odds
        BigDecimal potentialPayout = request.getStake()
                .multiply(currentOdds)
                .setScale(2, RoundingMode.HALF_UP);

        // 9. Create the copy bet (same fixture, same outcome, follower's stake, current odds)
        Bet copyBetEntity = Bet.builder()
                .user(follower)
                .fixture(fixture)
                .betType(BetType.COPY)
                .outcome(originalBet.getOutcome())  // Blind: same outcome as tipster
                .oddsAtPlacement(currentOdds)
                .stake(request.getStake())
                .potentialPayout(potentialPayout)
                .status(BetStatus.PENDING)
                .sourceBetId(originalBet.getId())
                .build();

        copyBetEntity = betRepository.save(copyBetEntity);

        // 10. Debit follower's wallet
        walletService.debit(
                follower.getId(),
                request.getStake(),
                TxnType.BET_PLACED,
                copyBetEntity.getId(),
                "COPY_BET",
                String.format("Copy bet: %s vs %s (copied from tipster %s)",
                        fixture.getHomeTeam().getName(),
                        fixture.getAwayTeam().getName(),
                        tipster.getUser().getUsername())
        );

        // 11. Create CopyBet link
        CopyBet copyBet = CopyBet.builder()
                .originalBet(originalBet)
                .copyBet(copyBetEntity)
                .tipster(tipster)
                .follower(follower)
                .build();

        copyBet = copyBetRepository.save(copyBet);

        log.info("Copy bet placed: follower {} copied tipster {} bet {} — stake {}, odds {}",
                follower.getUsername(), tipster.getUser().getUsername(),
                originalBet.getId(), request.getStake(), currentOdds);

        return mapToResponse(copyBet, fixture);
    }

    /**
     * Get available bets from tipsters that the user follows.
     * Shows bet TYPE but NOT the outcome (blind model).
     */
    @Transactional(readOnly = true)
    public List<TipsterBetPreview> getTipsterBetPreviews(UUID tipsterUserId) {
        User follower = userService.getCurrentUser();

        TipsterProfile tipster = tipsterRepository.findByUserId(tipsterUserId)
                .orElseThrow(() -> new ResourceNotFoundException("TipsterProfile", "userId", tipsterUserId));

        if (!followerRepository.existsByTipsterIdAndFollowerId(tipster.getId(), follower.getId())) {
            throw new BadRequestException("You must follow this tipster to view their bets");
        }

        // Get tipster's PENDING bets
        List<Bet> tipsterBets = betRepository.findByUserIdOrderByCreatedAtDesc(
                        tipsterUserId, PageRequest.of(0, 20))
                .getContent();

        return tipsterBets.stream()
                .map(bet -> {
                    Fixture f = bet.getFixture();
                    String betType = deriveBetType(bet.getOutcome());

                    return TipsterBetPreview.builder()
                            .betId(bet.getId())
                            .fixtureId(f.getId())
                            .homeTeamName(f.getHomeTeam().getName())
                            .awayTeamName(f.getAwayTeam().getName())
                            .betType(betType)      // MATCH_RESULT, OVER_UNDER, or BTTS — NOT the outcome
                            .fixtureStatus(f.getStatus().name())
                            .betStatus(bet.getStatus().name())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CopyBetResponse> getCopyBetHistory(int page, int size) {
        User follower = userService.getCurrentUser();
        Page<CopyBet> copyBets = copyBetRepository.findByFollowerIdOrderByCreatedAtDesc(
                follower.getId(), PageRequest.of(page, size));

        return copyBets.getContent().stream()
                .map(cb -> mapToResponse(cb, cb.getCopyBet().getFixture()))
                .collect(Collectors.toList());
    }

    // ── Helpers ──

    private String deriveBetType(BetOutcome outcome) {
        return switch (outcome) {
            case HOME_WIN, DRAW, AWAY_WIN -> "MATCH_RESULT";
            case OVER_1_5, UNDER_1_5, OVER_2_5, UNDER_2_5, OVER_3_5, UNDER_3_5 -> "OVER_UNDER";
            case BTTS_YES, BTTS_NO -> "BTTS";
        };
    }

    private BigDecimal getOddsForOutcome(FixtureOdds odds, BetOutcome outcome) {
        return switch (outcome) {
            case HOME_WIN -> odds.getHomeWinOdds();
            case DRAW -> odds.getDrawOdds();
            case AWAY_WIN -> odds.getAwayWinOdds();
            case OVER_1_5 -> odds.getOver15Odds();
            case UNDER_1_5 -> odds.getUnder15Odds();
            case OVER_2_5 -> odds.getOver25Odds();
            case UNDER_2_5 -> odds.getUnder25Odds();
            case OVER_3_5 -> odds.getOver35Odds();
            case UNDER_3_5 -> odds.getUnder35Odds();
            case BTTS_YES -> odds.getBttsYesOdds();
            case BTTS_NO -> odds.getBttsNoOdds();
        };
    }

    private CopyBetResponse mapToResponse(CopyBet copyBet, Fixture fixture) {
        Bet cb = copyBet.getCopyBet();
        boolean fixtureFinished = fixture.getStatus() == FixtureStatus.FINISHED;

        return CopyBetResponse.builder()
                .id(copyBet.getId())
                .copyBetId(cb.getId())
                .originalBetId(copyBet.getOriginalBet().getId())
                .tipsterUsername(copyBet.getTipster().getUser().getUsername())
                .fixtureId(fixture.getId())
                .homeTeamName(fixture.getHomeTeam().getName())
                .awayTeamName(fixture.getAwayTeam().getName())
                .betType(deriveBetType(cb.getOutcome()))
                .outcome(fixtureFinished ? cb.getOutcome().name() : "HIDDEN")
                .oddsAtPlacement(cb.getOddsAtPlacement())
                .stake(cb.getStake())
                .potentialPayout(cb.getPotentialPayout())
                .status(cb.getStatus().name())
                .tipsterCut(copyBet.getTipsterCut())
                .createdAt(copyBet.getCreatedAt())
                .build();
    }
}
