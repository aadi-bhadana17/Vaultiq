# Advanced Vaultiq Odds Calculation Engine v2.1 (Interview-Ready Architecture)

## Overview & Philosophy
The Vaultiq math engine breaks down into three distinct layers, designed not just for realistic simulation, but defensively built with financial liability, probability normalization, and market risk in mind:
1. **Layer 1 (Base Model)**: Match Winner driven by *Effective Strength* (mapped via Sigmoid function) and Goal Markets driven by a *Context-Normalized Expected Goals (xG)* model.
2. **Layer 2 (Market Adjustments)**: Dynamically balances odds based on *Financial Liability* (exposure risk) rather than raw stake volume.
3. **Layer 3 (In-Play Engine)**: Recalculates *underlying probabilities* (not raw odds) using time-decay and dynamically scaled disciplinary factors.
4. **Post-Match (Elo Rating)**: Automatically updates the Base Strength of teams using a self-healing 100-1000 scale Elo system.

---

## 1. Data Sources & Prerequisites
Before the engine mathematically computes a single odd, it requires the following data per team.
*   **Base Strength (`int 100-1000`)**: A dynamic Elo rating initialized at 500, mathematically updated after every match.
*   **Recent Form Data**: The points aggregated across the last 5 `MatchResult` records for the team.
*   **H2H Data**: The win/loss ratio from the recent head-to-head `MatchResult` records between Team A and Team B.
*   **League Averages**: `league_avg_goals` tracking the baseline scoring rate across the entire season.
*   **Goal Averages (`double`)**: `avg_goals_scored` and `avg_goals_conceded` for each team.
*   **Live Disciplinary Data (`int`)**: `red_cards` and `yellow_cards` dynamically updated in live matches.

---

## 2. Match Winner (1X2) Base Pre-Match Odds (Layer 1)
*Goal: To predict who will win before the match starts using non-linear scaling and strict probability normalization.*

### Step 2.1: Effective Strength Calculation
We augment the 100-1000 base Elo rating into a fluid floating-point number using Form and H2H (Historical Bias).
```java
// On a 100-1000 scale, a home advantage might be worth +30 points
double HOME_ADVANTAGE = 30.0;

// Details on formBonus and h2hOffset omitted for brevity, calculated from recent history.
// A massive form win streak might add +40 points, H2H dominance might add +50 points.
double effectiveHomeStrength = homeTeam.getStrength() + homeFormBonus + h2hOffset + HOME_ADVANTAGE;
double effectiveAwayStrength = awayTeam.getStrength() + awayFormBonus - h2hOffset; 

double strengthDiff = effectiveHomeStrength - effectiveAwayStrength;
```

### Step 2.2: Sigmoid Probability Mapping
Instead of linear scaling (which breaks at extremes), we use a logistic (sigmoid) function to map the strength difference to a smooth win probability curve.
```java
// k defines how steeply the advantage translates to pure probability.
// Since strengthDiff is now on a 100-1000 scale, k is adjusted downwards (e.g., 0.004)
double k = 0.004; 
double rawHomeProb = 1.0 / (1.0 + Math.exp(-k * strengthDiff));
double rawAwayProb = 1.0 / (1.0 + Math.exp(k * strengthDiff)); // Inverse curve

// Draw probability peaks when teams are equal (strengthDiff = 0)
// Adjusted for the scale. 
double rawDrawProb = 0.30 * Math.exp(-0.0001 * Math.pow(strengthDiff, 2)); 
```

### Step 2.3: Probability Normalization & Odds Generation
To prevent mathematical drift and arbitrage exploitation, all probabilities must sum perfectly to `1.0` before adding the bookmaker margin.
```java
double totalProbSum = rawHomeProb + rawAwayProb + rawDrawProb;

double pHome = rawHomeProb / totalProbSum;
double pAway = rawAwayProb / totalProbSum;
double pDraw = rawDrawProb / totalProbSum;
// pHome + pAway + pDraw is now exactly 1.0

double MARGIN = 1.05; // 5% Overround
double homeOdds = (1.0 / pHome) / MARGIN;
double awayOdds = (1.0 / pAway) / MARGIN;
double drawOdds = (1.0 / pDraw) / MARGIN;
```

---

## 3. Context-Normalized Expected Goals (xG) Model
*Goal: Separate "Winning Strength" from "Scoring Tendency" using contextual league baselines.*

A team might score a high average by farming against weak teams. We normalize their stats against the `league_avg_goals` to find true Attack/Defense coefficients.

```java
double LEAGUE_AVG = 2.6; // Example average goals per match in the league

double homeAttackStrength = homeTeam.getAvgScored() / LEAGUE_AVG;
double awayDefenseWeakness = awayTeam.getAvgConceded() / LEAGUE_AVG;

double awayAttackStrength = awayTeam.getAvgScored() / LEAGUE_AVG;
double homeDefenseWeakness = homeTeam.getAvgConceded() / LEAGUE_AVG;

// Formula: Attack * Opponent Defense * League Average
double homeXg = homeAttackStrength * awayDefenseWeakness * LEAGUE_AVG;
double awayXg = awayAttackStrength * homeDefenseWeakness * LEAGUE_AVG;

double matchXg = homeXg + awayXg; 
```

---

## 4. Over / Under Goals Markets
*Implementation Note: We intentionally use a logistic sigmoid approximation of the Poisson distribution. While standard Poisson calculations exist, logistic mapping operates at O(1) computational complexity without heavy factorials and acts gracefully around defined betting pivots (1.5, 2.5, 3.5).*

```java
// Approximated Poisson cumulative distribution around pivots
double over25Prob = 1.0 / (1.0 + Math.exp(-1.5 * (matchXg - 2.5)));
double pUnder25 = 1.0 - over25Prob;
double under25Odds = (1.0 / pUnder25) / MARGIN;
```

---

## 5. Market Adjustments: Liability-Based Risk Control (Layer 2)
*Goal: Protect the platform from severe financial exposure by shifting odds based on potential liability, not just the raw volume of money placed.*

If $1,000 is placed on an outcome with `Odds = 10.00`, the platform's liability is $10,000. If $1,000 is placed on `Odds = 1.20`, liability is only $1,200. The market must shift based on *Risk*, not gross volume.

```java
// Track monetary exposure per outcome
double homeLiability = totalHomeStake * currentHomeOdds;
double drawLiability = totalDrawStake * currentDrawOdds;
double awayLiability = totalAwayStake * currentAwayOdds;

double totalLiability = homeLiability + drawLiability + awayLiability;

// Shift the underlying probabilities (not the odds directly) 
// to squeeze the most vulnerable market outcome down, and inflate the safest outcome.
// Follow up by re-normalizing the probabilities (Step 2.3) and regenerating odds. 
```

---

## 6. Live In-Play Adjustments & Scaled Cards (Layer 3)
*Goal: Instantly react to goals and cards by recalculating underlying probabilities dynamically.*

**Critical Fix:** Modifying raw decimal odds mathematically breaks the `1.0` sum consistency. The In-Play engine must instead recalculate the *probabilities* (`pHome`, `pAway`, `pDraw`) accounting for time decay, and then regenerate the odds.

### Dynamically Scaled Card Penalties
A red card at 10 minutes is catastrophic. A red card at 88 minutes is negligible. The disciplinary penalty must scale directly with remaining time.

```java
// On a 100-1000 Elo scale, a red card might equate to a brutal 150-point strength deduction.
double baseRedPenaltyElo = 150.0; 

// Minute 10 -> high remaining time factor (e.g. ~1.9 penalty weight)
// Minute 88 -> low remaining time factor (e.g. ~1.0 penalty weight)
double remainingTimeFactor = (90.0 - matchMinute) / 90.0;
double scaledRedPenaltyElo = baseRedPenaltyElo * (1.0 + remainingTimeFactor); 

// Inject this scaled penalty against the team's underlying Effective Strength
// Then rerun the Sigmoid Mapping (Step 2.2) using the updated remaining match time.
```

---

## 7. Post-Match: Dynamic Elo Adjustment
*Goal: Create a "self-healing" rating system. When a match concludes (status -> FINISHED), automatically update Base Strengths using Elo Rating logic.*

```java
public void updateTeamStrengths(MatchResult result) {
    // 7.1 Grab the pre-match probabilities we calculated in Layer 1
    double expectedHomeWinProb = getPreMatchHomeProb(fixture); 
    double expectedAwayWinProb = getPreMatchAwayProb(fixture);

    // 7.2 Determine the actual outcome values (1.0 for Win, 0.5 for Draw, 0.0 for Loss)
    double actualHomeScore = (result.homeWon()) ? 1.0 : (result.isDraw()) ? 0.5 : 0.0;
    double actualAwayScore = (result.awayWon()) ? 1.0 : (result.isDraw()) ? 0.5 : 0.0;

    // 7.3 Asymmetric Dynamic K-Factors (The "Gravity" Engine)
    // - Upward Gravity (Gains): Inversely scales with Elo. A high Elo naturally suppresses rating gains. 
    // - Downward Gravity (Losses): Directly scales with Elo. A high Elo multiplies rating losses.
    double BASE_K = 40.0;
    
    double homeDelta = actualHomeScore - expectedHomeWinProb;
    double awayDelta = actualAwayScore - expectedAwayWinProb;

    // Calculate Home Dynamic K
    double homeK = BASE_K;
    if (homeDelta > 0) { 
        // Gains: Exponential inverse decay. 1000 Elo = 0.1 scale.
        double upScale = Math.max(0.1, Math.pow(1.0 - (homeTeam.getStrength() / 1000.0), 1.5));
        homeK = BASE_K * upScale;
    } else { 
        // Losses: Logarithmic direct scaling. 1000 Elo = 1.2 scale.
        double downScale = Math.max(0.2, (Math.log10(homeTeam.getStrength()) / Math.log10(1000.0)) * 1.2);
        homeK = BASE_K * downScale;
    }
    
    // Calculate Away Dynamic K
    double awayK = BASE_K;
    if (awayDelta > 0) {
        double upScale = Math.max(0.1, Math.pow(1.0 - (awayTeam.getStrength() / 1000.0), 1.5));
        awayK = BASE_K * upScale;
    } else {
        double downScale = Math.max(0.2, (Math.log10(awayTeam.getStrength()) / Math.log10(1000.0)) * 1.2);
        awayK = BASE_K * downScale;
    }

    // 7.4 Compute new Elo ratings using dynamic localized K
    int newHomeStrength = (int) Math.round(homeTeam.getStrength() + homeK * homeDelta);
    int newAwayStrength = (int) Math.round(awayTeam.getStrength() + awayK * awayDelta);

    // Clamp ratings to the 100 - 1000 scale
    homeTeam.setStrength(Math.max(100, Math.min(1000, newHomeStrength)));
    awayTeam.setStrength(Math.max(100, Math.min(1000, newAwayStrength)));

    teamRepository.saveAll(List.of(homeTeam, awayTeam));
}
```

*Example (The Gravity Effect):*
If Madrid (Elo 950) plays Alaves (Elo 300) and Madrid wins as expected:
Madrid's Gain: `homeK` forces an inverse scale down. They might only get `40 * 0.1 * 0.15` = **`+0.6 points`**.
But if Madrid loses:
Madrid's Loss: `homeK` forces a direct multiplier up. They suffer `40 * 1.2 * (-0.85)` = **`-41 points`**.

This system brutally enforces rank deflation at the top, preventing rating hoarding.
