package com.kilgore.vaultiq.league.service;

import com.kilgore.vaultiq.identity.entity.Role;
import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.service.UserService;
import com.kilgore.vaultiq.league.dto.LeagueRequest;
import com.kilgore.vaultiq.league.dto.LeagueResponse;
import com.kilgore.vaultiq.league.entity.League;
import com.kilgore.vaultiq.league.repository.LeagueRepository;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import com.kilgore.vaultiq.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LeagueService {

    private final LeagueRepository leagueRepository;
    private final UserService userService;

    @Transactional
    public LeagueResponse createLeague(LeagueRequest request) {
        User currentUser = userService.getCurrentUser();

        if (currentUser.getRole() != Role.LEAGUE_ADMIN && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only LEAGUE_ADMIN or ADMIN can create leagues");
        }

        if (leagueRepository.existsByName(request.getName())) {
            throw new BadRequestException("League with name '" + request.getName() + "' already exists");
        }

        League league = League.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createdBy(currentUser)
                .build();

        league = leagueRepository.save(league);
        return mapToResponse(league);
    }

    @Transactional(readOnly = true)
    public List<LeagueResponse> getAllLeagues() {
        return leagueRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LeagueResponse getLeagueById(UUID id) {
        League league = findLeagueOrThrow(id);
        return mapToResponse(league);
    }

    @Transactional
    public LeagueResponse updateLeague(UUID id, LeagueRequest request) {
        User currentUser = userService.getCurrentUser();
        League league = findLeagueOrThrow(id);

        if (!league.getCreatedBy().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Only the league creator or ADMIN can update this league");
        }

        if (!league.getName().equals(request.getName()) && leagueRepository.existsByName(request.getName())) {
            throw new BadRequestException("League with name '" + request.getName() + "' already exists");
        }

        league.setName(request.getName());
        league.setDescription(request.getDescription());

        league = leagueRepository.save(league);
        return mapToResponse(league);
    }

    public League findLeagueOrThrow(UUID id) {
        return leagueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("League", "id", id));
    }

    private LeagueResponse mapToResponse(League league) {
        return LeagueResponse.builder()
                .id(league.getId())
                .name(league.getName())
                .description(league.getDescription())
                .createdByUsername(league.getCreatedBy().getUsername())
                .seasonCount(league.getSeasons().size())
                .createdAt(league.getCreatedAt())
                .updatedAt(league.getUpdatedAt())
                .build();
    }
}
