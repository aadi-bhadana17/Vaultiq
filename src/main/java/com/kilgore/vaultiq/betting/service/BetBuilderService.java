package com.kilgore.vaultiq.betting.service;

import com.kilgore.vaultiq.betting.dto.*;
import com.kilgore.vaultiq.betting.entity.*;
import com.kilgore.vaultiq.betting.repository.BetBuilderLegRepository;
import com.kilgore.vaultiq.betting.repository.BetBuilderRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bet Builder — multi-fixture accumulator.
 * Users combine 2+ legs across different fixtures (one category per fixture).
 * All legs must win for payout. Fail-fast on first lost leg.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BetBuilderService {

    private final BetBuilderRepository betBuilderRepository;
    private final BetBuilderLegRepository betBuilderLegRepository;
    private final FixtureRepository fixtureRepository;
    private final OddsService oddsService;
    private final WalletService walletService;
    private final UserService userService;

    @Transactional
    public BetBuilderResponse placeBetBuilder(BetBuilderRequest request) {
        User user = userService.getCurrentUser();

        if (user.isBettingRestricted()) {
            throw new BadRequestException("Your account is currently restricted from placing bets");
        }

        List<BetBuilderLegRequest> legRequests = request.getLegs();

        // 1. Validate at least 2 legs
        if (legRequests.size() < 2) {
            throw new BadRequestException("Bet builder requires at least 2 legs");
        }

        // 2. Check for duplicate fixture+category conflicts
        Set<String> fixtureCategories = new HashSet<>();
        BigDecimal combinedOdds = BigDecimal.ONE;

        List<BetBuilderLeg> legs = new ArrayList<>();

        for (BetBuilderLegRequest legReq : legRequests) {
            // Validate fixture
            Fixture fixture = fixtureRepository.findById(legReq.getFixtureId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fixture", "id", legReq.getFixtureId()));

            if (fixture.getStatus() != FixtureStatus.OPEN) {
                throw new BadRequestException("Fixture " + fixture.getHomeTeam().getName()
                        + " vs " + fixture.getAwayTeam().getName() + " is not open for betting");
            }

            // Parse outcome
            BetOutcome outcome = parseOutcome(legReq.getOutcome());
            OutcomeCategory category = deriveCategory(outcome);

            // Check no duplicate fixture+category
            String key = legReq.getFixtureId() + ":" + category;
            if (!fixtureCategories.add(key)) {
                throw new BadRequestException("Duplicate category " + category
                        + " for fixture " + fixture.getHomeTeam().getName()
                        + " vs " + fixture.getAwayTeam().getName());
            }

            // Snapshot odds
            FixtureOdds fixtureOdds = oddsService.findOddsOrThrow(fixture.getId());
            BigDecimal odds = getOddsForOutcome(fixtureOdds, outcome);

            combinedOdds = combinedOdds.multiply(odds);

            BetBuilderLeg leg = BetBuilderLeg.builder()
                    .fixture(fixture)
                    .outcome(outcome)
                    .outcomeCategory(category)
                    .oddsAtPlacement(odds)
                    .result(LegResult.PENDING)
                    .build();

            legs.add(leg);
        }

        combinedOdds = combinedOdds.setScale(3, RoundingMode.HALF_UP);
        BigDecimal potentialPayout = request.getStake().multiply(combinedOdds)
                .setScale(2, RoundingMode.HALF_UP);

        // 3. Build and save BetBuilder
        BetBuilder builder = BetBuilder.builder()
                .user(user)
                .combinedOdds(combinedOdds)
                .stake(request.getStake())
                .potentialPayout(potentialPayout)
                .status(BetStatus.PENDING)
                .totalLegs(legs.size())
                .settledLegs(0)
                .build();

        builder = betBuilderRepository.save(builder);

        // 4. Link legs to builder and save
        for (BetBuilderLeg leg : legs) {
            leg.setBetBuilder(builder);
        }
        betBuilderLegRepository.saveAll(legs);
        builder.setLegs(legs);

        // 5. Debit wallet
        walletService.debit(
                user.getId(),
                request.getStake(),
                TxnType.BET_PLACED,
                builder.getId(),
                "BET_BUILDER",
                String.format("Bet builder placed: %d legs, combined odds %s",
                        legs.size(), combinedOdds)
        );

        log.info("Bet builder {} placed by user {} — {} legs, combined odds {}, stake {}",
                builder.getId(), user.getUsername(), legs.size(), combinedOdds, request.getStake());

        return mapToResponse(builder);
    }

    @Transactional(readOnly = true)
    public List<BetBuilderResponse> getUserBetBuilders(int page, int size) {
        User user = userService.getCurrentUser();
        Page<BetBuilder> builders = betBuilderRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(page, size));

        return builders.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BetBuilderResponse getBetBuilderById(UUID id) {
        User user = userService.getCurrentUser();
        BetBuilder builder = betBuilderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BetBuilder", "id", id));

        if (!builder.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not have access to this bet builder");
        }

        return mapToResponse(builder);
    }

    // ── Helpers ──

    private BetOutcome parseOutcome(String outcome) {
        try {
            return BetOutcome.valueOf(outcome.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid outcome: " + outcome);
        }
    }

    private OutcomeCategory deriveCategory(BetOutcome outcome) {
        return switch (outcome) {
            case HOME_WIN, DRAW, AWAY_WIN -> OutcomeCategory.MATCH_RESULT;
            case OVER_1_5, UNDER_1_5, OVER_2_5, UNDER_2_5, OVER_3_5, UNDER_3_5 -> OutcomeCategory.OVER_UNDER;
            case BTTS_YES, BTTS_NO -> OutcomeCategory.BTTS;
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

    private BetBuilderResponse mapToResponse(BetBuilder builder) {
        List<BetBuilderLegResponse> legResponses = builder.getLegs().stream()
                .map(leg -> {
                    Fixture f = leg.getFixture();
                    return BetBuilderLegResponse.builder()
                            .id(leg.getId())
                            .fixtureId(f.getId())
                            .homeTeamName(f.getHomeTeam().getName())
                            .awayTeamName(f.getAwayTeam().getName())
                            .outcome(leg.getOutcome().name())
                            .outcomeCategory(leg.getOutcomeCategory().name())
                            .oddsAtPlacement(leg.getOddsAtPlacement())
                            .result(leg.getResult().name())
                            .build();
                })
                .collect(Collectors.toList());

        return BetBuilderResponse.builder()
                .id(builder.getId())
                .combinedOdds(builder.getCombinedOdds())
                .stake(builder.getStake())
                .potentialPayout(builder.getPotentialPayout())
                .status(builder.getStatus().name())
                .totalLegs(builder.getTotalLegs())
                .settledLegs(builder.getSettledLegs())
                .legs(legResponses)
                .createdAt(builder.getCreatedAt())
                .settledAt(builder.getSettledAt())
                .build();
    }
}
