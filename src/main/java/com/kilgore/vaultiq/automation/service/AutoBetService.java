package com.kilgore.vaultiq.automation.service;

import com.kilgore.vaultiq.automation.dto.AutoBetRuleRequest;
import com.kilgore.vaultiq.automation.dto.AutoBetRuleResponse;
import com.kilgore.vaultiq.automation.entity.AutoBetRule;
import com.kilgore.vaultiq.automation.repository.AutoBetRuleRepository;
import com.kilgore.vaultiq.betting.entity.BetOutcome;
import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.service.UserService;
import com.kilgore.vaultiq.league.entity.Team;
import com.kilgore.vaultiq.league.repository.TeamRepository;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoBetService {

    private final AutoBetRuleRepository ruleRepository;
    private final TeamRepository teamRepository;
    private final UserService userService;

    @Transactional
    public AutoBetRuleResponse createRule(AutoBetRuleRequest request) {
        User user = userService.getCurrentUser();

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", request.getTeamId()));

        BetOutcome outcome = parseOutcome(request.getOutcome());

        AutoBetRule rule = AutoBetRule.builder()
                .user(user)
                .team(team)
                .outcome(outcome)
                .minOdds(request.getMinOdds())
                .stake(request.getStake())
                .active(true)
                .build();

        rule = ruleRepository.save(rule);

        log.info("Auto-bet rule {} created by {} — team {}, outcome {}, minOdds {}, stake {}",
                rule.getId(), user.getUsername(), team.getName(), outcome, request.getMinOdds(), request.getStake());

        return mapToResponse(rule);
    }

    @Transactional(readOnly = true)
    public List<AutoBetRuleResponse> getUserRules(int page, int size) {
        User user = userService.getCurrentUser();
        Page<AutoBetRule> rules = ruleRepository.findByUserIdOrderByCreatedAtDesc(
                user.getId(), PageRequest.of(page, size));
        return rules.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AutoBetRuleResponse toggleRule(UUID ruleId, boolean active) {
        User user = userService.getCurrentUser();
        AutoBetRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("AutoBetRule", "id", ruleId));

        if (!rule.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not own this rule");
        }

        rule.setActive(active);
        ruleRepository.save(rule);

        return mapToResponse(rule);
    }

    @Transactional
    public void deleteRule(UUID ruleId) {
        User user = userService.getCurrentUser();
        AutoBetRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("AutoBetRule", "id", ruleId));

        if (!rule.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You do not own this rule");
        }

        ruleRepository.delete(rule);
    }

    // ── Helpers ──

    private BetOutcome parseOutcome(String outcome) {
        try {
            return BetOutcome.valueOf(outcome.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid outcome: " + outcome);
        }
    }

    private AutoBetRuleResponse mapToResponse(AutoBetRule rule) {
        return AutoBetRuleResponse.builder()
                .id(rule.getId())
                .teamId(rule.getTeam().getId())
                .teamName(rule.getTeam().getName())
                .outcome(rule.getOutcome().name())
                .minOdds(rule.getMinOdds())
                .stake(rule.getStake())
                .active(rule.isActive())
                .lastTriggeredAt(rule.getLastTriggeredAt())
                .createdAt(rule.getCreatedAt())
                .build();
    }
}
