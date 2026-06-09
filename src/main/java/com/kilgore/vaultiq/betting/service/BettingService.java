package com.kilgore.vaultiq.betting.service;

import com.kilgore.vaultiq.automation.service.DynamicBetLimitService;
import com.kilgore.vaultiq.betting.dto.BetResponse;
import com.kilgore.vaultiq.betting.dto.PlaceBetRequest;
import com.kilgore.vaultiq.betting.entity.*;
import com.kilgore.vaultiq.betting.repository.BetRepository;
import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.service.UserService;
import com.kilgore.vaultiq.league.entity.Fixture;
import com.kilgore.vaultiq.league.entity.FixtureStatus;
import com.kilgore.vaultiq.league.repository.FixtureRepository;
import com.kilgore.vaultiq.odds.entity.FixtureOdds;
import com.kilgore.vaultiq.odds.service.OddsService;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core betting service — handles single bet placement and queries.
 *
 * Placement flow:
 *   1. Validate user (not restricted)
 *   2. Check dynamic bet limits
 *   3. Validate fixture (must be OPEN)
 *   4. Snapshot odds at placement time
 *   5. Calculate potential payout (stake × odds)
 *   6. Debit wallet via WalletService
 *   7. Save Bet entity
 *   8. Trigger demand adjustment for match-winner bets
 *   9. Run suspicion detection
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BettingService {

    private final BetRepository betRepository;
    private final FixtureRepository fixtureRepository;
    private final UserService userService;
    private final WalletService walletService;
    private final OddsService oddsService;
    private final DynamicBetLimitService dynamicBetLimitService;
    private final SuspicionDetectionService suspicionDetectionService;

    // ── Bet Placement (user-facing) ──

    @Transactional
    public BetResponse placeBet(PlaceBetRequest request) {
        // 1. Get current user
        User user = userService.getCurrentUser();

        // 2. Validate user is not restricted
        if (user.isBettingRestricted()) {
            throw new BadRequestException("Your account is currently restricted from placing bets");
        }

        // 3. Check dynamic bet limits
        dynamicBetLimitService.checkBetWithinLimits(user.getId(), request.getStake());

        // 4. Validate fixture exists and is SCHEDULED or OPEN (pre-match + in-play betting)
        Fixture fixture = fixtureRepository.findById(request.getFixtureId())
                .orElseThrow(() -> new ResourceNotFoundException("Fixture", "id", request.getFixtureId()));

        if (fixture.getStatus() != FixtureStatus.OPEN && fixture.getStatus() != FixtureStatus.SCHEDULED) {
            String message = switch (fixture.getStatus()) {
                case LOCKED -> "Fixture is currently locked for odds recalculation — try again shortly";
                case FINISHED -> "Fixture has already finished — betting is closed";
                default -> "Fixture is not available for betting";
            };
            throw new BadRequestException(message);
        }

        // 5. Validate stake
        if (request.getStake() == null || request.getStake().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Stake must be greater than zero");
        }

        // 6. Parse and validate outcome
        BetOutcome outcome = parseOutcome(request.getOutcome());

        // 7. Snapshot current odds for the chosen outcome
        FixtureOdds fixtureOdds = oddsService.findOddsOrThrow(fixture.getId());
        BigDecimal oddsAtPlacement = getOddsForOutcome(fixtureOdds, outcome);

        // 8. Calculate potential payout
        BigDecimal potentialPayout = request.getStake()
                .multiply(oddsAtPlacement)
                .setScale(2, RoundingMode.HALF_UP);

        // 9. Build Bet entity (save first to get the ID for wallet txn reference)
        Bet bet = Bet.builder()
                .user(user)
                .fixture(fixture)
                .betType(BetType.SINGLE)
                .outcome(outcome)
                .oddsAtPlacement(oddsAtPlacement)
                .stake(request.getStake())
                .potentialPayout(potentialPayout)
                .status(BetStatus.PENDING)
                .build();

        bet = betRepository.save(bet);

        // 10. Debit wallet
        walletService.debit(
                user.getId(),
                request.getStake(),
                TxnType.BET_PLACED,
                bet.getId(),
                "BET",
                String.format("Bet placed: %s on %s vs %s",
                        outcome.name(),
                        fixture.getHomeTeam().getName(),
                        fixture.getAwayTeam().getName())
        );

        // 11. Trigger demand adjustment for match-winner bets only
        if (isMatchWinnerOutcome(outcome)) {
            oddsService.applyDemandAdjustment(fixture.getId(), outcome.name(), request.getStake());
        }

        // 12. Run suspicion detection
        suspicionDetectionService.checkForSuspicion(bet);

        return mapToResponse(bet);
    }

    // ── Internal Placement (for auto-bet + syndicate — no security context) ──

    /**
     * Place a bet internally without requiring the security context.
     * Used by AutoBetScheduler and SyndicateService.
     */
    @Transactional
    public BetResponse placeBetInternal(UUID userId, UUID fixtureId, String outcomeName, BigDecimal stake) {
        User user = userService.getUserById(userId);

        if (user.isBettingRestricted()) {
            throw new BadRequestException("User account is restricted from placing bets");
        }

        Fixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new ResourceNotFoundException("Fixture", "id", fixtureId));

        if (fixture.getStatus() != FixtureStatus.OPEN && fixture.getStatus() != FixtureStatus.SCHEDULED) {
            throw new BadRequestException("Fixture is not available for betting");
        }

        BetOutcome outcome = parseOutcome(outcomeName);
        FixtureOdds fixtureOdds = oddsService.findOddsOrThrow(fixtureId);
        BigDecimal oddsAtPlacement = getOddsForOutcome(fixtureOdds, outcome);

        BigDecimal potentialPayout = stake.multiply(oddsAtPlacement)
                .setScale(2, RoundingMode.HALF_UP);

        Bet bet = Bet.builder()
                .user(user)
                .fixture(fixture)
                .betType(BetType.SINGLE)
                .outcome(outcome)
                .oddsAtPlacement(oddsAtPlacement)
                .stake(stake)
                .potentialPayout(potentialPayout)
                .status(BetStatus.PENDING)
                .build();

        bet = betRepository.save(bet);

        walletService.debit(
                userId,
                stake,
                TxnType.BET_PLACED,
                bet.getId(),
                "BET",
                String.format("Auto-bet: %s on %s vs %s",
                        outcome.name(),
                        fixture.getHomeTeam().getName(),
                        fixture.getAwayTeam().getName())
        );

        if (isMatchWinnerOutcome(outcome)) {
            oddsService.applyDemandAdjustment(fixtureId, outcome.name(), stake);
        }

        log.info("Internal bet placed: user {}, fixture {}, outcome {}, stake {}",
                user.getUsername(), fixtureId, outcome, stake);

        return mapToResponse(bet);
    }

    // ── Queries ──

    @Transactional(readOnly = true)
    public List<BetResponse> getUserBets(int page, int size) {
        User user = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<Bet> bets = betRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        return bets.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BetResponse getBetById(UUID betId) {
        User user = userService.getCurrentUser();
        Bet bet = betRepository.findById(betId)
                .orElseThrow(() -> new ResourceNotFoundException("Bet", "id", betId));

        // Ownership check
        if (!bet.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not have access to this bet");
        }

        return mapToResponse(bet);
    }

    // ── Helpers ──

    private BetOutcome parseOutcome(String outcome) {
        try {
            return BetOutcome.valueOf(outcome.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid outcome: " + outcome
                    + ". Valid values: HOME_WIN, DRAW, AWAY_WIN, OVER_1_5, UNDER_1_5, "
                    + "OVER_2_5, UNDER_2_5, OVER_3_5, UNDER_3_5, BTTS_YES, BTTS_NO");
        }
    }

    private boolean isMatchWinnerOutcome(BetOutcome outcome) {
        return outcome == BetOutcome.HOME_WIN
                || outcome == BetOutcome.DRAW
                || outcome == BetOutcome.AWAY_WIN;
    }

    /**
     * Extract the specific odds value from FixtureOdds based on the BetOutcome.
     */
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

    private BetResponse mapToResponse(Bet bet) {
        Fixture fixture = bet.getFixture();
        return BetResponse.builder()
                .id(bet.getId())
                .fixtureId(fixture.getId())
                .homeTeamName(fixture.getHomeTeam().getName())
                .awayTeamName(fixture.getAwayTeam().getName())
                .betType(bet.getBetType().name())
                .outcome(bet.getOutcome().name())
                .oddsAtPlacement(bet.getOddsAtPlacement())
                .stake(bet.getStake())
                .potentialPayout(bet.getPotentialPayout())
                .status(bet.getStatus().name())
                .cashedOut(bet.isCashedOut())
                .cashoutAmount(bet.getCashoutAmount())
                .createdAt(bet.getCreatedAt())
                .settledAt(bet.getSettledAt())
                .build();
    }
}
