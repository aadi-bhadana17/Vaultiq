package com.kilgore.vaultiq.league.service;

import com.kilgore.vaultiq.league.entity.Fixture;
import com.kilgore.vaultiq.league.entity.FixtureStatus;
import com.kilgore.vaultiq.league.repository.FixtureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchClockScheduler {

    private final FixtureRepository fixtureRepository;
    private final MatchResultService matchResultService;

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Transactional
    public void updateMatchClocks() {
        // 1. Process OPEN fixtures
        List<Fixture> openFixtures = fixtureRepository.findByStatus(FixtureStatus.OPEN);
        for (Fixture fixture : openFixtures) {
            int currentMinute = fixture.getMatchMinute();
            
            if (currentMinute < 45) {
                fixture.setMatchMinute(currentMinute + 1);
                if (fixture.getMatchMinute() == 45) {
                    fixture.setStatus(FixtureStatus.HALF_TIME);
                }
            } else if (currentMinute >= 46 && currentMinute < 90) {
                fixture.setMatchMinute(currentMinute + 1);
                if (fixture.getMatchMinute() == 90) {
                    fixture.setStatus(FixtureStatus.AWAITING_EXTRA_TIME);
                    fixture.setClockStatusUpdatedAt(LocalDateTime.now());
                }
            }
            fixtureRepository.save(fixture);
        }

        // 2. Process AWAITING_EXTRA_TIME fixtures
        List<Fixture> awaitingFixtures = fixtureRepository.findByStatus(FixtureStatus.AWAITING_EXTRA_TIME);
        LocalDateTime twoMinutesAgo = LocalDateTime.now().minusMinutes(2);
        for (Fixture fixture : awaitingFixtures) {
            if (fixture.getClockStatusUpdatedAt() != null && fixture.getClockStatusUpdatedAt().isBefore(twoMinutesAgo)) {
                log.info("Fixture {} reached 90 mins with no extra time provided. Finishing match.", fixture.getId());
                matchResultService.finishFixture(fixture.getId());
            }
        }

        // 3. Process ADDITIONAL_TIME fixtures
        List<Fixture> additionalTimeFixtures = fixtureRepository.findByStatus(FixtureStatus.ADDITIONAL_TIME);
        for (Fixture fixture : additionalTimeFixtures) {
            int currentMinute = fixture.getMatchMinute();
            int maxMinute = 90 + fixture.getAdditionalTimeMinutes();
            
            fixture.setMatchMinute(currentMinute + 1);
            if (fixture.getMatchMinute() >= maxMinute) {
                log.info("Fixture {} reached end of additional time ({} mins). Finishing match.", fixture.getId(), maxMinute);
                matchResultService.finishFixture(fixture.getId());
            } else {
                fixtureRepository.save(fixture);
            }
        }
    }
}
