package com.kilgore.vaultiq.league.service;

import com.kilgore.vaultiq.league.dto.TeamRequest;
import com.kilgore.vaultiq.league.dto.TeamResponse;
import com.kilgore.vaultiq.league.entity.Season;
import com.kilgore.vaultiq.league.entity.Team;
import com.kilgore.vaultiq.league.repository.TeamRepository;
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
public class TeamService {

    private final TeamRepository teamRepository;
    private final SeasonService seasonService;

    @Transactional
    public TeamResponse createTeam(TeamRequest request) {
        Season season = seasonService.findSeasonOrThrow(request.getSeasonId());

        if (!season.isActive()) {
            throw new BadRequestException("Cannot add teams to an inactive season");
        }

        if (teamRepository.existsBySeasonIdAndName(request.getSeasonId(), request.getName())) {
            throw new BadRequestException("Team '" + request.getName() + "' already exists in this season");
        }

        Team team = Team.builder()
                .season(season)
                .name(request.getName())
                .strength(request.getStrength())
                .build();

        team = teamRepository.save(team);
        return mapToResponse(team);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeamsBySeason(UUID seasonId) {
        seasonService.findSeasonOrThrow(seasonId); // validate season exists
        return teamRepository.findBySeasonId(seasonId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID id) {
        Team team = findTeamOrThrow(id);
        return mapToResponse(team);
    }

    @Transactional
    public TeamResponse updateTeam(UUID id, TeamRequest request) {
        Team team = findTeamOrThrow(id);

        if (!team.getName().equals(request.getName())
                && teamRepository.existsBySeasonIdAndName(team.getSeason().getId(), request.getName())) {
            throw new BadRequestException("Team '" + request.getName() + "' already exists in this season");
        }

        team.setName(request.getName());
        team.setStrength(request.getStrength());

        team = teamRepository.save(team);
        return mapToResponse(team);
    }

    public Team findTeamOrThrow(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));
    }

    private TeamResponse mapToResponse(Team team) {
        return TeamResponse.builder()
                .id(team.getId())
                .seasonId(team.getSeason().getId())
                .seasonName(team.getSeason().getName())
                .name(team.getName())
                .strength(team.getStrength())
                .createdAt(team.getCreatedAt())
                .build();
    }
}
