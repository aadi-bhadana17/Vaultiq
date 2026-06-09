# 🏆 Vaultiq — Football League Management & Intelligent Wagering Platform

## Overview
Vaultiq is a football league management platform with an intelligent wagering engine built on top.
Designed and implemented a state-machine-driven fixture lifecycle, a three-layer odds engine, and
a concurrent bet placement system with race condition handling via optimistic locking.

Built with Spring Boot, PostgreSQL, JWT authentication, and Spring Scheduler.

---

## Tech Stack

**Backend**
- Java 17+ with Spring Boot
- Spring Security with JWT
- PostgreSQL + JPA/Hibernate
- Spring Scheduler for automated fixture/bet lifecycle management
- Lombok for boilerplate reduction
- Maven for dependency management

---

## Roles
- `USER` — Places bets, manages wallet, follows tipsters
- `LEAGUE_ADMIN` — Manages leagues, teams, fixtures, and match results
- `ADMIN` — Platform-level control

> All football data (teams, fixtures, results) is manually entered by LEAGUE_ADMIN.
> No external football API dependency.

---

## Fixture Lifecycle

- **SCHEDULED** — Fixture created, not yet accepting bets
- **OPEN** — Bets are live and accepting
- **LOCKED** — Bets frozen at kickoff
- **OPEN** — Re-opened briefly for live updates (if applicable)
- **FINISHED** — Result entered, bets settled

---

## Core Features

### 1. Bet Builder
Users compose a multi-selection accumulator bet across multiple fixtures.
Each selection is independently validated against fixture status and odds availability.
Total odds are compounded; payout is calculated at placement time and locked in.

### 2. Suspicious Bet Detection
System flags bets that deviate significantly from normal patterns —
unusually high stakes, odds manipulation signals, or coordinated placement timing.
Flagged bets are held for admin review before settlement.

### 3. Auto Bet + Auto Cashout
Users configure a rule: place a bet automatically when specific odds conditions are met.
Auto Cashout locks in a partial return when live odds shift past a configured threshold,
protecting the user from a potential loss before the fixture ends.

### 4. Tipster / Copy Bet System
High-performing users can be designated as Tipsters.
Other users can follow and copy their active bets (with configurable stake scaling).
Tipster performance metrics (ROI, win rate, follower count) are tracked and updated post-settlement.

### 5. Bet Syndicate
A group of users pools their stake on a shared bet.
One user initiates the syndicate; others join and contribute wallet balance.
Payout is distributed proportionally based on individual contribution at settlement.

### 6. Bet Insurance
Users optionally purchase insurance on a bet at placement time for a small premium.
If the bet loses, a configurable percentage of the stake is refunded to the wallet.
Insurance is priced dynamically based on current odds confidence.

### 7. Dynamic Bet Limits
Per-user bet limits are adjusted dynamically based on account history,
suspicious activity flags, and admin-configured platform-wide ceilings.
Limits are enforced at placement time before wallet deduction.

---

## Odds Engine (Three-Layer Architecture)

**Layer 1 — Base Odds**
Calculated from league standings, historical win/loss ratios between the two teams.

**Layer 2 — Fixture Adjustments**
Modifiers applied based on home/away advantage, current form streak, and head-to-head record gaps.

**Layer 3 — Market Adjustments**
Real-time adjustments based on total money wagered on each outcome (book balancing),
ensuring the platform maintains a sustainable margin.

Odds are stored in `FixtureOdds` with `@Version` for optimistic locking —
concurrent bet placements on the same fixture compete safely without over-deducting.

---

## Concurrency Model

- **Wallet deductions** — Pessimistic locking ensures no two simultaneous bets
  can double-spend the same wallet balance.
- **Odds updates** — Optimistic locking (`@Version` on `FixtureOdds`) with retry logic
  handles concurrent stake totals without serializing all bet placements.

---

## Automated Scheduling

- Fixture status transitions (SCHEDULED → OPEN, OPEN → LOCKED) triggered by Spring Scheduler
- Bet settlement triggered on FINISHED status
- Syndicate contribution window expiry handled automatically
- Auto Bet condition evaluation runs on a fixed-rate schedule

---

## Project Goal
Demonstrate AI-directed development as a legitimate engineering skill —
using AI tooling for architecture decisions, boilerplate generation, and design validation
while maintaining full ownership of architectural judgment and system design.

---

## Notes
See project documentation for concurrency load test results and odds engine calibration details.
