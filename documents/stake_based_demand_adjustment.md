# Demand Adjustment: Stake-Based vs Volume-Based

## Problem Statement

In the original odds calculating engine, demand adjustments were driven by the *number of bets* placed on a specific outcome, rather than the *monetary amount* (stake) placed. 

This created a severe vulnerability: twenty small users placing $100 bets generated a significantly larger downward shift on odds than a single "whale" placing a $10,000 bet. From a bookmaker's perspective, this is critically flawed—financial exposure and liability are tied exclusively to the monetary volume at risk, not the head count. The odds must correctly represent real financial liquidity to protect the platform.

---

## Proposed Solution 

Change the calculation engine to track and compute market movement based on `BigDecimal totalStake` per outcome, passing the user's stake into the adjustment method. 

### Java Implementation

```java
    /**
     * Increment demand based on the actual financial stake and recalculate match-winner odds.
     * Only applies to HOME_WIN, DRAW, AWAY_WIN outcomes.
     */
    @Transactional
    public void applyDemandAdjustment(UUID fixtureId, String outcome, BigDecimal betStake) {
        FixtureOdds odds = findOddsOrThrow(fixtureId);

        // Increment the relevant monetary pool rather than the bet counter
        switch (outcome) {
            case "HOME_WIN" -> odds.setTotalHomeStake(odds.getTotalHomeStake().add(betStake));
            case "DRAW" -> odds.setTotalDrawStake(odds.getTotalDrawStake().add(betStake));
            case "AWAY_WIN" -> odds.setTotalAwayStake(odds.getTotalAwayStake().add(betStake));
            default -> {
                // Non-match-winner outcome — no demand adjustment
                return;
            }
        }

        // Calculate the total money placed across all match-winner events
        BigDecimal totalStake = odds.getTotalHomeStake()
                                  .add(odds.getTotalDrawStake())
                                  .add(odds.getTotalAwayStake());

        if (totalStake.compareTo(BigDecimal.ZERO) > 0) {
            // Find the percentage distribution of the total pool each outcome holds
            BigDecimal homeRatio = odds.getTotalHomeStake().divide(totalStake, 6, RoundingMode.HALF_UP);
            BigDecimal drawRatio = odds.getTotalDrawStake().divide(totalStake, 6, RoundingMode.HALF_UP);
            BigDecimal awayRatio = odds.getTotalAwayStake().divide(totalStake, 6, RoundingMode.HALF_UP);

            BigDecimal evenShare = BigDecimal.ONE.divide(new BigDecimal("3"), 6, RoundingMode.HALF_UP);

            // Shift calculation relying on STAKE_SHIFT_FACTOR
            BigDecimal homeShift = homeRatio.subtract(evenShare).multiply(totalStake).multiply(STAKE_SHIFT_FACTOR);
            BigDecimal drawShift = drawRatio.subtract(evenShare).multiply(totalStake).multiply(STAKE_SHIFT_FACTOR);
            BigDecimal awayShift = awayRatio.subtract(evenShare).multiply(totalStake).multiply(STAKE_SHIFT_FACTOR);

            // Subtracting the shift means heavily-backed outcomes drop in odds
            odds.setHomeWinOdds(clamp(odds.getHomeWinOdds().subtract(homeShift)));
            odds.setDrawOdds(clamp(odds.getDrawOdds().subtract(drawShift)));
            odds.setAwayWinOdds(clamp(odds.getAwayWinOdds().subtract(awayShift)));
        }

        fixtureOddsRepository.save(odds);
    }
```

---

## New Problems That May Arise

While this moves the math to a realistic financial model, weighting by gross stake introduces entirely new technical edge cases. 

### 1. The Whale Distortion Problem
If a high-roller drops an astronomical bet (e.g., $1,000,000) on a match with low existing liquidity, the `totalStake` multiplies the shift drastically. The linear formula will produce a mathematically enormous shift, instantly slamming the odds to the absolute floor (`1.050`) and leaving the other outcomes highly vulnerable.
**Solution**: Implement a logarithmic dampener (e.g. `Math.log10()`) or cap the maximum shift any single transaction can impact on the market. 

### 2. Multiplier Calibration Crisis (`STAKE_SHIFT_FACTOR`)
Switching from "bet count" to "money" means the magnitude of integers has exploded. A scenario with 1,000 users used to mean `totalBets = 1,000`. Now, 1,000 users betting $100 means `totalStake = 100,000`. If you use your legacy factor, odds dropping by `0.1` will now drop by `10.0`.
**Solution**: The new `STAKE_SHIFT_FACTOR` must be significantly smaller than the previous one. A good rule of thumb is to divide the old factor by the projected average bet size. 

### 3. Early Liquidity Volatility
When a match first opens and `totalStake` is zero, the very first $100 placed shifts that outcome's ratio to 100%. The math reacts violently to any wager placed before the pool is adequately dispersed.
**Solution**: Seed the initial fixture odds with "Phantom Liquidity"—virtual stakes of, say, $5,000 spread evenly across Home, Draw, and Away. This artificial baseline ensures that early real bets only push the needle mildly until genuine user volume builds up.
