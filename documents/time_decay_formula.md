# Time-Decay Odds Adjuster: Problem & Proposed Solution

## Problem Statement
In the original odds calculating engine, match-winner probability variations were driven purely by a linear goal difference (`scoreDiff * 0.350`). This meant that **Time**—the most absolutely critical factor in live sports betting—was ignored. 

A 1-0 lead at the 7th minute swung odds by the exact same amount as a 1-0 lead in the 88th minute. In reality, a late lead is a near-certainty, while an early lead is easily overturned. A mathematical framework was required to dynamically scale score importance based on remaining time.

---

## Proposed Formula: Exponential Time Decay

Instead of a static multiplier, introducing an **Exponential Time Decay Multiplier** solves the issue. Odds remain relatively steady and elastic during the first half of the match, but shift aggressively as the match reaches its conclusion.

### 1. The Time Decay Constant (`timeDecay`)
```java
int minute = fixture.getElapsedMinutes(); // 0 to 90
double tRatio = Math.min((double) minute / 90.0, 1.0); 
double timeDecay = Math.pow(tRatio, 2); // Exponential scaling (0.0 -> 1.0)
```

### 2. Match Winner (1X2) Dynamic Weights
```java
// Early goal = 0.200 shift, Late goal = up to 1.700 shift
double dynamicWeight = 0.200 + (1.500 * timeDecay); 
BigDecimal scoreFactor = new BigDecimal(scoreDiff).multiply(new BigDecimal(dynamicWeight));

BigDecimal newHomeOdds = baseHome.subtract(scoreFactor); // Trailing team odds go up, leader down
BigDecimal newAwayOdds = baseAway.add(scoreFactor);
```

### 3. Smart Draw Logic 
The draw moves in opposing directions based on the scoreline.

```java
BigDecimal newDrawOdds;
if (scoreDiff == 0) {
    // A draw is highly likely as time expires, odds crash downwards
    BigDecimal drop = baseDraw.subtract(new BigDecimal("1.100")).multiply(new BigDecimal(timeDecay));
    newDrawOdds = baseDraw.subtract(drop); 
} else {
    // If one team is trailing, less time means a draw is MUCH less likely. Odds skyrocket.
    double drawPenalty = Math.abs(scoreDiff) * (0.500 + 4.000 * timeDecay);
    newDrawOdds = baseDraw.add(new BigDecimal(drawPenalty));
}
```

---

## Example Calculation: The Contrast of Time

**Base Setup (Strength 9 vs 9):**
- **Score:** 1-0 (Home Leading `scoreDiff = 1`)
- **Base Home Odds:** 2.262
- **Base Draw Odds:** 4.318
- **Base Away Odds:** 2.639

### Scenario A: Early Goal (Minute 10)
At minute 10, the away team has 80 minutes to recover. The odds should only shift mildly.

- `tRatio = 10 / 90.0 = 0.111`
- `timeDecay = (0.111)^2 = 0.0123`
- `dynamicWeight = 0.200 + (1.500 * 0.0123) = 0.218`
- `scoreFactor = 1 * 0.218 = 0.218`

**Calculations:**
- Home Odds = `2.262 - 0.218` = **2.044** *(A reasonable, slight drop)*
- Away Odds = `2.639 + 0.218` = **2.857** *(An equally balanced bump)*
- Draw Odds Penalty = `1 * (0.500 + 4.000 * 0.0123) = 0.549`
- Draw Odds = `4.318 + 0.549` = **4.867**

### Scenario B: Late Goal (Minute 85)
At minute 85, the away team is in deep trouble. The math must slam the door shut.

- `tRatio = 85 / 90.0 = 0.944`
- `timeDecay = (0.944)^2 = 0.891`
- `dynamicWeight = 0.200 + (1.500 * 0.891) = 1.536`
- `scoreFactor = 1 * 1.536 = 1.536`

**Calculations:**
- Home Odds = `2.262 - 1.536` = `0.726` → **Clamped to MIN Limit (1.050)** *(Victory assured)*
- Away Odds = `2.639 + 1.536` = **4.175** *(Skyrockets)*
- Draw Odds Penalty = `1 * (0.500 + 4.000 * 0.891) = 4.064`
- Draw Odds = `4.318 + 4.064` = **8.382** *(Massively penalizes trailing team for lack of time)*
