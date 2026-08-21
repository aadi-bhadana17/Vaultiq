package com.kilgore.vaultiq.league.service;

import com.kilgore.vaultiq.league.dto.FixtureRequest;
import com.kilgore.vaultiq.league.dto.FixtureResponse;
import com.kilgore.vaultiq.league.dto.LiveFeedResponse;
import com.kilgore.vaultiq.league.dto.MatchResultResponse;
import com.kilgore.vaultiq.league.entity.*;
import com.kilgore.vaultiq.league.repository.FixtureRepository;
import com.kilgore.vaultiq.odds.service.OddsService;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FixtureService {

    private final FixtureRepository fixtureRepository;
    private final SeasonService seasonService;
    private final TeamService teamService;
    private final OddsService oddsService;

    @Transactional
    public FixtureResponse createFixture(FixtureRequest request) {
        Season season = seasonService.findSeasonOrThrow(request.getSeasonId());

        if (!season.isActive()) {
            throw new BadRequestException("Cannot create fixtures in an inactive season");
        }

        Team homeTeam = teamService.findTeamOrThrow(request.getHomeTeamId());
        Team awayTeam = teamService.findTeamOrThrow(request.getAwayTeamId());

        // Validate teams belong to the same season
        if (!homeTeam.getSeason().getId().equals(season.getId())
                || !awayTeam.getSeason().getId().equals(season.getId())) {
            throw new BadRequestException("Both teams must belong to the specified season");
        }

        // Validate home != away
        if (homeTeam.getId().equals(awayTeam.getId())) {
            throw new BadRequestException("Home team and away team must be different");
        }

        Fixture fixture = Fixture.builder()
                .season(season)
                .homeTeam(homeTeam)
                .awayTeam(awayTeam)
                .scheduledAt(request.getScheduledAt())
                .status(FixtureStatus.SCHEDULED)
                .matchMinute(0)
                .build();

        fixture = fixtureRepository.save(fixture);

        // Layer 1: Auto-generate base odds from team strengths
        oddsService.generateBaseOdds(fixture);

        return mapToResponse(fixture);
    }

    /**
     * Live Feed — returns all non-finished fixtures from active seasons,
     * eagerly bundled with their computed odds in a single response.
     */
    @Transactional(readOnly = true)
    public List<LiveFeedResponse> getLiveFeed() {
        return fixtureRepository.findActiveFeedFixtures().stream()
                .map(fixture -> {
                    LiveFeedResponse.LiveFeedResponseBuilder builder = LiveFeedResponse.builder()
                            .fixture(mapToResponse(fixture));
                    try {
                        builder.odds(oddsService.getOddsForFixture(fixture.getId()));
                    } catch (Exception e) {
                        // Fixture might not have odds generated yet — skip gracefully
                        builder.odds(null);
                    }
                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Admin Live Feed — same as getLiveFeed but returns FixtureOddsAdminResponse
     * with stake pool data for financial exposure monitoring.
     */
    @Transactional(readOnly = true)
    public List<LiveFeedResponse> getAdminLiveFeed() {
        return fixtureRepository.findActiveFeedFixtures().stream()
                .map(fixture -> {
                    LiveFeedResponse.LiveFeedResponseBuilder builder = LiveFeedResponse.builder()
                            .fixture(mapToResponse(fixture));
                    try {
                        builder.odds(oddsService.getOddsForFixtureAdmin(fixture.getId()));
                    } catch (Exception e) {
                        builder.odds(null);
                    }
                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LiveFeedResponse> getLiveFeedBySeason(UUID seasonId) {
        seasonService.findSeasonOrThrow(seasonId);
        return fixtureRepository.findActiveFeedFixturesBySeasonId(seasonId).stream()
                .map(fixture -> {
                    LiveFeedResponse.LiveFeedResponseBuilder builder = LiveFeedResponse.builder()
                            .fixture(mapToResponse(fixture));
                    try {
                        builder.odds(oddsService.getOddsForFixture(fixture.getId()));
                    } catch (Exception e) {
                        builder.odds(null);
                    }
                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FixtureResponse> getFixturesBySeason(UUID seasonId) {
        seasonService.findSeasonOrThrow(seasonId);
        return fixtureRepository.findBySeasonId(seasonId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FixtureResponse> getFixturesBySeasonAndStatus(UUID seasonId, FixtureStatus status) {
        return fixtureRepository.findBySeasonIdAndStatus(seasonId, status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FixtureResponse getFixtureById(UUID id) {
        Fixture fixture = findFixtureOrThrow(id);
        return mapToResponse(fixture);
    }

    /**
     * Transition fixture status. Valid transitions:
     * SCHEDULED → OPEN, OPEN → LOCKED, LOCKED → OPEN, OPEN → FINISHED, LOCKED → FINISHED
     */
    @Transactional
    public FixtureResponse updateFixtureStatus(UUID id, FixtureStatus newStatus) {
        Fixture fixture = findFixtureOrThrow(id);
        FixtureStatus currentStatus = fixture.getStatus();

        validateStatusTransition(currentStatus, newStatus);

        fixture.setStatus(newStatus);
        fixture = fixtureRepository.save(fixture);
        return mapToResponse(fixture);
    }

    public Fixture findFixtureOrThrow(UUID id) {
        return fixtureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fixture", "id", id));
    }

    private void validateStatusTransition(FixtureStatus from, FixtureStatus to) {
        boolean valid = switch (from) {
            case SCHEDULED -> to == FixtureStatus.OPEN;
            case OPEN -> to == FixtureStatus.LOCKED || to == FixtureStatus.FINISHED || to == FixtureStatus.HALF_TIME || to == FixtureStatus.AWAITING_EXTRA_TIME;
            case HALF_TIME -> to == FixtureStatus.OPEN;
            case AWAITING_EXTRA_TIME -> to == FixtureStatus.ADDITIONAL_TIME || to == FixtureStatus.FINISHED;
            case ADDITIONAL_TIME -> to == FixtureStatus.FINISHED;
            case LOCKED -> to == FixtureStatus.OPEN || to == FixtureStatus.FINISHED;
            case FINISHED -> false;
        };

        if (!valid) {
            throw new BadRequestException("Invalid status transition: " + from + " → " + to);
        }
    }

    FixtureResponse mapToResponse(Fixture fixture) {
        FixtureResponse.FixtureResponseBuilder builder = FixtureResponse.builder()
                .id(fixture.getId())
                .seasonId(fixture.getSeason().getId())
                .homeTeamName(fixture.getHomeTeam().getName())
                .homeTeamId(fixture.getHomeTeam().getId())
                .awayTeamName(fixture.getAwayTeam().getName())
                .awayTeamId(fixture.getAwayTeam().getId())
                .matchMinute(fixture.getMatchMinute())
                .status(fixture.getStatus().name())
                .scheduledAt(fixture.getScheduledAt())
                .platformProfit(fixture.getPlatformProfit())
                .createdAt(fixture.getCreatedAt())
                .updatedAt(fixture.getUpdatedAt());

        if (fixture.getMatchResult() != null) {
            MatchResult mr = fixture.getMatchResult();
            builder.matchResult(MatchResultResponse.builder()
                    .id(mr.getId())
                    .fixtureId(fixture.getId())
                    .homeScore(mr.getHomeScore())
                    .awayScore(mr.getAwayScore())
                    .isFinal(mr.isFinal())
                    .updatedAt(mr.getUpdatedAt())
                    .build());
        }

        return builder.build();
    }
}
