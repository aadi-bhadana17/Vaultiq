package com.kilgore.vaultiq.league.service;

import com.kilgore.vaultiq.league.dto.SeasonRequest;
import com.kilgore.vaultiq.league.dto.SeasonResponse;
import com.kilgore.vaultiq.league.entity.League;
import com.kilgore.vaultiq.league.entity.Season;
import com.kilgore.vaultiq.league.repository.SeasonRepository;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final LeagueService leagueService;

    @Transactional
    public SeasonResponse createSeason(SeasonRequest request) {
        League league = leagueService.findLeagueOrThrow(request.getLeagueId());

        // Enforce one active season per league:
        // If there is already an active season, deactivate it before creating the new one.
        Optional<Season> existingActive = seasonRepository.findActiveSeasonByLeagueId(league.getId());
        existingActive.ifPresent(s -> {
            s.setActive(false);
            seasonRepository.save(s);
        });

        if (request.getEndDate() != null && request.getEndDate().isBefore(request.getStartDate())) {
            throw new BadRequestException("End date cannot be before start date");
        }

        Season season = Season.builder()
                .league(league)
                .name(request.getName())
                .active(true)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        season = seasonRepository.save(season);
        return mapToResponse(season);
    }

    @Transactional(readOnly = true)
    public List<SeasonResponse> getSeasonsByLeague(UUID leagueId) {
        leagueService.findLeagueOrThrow(leagueId); // validate league exists
        return seasonRepository.findByLeagueId(leagueId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SeasonResponse getSeasonById(UUID id) {
        Season season = findSeasonOrThrow(id);
        return mapToResponse(season);
    }

    @Transactional(readOnly = true)
    public SeasonResponse getActiveSeasonByLeague(UUID leagueId) {
        leagueService.findLeagueOrThrow(leagueId);
        Season season = seasonRepository.findActiveSeasonByLeagueId(leagueId)
                .orElseThrow(() -> new ResourceNotFoundException("No active season found for league " + leagueId));
        return mapToResponse(season);
    }

    @Transactional
    public SeasonResponse deactivateSeason(UUID id) {
        Season season = findSeasonOrThrow(id);
        season.setActive(false);
        season = seasonRepository.save(season);
        return mapToResponse(season);
    }

    public Season findSeasonOrThrow(UUID id) {
        return seasonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Season", "id", id));
    }

    private SeasonResponse mapToResponse(Season season) {
        return SeasonResponse.builder()
                .id(season.getId())
                .leagueId(season.getLeague().getId())
                .leagueName(season.getLeague().getName())
                .name(season.getName())
                .active(season.isActive())
                .startDate(season.getStartDate())
                .endDate(season.getEndDate())
                .teamCount(season.getTeams().size())
                .fixtureCount(season.getFixtures().size())
                .createdAt(season.getCreatedAt())
                .build();
    }
}
