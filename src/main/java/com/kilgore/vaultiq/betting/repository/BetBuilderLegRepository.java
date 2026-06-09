package com.kilgore.vaultiq.betting.repository;

import com.kilgore.vaultiq.betting.entity.BetBuilderLeg;
import com.kilgore.vaultiq.betting.entity.LegResult;
import com.kilgore.vaultiq.betting.entity.OutcomeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BetBuilderLegRepository extends JpaRepository<BetBuilderLeg, UUID> {

    List<BetBuilderLeg> findByFixtureIdAndResult(UUID fixtureId, LegResult result);

    List<BetBuilderLeg> findByBetBuilderId(UUID betBuilderId);

    boolean existsByBetBuilderIdAndFixtureIdAndOutcomeCategory(UUID betBuilderId, UUID fixtureId, OutcomeCategory outcomeCategory);
}
