package com.kilgore.vaultiq.betting.service;

import com.kilgore.vaultiq.betting.dto.*;
import com.kilgore.vaultiq.betting.entity.*;
import com.kilgore.vaultiq.betting.repository.SyndicateBetRepository;
import com.kilgore.vaultiq.betting.repository.SyndicateMemberRepository;
import com.kilgore.vaultiq.betting.repository.SyndicateRepository;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyndicateService {

    private final SyndicateRepository syndicateRepository;
    private final SyndicateMemberRepository memberRepository;
    private final SyndicateBetRepository syndicateBetRepository;
    private final FixtureRepository fixtureRepository;
    private final OddsService oddsService;
    private final WalletService walletService;
    private final UserService userService;

    @Transactional
    public SyndicateResponse createSyndicate(CreateSyndicateRequest request) {
        User creator = userService.getCurrentUser();

        Syndicate syndicate = Syndicate.builder()
                .name(request.getName())
                .createdBy(creator)
                .targetStake(request.getTargetStake())
                .currentPool(BigDecimal.ZERO)
                .status(SyndicateStatus.OPEN)
                .build();

        syndicate = syndicateRepository.save(syndicate);

        log.info("Syndicate '{}' created by {} — target stake {}",
                syndicate.getName(), creator.getUsername(), request.getTargetStake());

        return mapToResponse(syndicate);
    }

    @Transactional
    public SyndicateResponse joinSyndicate(UUID syndicateId, JoinSyndicateRequest request) {
        User user = userService.getCurrentUser();
        Syndicate syndicate = findSyndicateOrThrow(syndicateId);

        if (syndicate.getStatus() != SyndicateStatus.OPEN) {
            throw new BadRequestException("Syndicate is no longer open for contributions");
        }

        if (memberRepository.existsBySyndicateIdAndUserId(syndicateId, user.getId())) {
            throw new BadRequestException("You have already joined this syndicate");
        }

        // Check if contribution would exceed target
        BigDecimal newPool = syndicate.getCurrentPool().add(request.getContribution());
        if (newPool.compareTo(syndicate.getTargetStake()) > 0) {
            BigDecimal maxContribution = syndicate.getTargetStake().subtract(syndicate.getCurrentPool());
            throw new BadRequestException("Contribution exceeds remaining pool space. Max contribution: " + maxContribution);
        }

        // Debit wallet
        walletService.debit(
                user.getId(),
                request.getContribution(),
                TxnType.SYNDICATE_CONTRIBUTION,
                syndicateId,
                "SYNDICATE",
                "Syndicate contribution: " + syndicate.getName()
        );

        // Create membership
        SyndicateMember member = SyndicateMember.builder()
                .syndicate(syndicate)
                .user(user)
                .contribution(request.getContribution())
                .build();
        memberRepository.save(member);

        // Update pool
        syndicate.setCurrentPool(newPool);
        syndicateRepository.save(syndicate);

        log.info("User {} joined syndicate '{}' with contribution {}",
                user.getUsername(), syndicate.getName(), request.getContribution());

        return mapToResponse(syndicate);
    }

    @Transactional
    public SyndicateResponse placeSyndicateBet(UUID syndicateId, PlaceSyndicateBetRequest request) {
        User user = userService.getCurrentUser();
        Syndicate syndicate = findSyndicateOrThrow(syndicateId);

        // Only creator can place the bet
        if (!syndicate.getCreatedBy().getId().equals(user.getId())) {
            throw new BadRequestException("Only the syndicate creator can place the bet");
        }

        if (syndicate.getStatus() != SyndicateStatus.OPEN) {
            throw new BadRequestException("Syndicate bet has already been placed or is cancelled");
        }

        // Validate fixture
        Fixture fixture = fixtureRepository.findById(request.getFixtureId())
                .orElseThrow(() -> new ResourceNotFoundException("Fixture", "id", request.getFixtureId()));

        if (fixture.getStatus() != FixtureStatus.OPEN) {
            throw new BadRequestException("Fixture is not open for betting");
        }

        // Parse outcome and get odds
        BetOutcome outcome = parseOutcome(request.getOutcome());
        FixtureOdds fixtureOdds = oddsService.findOddsOrThrow(fixture.getId());
        BigDecimal odds = getOddsForOutcome(fixtureOdds, outcome);

        BigDecimal totalStake = syndicate.getCurrentPool();
        BigDecimal potentialPayout = totalStake.multiply(odds).setScale(2, RoundingMode.HALF_UP);

        // Create syndicate bet
        SyndicateBet syndicateBet = SyndicateBet.builder()
                .syndicate(syndicate)
                .fixture(fixture)
                .outcome(outcome)
                .oddsAtPlacement(odds)
                .totalStake(totalStake)
                .potentialPayout(potentialPayout)
                .status(BetStatus.PENDING)
                .build();
        syndicateBetRepository.save(syndicateBet);

        // Mark syndicate as PLACED
        syndicate.setStatus(SyndicateStatus.PLACED);
        syndicateRepository.save(syndicate);

        log.info("Syndicate '{}' bet placed: {} on {} vs {} — stake {}, odds {}",
                syndicate.getName(), outcome, fixture.getHomeTeam().getName(),
                fixture.getAwayTeam().getName(), totalStake, odds);

        return mapToResponse(syndicate);
    }

    @Transactional
    public SyndicateResponse cancelSyndicate(UUID syndicateId) {
        User user = userService.getCurrentUser();
        Syndicate syndicate = findSyndicateOrThrow(syndicateId);

        if (!syndicate.getCreatedBy().getId().equals(user.getId())) {
            throw new BadRequestException("Only the syndicate creator can cancel it");
        }

        if (syndicate.getStatus() != SyndicateStatus.OPEN) {
            throw new BadRequestException("Only OPEN syndicates can be cancelled");
        }

        // Refund all members
        List<SyndicateMember> members = memberRepository.findBySyndicateId(syndicateId);
        for (SyndicateMember member : members) {
            walletService.credit(
                    member.getUser().getId(),
                    member.getContribution(),
                    TxnType.SYNDICATE_PAYOUT,
                    syndicateId,
                    "SYNDICATE",
                    "Syndicate cancelled — refund: " + syndicate.getName()
            );
        }

        syndicate.setStatus(SyndicateStatus.CANCELLED);
        syndicateRepository.save(syndicate);

        log.info("Syndicate '{}' cancelled — {} members refunded", syndicate.getName(), members.size());

        return mapToResponse(syndicate);
    }

    @Transactional(readOnly = true)
    public List<SyndicateResponse> browseOpenSyndicates(int page, int size) {
        Page<Syndicate> syndicates = syndicateRepository.findByStatusOrderByCreatedAtDesc(
                SyndicateStatus.OPEN, PageRequest.of(page, size));
        return syndicates.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SyndicateResponse getSyndicateById(UUID id) {
        Syndicate syndicate = findSyndicateOrThrow(id);
        return mapToResponse(syndicate);
    }

    // ── Helpers ──

    private Syndicate findSyndicateOrThrow(UUID id) {
        return syndicateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Syndicate", "id", id));
    }

    private BetOutcome parseOutcome(String outcome) {
        try {
            return BetOutcome.valueOf(outcome.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid outcome: " + outcome);
        }
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

    private SyndicateResponse mapToResponse(Syndicate syndicate) {
        List<SyndicateMember> members = memberRepository.findBySyndicateId(syndicate.getId());

        List<SyndicateMemberResponse> memberResponses = members.stream()
                .map(m -> SyndicateMemberResponse.builder()
                        .id(m.getId())
                        .userId(m.getUser().getId())
                        .username(m.getUser().getUsername())
                        .contribution(m.getContribution())
                        .payout(m.getPayout())
                        .joinedAt(m.getJoinedAt())
                        .build())
                .collect(Collectors.toList());

        SyndicateBetResponse betResponse = syndicateBetRepository.findBySyndicateId(syndicate.getId())
                .map(bet -> {
                    Fixture f = bet.getFixture();
                    return SyndicateBetResponse.builder()
                            .id(bet.getId())
                            .fixtureId(f.getId())
                            .homeTeamName(f.getHomeTeam().getName())
                            .awayTeamName(f.getAwayTeam().getName())
                            .outcome(bet.getOutcome().name())
                            .oddsAtPlacement(bet.getOddsAtPlacement())
                            .totalStake(bet.getTotalStake())
                            .potentialPayout(bet.getPotentialPayout())
                            .status(bet.getStatus().name())
                            .placedAt(bet.getPlacedAt())
                            .settledAt(bet.getSettledAt())
                            .build();
                }).orElse(null);

        return SyndicateResponse.builder()
                .id(syndicate.getId())
                .name(syndicate.getName())
                .creatorUsername(syndicate.getCreatedBy().getUsername())
                .targetStake(syndicate.getTargetStake())
                .currentPool(syndicate.getCurrentPool())
                .status(syndicate.getStatus().name())
                .members(memberResponses)
                .bet(betResponse)
                .createdAt(syndicate.getCreatedAt())
                .build();
    }
}
