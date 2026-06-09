package com.kilgore.vaultiq.league.service;

import com.kilgore.vaultiq.league.entity.Fixture;
import com.kilgore.vaultiq.league.entity.FixtureStatus;
import com.kilgore.vaultiq.league.repository.FixtureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Fixture Kickoff Scheduler — runs every 30 seconds.
 * 
 * Automatically transitions SCHEDULED fixtures to OPEN
 * when their scheduledAt time has arrived.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FixtureKickoffScheduler {

    private final FixtureRepository fixtureRepository;

    @Scheduled(fixedRate = 30000) // every 30 seconds
    @SchedulerLock(name = "fixtureKickoffLock", lockAtLeastFor = "15s", lockAtMostFor = "25s")
    @Transactional
    public void checkAndKickoff() {
        List<Fixture> scheduledFixtures = fixtureRepository.findByStatus(FixtureStatus.SCHEDULED);

        if (scheduledFixtures.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();

        for (Fixture fixture : scheduledFixtures) {
            if (fixture.getScheduledAt() != null && !fixture.getScheduledAt().isAfter(now)) {
                fixture.setStatus(FixtureStatus.OPEN);
                fixture.setMatchMinute(0);
                fixtureRepository.save(fixture);

                log.info("Fixture kicked off: {} ({} vs {}) — status → OPEN",
                        fixture.getId(),
                        fixture.getHomeTeam().getName(),
                        fixture.getAwayTeam().getName());
            }
        }
    }
}
