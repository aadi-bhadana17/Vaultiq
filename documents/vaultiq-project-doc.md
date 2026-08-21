# Vaultiq — Project Document

## Overview
Vaultiq is a football league management and intelligent wagering platform built as a modular monolith using Spring Boot + PostgreSQL. The project is AI-directed, with the primary learning goals being **Security, Concurrency, and Real-time Scheduling**.

---

## Roles
- **USER** — places bets, joins syndicates, follows tipsters
- **LEAGUE_ADMIN** — manages leagues, teams, fixtures, updates scores
- **ADMIN** — platform-level control, user management

---

## Tech Stack
- Java 17, Spring Boot
- PostgreSQL + JPA/Hibernate
- JWT Authentication
- Spring Scheduler
- No external API dependencies

---

## League Management

### Structure
`League → Season (one active at a time) → Team (with strength 1-10, home/away) → Fixture → MatchResult`

### Fixture Status Flow
`SCHEDULED → OPEN → LOCKED → OPEN → LOCKED → ... → FINISHED`

- LEAGUE_ADMIN opens the scorecard → fixture goes **LOCKED**, betting stops for all users
- Admin updates score + clicks confirm → odds recalculate → fixture goes **OPEN**, betting resumes
- Once final result entered → fixture goes **FINISHED**, bet settlement triggers automatically

### Team Strength
- LEAGUE_ADMIN assigns strength score (1-10) per team at registration
- Used as base for odds calculation

---

## Odds Engine

### 3 Layers:

**Layer 1 — Base Odds (pre-match)**
Calculated from:
- Team strength differential
- Home/away factor
- Starting point: 1.5 base for both, adjusted by strength and venue

**Layer 2 — Demand Adjustment (ongoing)**
- As more users bet on a team → odds drop for that team
- Classic bookmaker volume balancing

**Layer 3 — In-play Adjustment (on score update)**
- When LEAGUE_ADMIN updates score and unlocks fixture
- Odds recalculate based on current scoreline + match minute
- Winning team odds drop, losing team odds rise

### During LOCKED status
- No betting allowed
- AutoBet and AutoCashout schedulers skip LOCKED fixtures

---

## Wallet
- Same pattern as Flocko — deposit/withdraw
- Simple balance on User entity
- Pessimistic locking for concurrency on wallet operations
- Platform holds insurance premiums

---

## Big 7 Features

### 1. Bet Builder
- User combines multiple match outcomes into one bet
- Outcomes: match winner, over/under goals, both teams to score
- Combined odds = multiplication of individual odds
- All outcomes must win for bet to succeed

### 2. Suspicious Bet Detection
- Flags unusual patterns — sudden large bets, rapid sequential bets, betting against own pattern
- Auto-restricts user betting when flagged
- ADMIN can review and unrestrict

### 3. Auto Bet + Auto Cashout
- **Auto Bet** — user sets rule: "place ₹X on Team A if odds exceed Y before match starts"
- **Auto Cashout** — user sets rule: "cash out when profit hits ₹X or loss hits ₹Y"
- Both run on scheduler, skip LOCKED fixtures
- Hits all 3 pillars — scheduling, concurrency, security

### 4. Tipster / Copy Bet System
- Any USER can become a tipster after: 100+ bets in last 30 days + minimum win rate threshold
- Followers copy bets — linked bet created using follower's wallet at same odds
- On win: tipster receives a % of follower's profit, calculated based on tipster's win ratio
- Tipster credibility score updates after every bet result

### 5. Bet Syndicate
- Group of users pool wallet money for a single bet
- One member creates syndicate, others join and contribute
- System auto-distributes winnings proportionally on settlement
- Same pattern as Flocko SharedCart

### 6. Bet Insurance
- User pays a premium to insure a bet
- Premium varies based on odds (higher odds = higher risk = higher premium)
- Platform keeps the premium
- On loss: user receives partial refund (not full)
- On win: no refund, premium kept by platform

### 7. Dynamic Bet Limits
- Max bet limit per user adjusts based on:
  - Betting behavior history
  - Suspicious activity score
  - Account age
- System recalculates limits periodically via scheduler

---

## Bet Settlement
- Triggered automatically when LEAGUE_ADMIN marks fixture as FINISHED
- Scheduler picks up FINISHED fixtures with unsettled bets
- Settles regular bets, builder bets, syndicate bets, insurance payouts
- Tipster cuts distributed on settled copy bets

---

## Key Architectural Decisions
- No external API — all data entered manually by LEAGUE_ADMIN
- Modular monolith — no microservices
- Pessimistic locking on wallet for concurrency
- Fixture LOCK mechanism replaces need for real-time infrastructure
- Separate tables for each bet type (Bet, BetBuilder, Syndicate)
- AutoBet/AutoCashout as scheduled jobs, not event-driven
- Tipster cut % derived from win ratio, not fixed
- Insurance premium varies by odds, platform keeps it
