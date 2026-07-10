# MULTI_GROUP_PLATFORM_DESIGN.md

## Overview

This document defines the architecture for the Multi-Group Platform evolution of ResenhaBET.

The current implementation is effectively single-tenant: one deployment serves one group of friends and all data belongs to that group implicitly.

The objective of this refactor is to evolve ResenhaBET into a single deployment capable of serving multiple independent groups simultaneously while preserving complete logical isolation between them.

This document supersedes the original "one instance per group" strategy and establishes Group as the primary tenancy boundary.

---

# Goals

## Functional Goals

Allow multiple friend groups to coexist in the same deployment.

Examples:

* Copa Gegê
* Resenha da Faculdade
* FIFA da Empresa

Each group must have:

* Independent tournaments
* Independent players
* Independent statistics
* Independent rankings
* Independent betting history

Users may participate in multiple groups simultaneously.

---

## Non-Goals

This is NOT intended to become a SaaS platform.

This is NOT intended to support public registrations.

This is NOT intended to introduce billing or real-money transactions.

This remains a closed-community platform.

---

# Core Architectural Decision

## Group is the Tenant

The fundamental architectural decision is:

```text
GROUP = TENANT
```

Every business operation must execute within a Group context.

No entity belonging to one group may be visible or accessible from another group.

---

# High-Level Domain Model

```text
User
 │
 ├── Session
 │
 └── GroupMember
       │
       ▼
     Group
       │
       ├── Players
       └── Tournaments
               │
               ├── Events
               ├── Markets
               ├── Bets
               ├── TournamentWallets
               └── Statistics
```

---

# Entity Classification

The first step in the refactor is classifying every entity according to its ownership boundary.

---

# Global Entities

Global entities are shared across the entire platform.

They do not belong to any group.

## User

Represents a platform account.

A User may participate in multiple Groups.

---

## Session

Authentication session.

Belongs to a User only.

---

## Competition

Represents real-world competitions.

Examples:

* World Cup 2026
* Champions League 2026
* Premier League 2026

Competitions are global because they represent real-world catalog data.

Duplicating them per Group provides no benefit.

---

## Group

Represents a friend community.

Examples:

* Copa Gegê
* FIFA da Faculdade

Acts as the tenancy root.

---

## GroupMember

Relationship entity between User and Group.

Responsible for:

* Membership
* Permissions
* Roles

Example:

```text
Francisco
 ├── Copa Gegê
 ├── FIFA da Empresa
 └── Resenha da Faculdade
```

---

# Group-Scoped Entities

These entities belong to a specific Group.

---

## Player

Player becomes Group-scoped.

This is one of the most important architectural changes.

Current model:

```text
User <-> Player
```

New model:

```text
Group
 │
 └── Player
         │
         └── User (optional)
```

Reason:

A player's sporting history belongs to a Group.

Example:

Group A:

Francisco
Elo = 1400

Group B:

Francisco
Elo = 980

Both are valid simultaneously.

---

## Team

Current recommendation:

Remain global.

Reasons:

* Mostly catalog data
* FIFA teams
* National teams
* Club teams

No meaningful business value exists in duplicating teams per group.

---

# Tournament-Scoped Entities

These entities belong to a specific Tournament.

Tournament becomes the economic boundary of the platform.

This is a deliberate decision.

---

# TournamentWallet

Current implementation:

```text
User
 │
 Wallet
```

Proposed implementation:

```text
Tournament
 │
 TournamentWallet
 │
 User
```

Constraint:

```sql
UNIQUE (tournament_id, user_id)
```

---

## Rationale

ResenhaBET does not use real money.

Persistent balances across tournaments create undesirable effects:

* Wealth accumulation
* New-player disadvantage
* Mixed historical data
* Difficult tournament comparisons

Tournament-scoped wallets solve these problems naturally.

Each tournament starts with a fresh economy.

Example:

```text
COPA GEGÊ 26

Francisco
Initial Balance: 1000
Final Balance: 4200
```

Next season:

```text
COPA GEGÊ 27

Francisco
Initial Balance: 1000
```

Balance resets.

Sporting history remains.

Betting history does not.

---

# Tournament Financial Entities

The following entities become Tournament-scoped:

## TournamentWallet

Stores available balance.

---

## Transaction

Stores financial movements.

Examples:

* Initial Credit
* Deposit
* Bet Placed
* Bet Won

---

## BetSlip

Belongs to a Tournament.

---

## BetSlipItem

Belongs indirectly to a Tournament through BetSlip.

---

# Tournament Statistics

Tournament-scoped financial data enables future features:

---

## Betting Ranking

```text
1° Francisco
2° Jonas
3° Rafael
```

Based on final balance.

---

## ROI Ranking

```text
ROI = Profit / Initial Balance
```

---

## Highest Profit

```text
Final Balance - Initial Balance
```

---

## Most Aggressive Bettor

Based on total wagered amount.

---

## Hit Rate

```text
Winning Bets / Total Bets
```

---

# Sporting vs Betting History

The platform intentionally separates sporting history from betting history.

## Sporting History

Group-scoped.

Persists across tournaments.

Examples:

* Elo
* Wins
* Losses
* Goals
* Championships

---

## Betting History

Tournament-scoped.

Resets every tournament.

Examples:

* Balance
* Profit
* ROI
* Betting rankings

This separation aligns with the intended social experience of ResenhaBET.

Players build long-term sporting legacies.

Betting competitions restart every season.

---

# Tournament Ownership

Tournament gains:

```java
@ManyToOne
private Group group;
```

New hierarchy:

```text
Group
 │
 └── Tournament
         │
         ├── Round
         ├── Event
         ├── Market
         └── Outcome
```

All tournament operations become group-aware.

---

# Group Context

The application must maintain an active Group context.

A User may belong to multiple Groups simultaneously.

Therefore authentication alone is insufficient.

Current:

```text
Session
 └── User
```

Future:

```text
Session
 ├── User
 └── Current Group
```

---

## Context Switching

Proposed flow:

```http
POST /api/v1/groups/{id}/switch
```

Response:

```json
{
  "groupId": 5,
  "groupName": "Copa Gegê"
}
```

The selected Group becomes the active context.

---

# Security Model

Every Group-scoped query must validate ownership.

Example:

```java
if (!entity.getGroup().getId().equals(currentGroupId)) {
    throw new UnauthorizedException();
}
```

This rule applies to:

* Players
* Tournaments
* Events
* Bets
* Statistics

---

# Migration Strategy

The migration should occur incrementally.

---

## Phase 1

Infrastructure

Create:

* Group
* GroupMember

Implement:

* Group creation
* Group joining
* Group listing
* Group switching

No business logic changes.

---

## Phase 2

Tournament Isolation

Add:

```text
tournament.group_id
```

All tournament queries become group-aware.

---

## Phase 3

Player Isolation

Add:

```text
player.group_id
```

Move sporting statistics into Group scope.

---

## Phase 4

Tournament Economy

Replace:

```text
Wallet
```

with:

```text
TournamentWallet
```

Migrate:

* Wallet
* Transaction
* BetSlip
* BetSlipItem

to Tournament scope.

---

## Phase 5

Authorization Hardening

Introduce:

```java
CurrentGroupContext
```

All services validate group ownership automatically.

---

# Final Architecture

```text
GLOBAL

User
Session
Competition
Group
GroupMember
Team

--------------------------------

GROUP-SCOPED

Player
Tournament

--------------------------------

TOURNAMENT-SCOPED

TournamentPlayer
TournamentWallet
Transaction
BetSlip
BetSlipItem
Event
Market
Outcome
Tournament Statistics
Betting Rankings
```

---
# APPENDIX A - GROUP PARTICIPATION MODEL (UNDER EVALUATION)

## Context

During the Multi-Group Platform design discussions, a major architectural question emerged:

How should Groups participate in real-world competitions without duplicating Tournament structures?

Example:

```text
World Cup 2026
```

Multiple Groups may want to bet on the same competition:

```text
Copa Gegê
Resenha da Faculdade
FIFA da Empresa
```

All groups consume the same football data.

However:

* betting rankings must be isolated
* wallets must be isolated
* transactions must be isolated
* betting history must be isolated

The challenge is avoiding large-scale duplication of Tournament data.

---

# Initial Approach Considered

One possible design was:

```text
Competition
    |
    +-- Tournament (Group A)
    |
    +-- Tournament (Group B)
    |
    +-- Tournament (Group C)
```

Each Group would receive its own Tournament clone.

This would create independent:

* Events
* Markets
* Outcomes
* Rounds

for every participating Group.

---

# Problems With Tournament Cloning

Consider:

```text
World Cup 2026
```

with:

```text
64 matches
```

If:

```text
100 groups
```

participate,

the platform would generate:

```text
6400 Event records
```

instead of:

```text
64 Event records
```

The same issue would affect:

* Markets
* Outcomes
* Settlement jobs
* Synchronization jobs

The scheduler would need to update every cloned structure individually.

This introduces unnecessary database growth and operational complexity.

---

# CompetitionInstance Approach

A more normalized model was considered:

```text
Competition
    |
    +-- CompetitionInstance
            |
            +-- Events
            +-- Markets
            +-- Outcomes
```

Groups would not own Tournament copies.

Instead, they would participate in a shared CompetitionInstance.

This model is architecturally elegant and highly scalable.

However, it introduces significant refactoring requirements because the current system is heavily centered around the Tournament entity.

Migrating the entire application to a CompetitionInstance-centric architecture would require substantial changes across:

* Scheduler
* Settlement
* Market generation
* Event synchronization
* APIs
* Frontend flows

---

# Current Recommended Approach

The current preferred strategy is to preserve the existing Tournament architecture and introduce Group-level isolation only where required.

Instead of cloning Tournament data, Groups would share the same Tournament.

Example:

```text
World Cup 2026 Tournament
```

exists only once.

The following entities remain shared:

```text
Tournament
Round
Event
Market
Outcome
```

All Groups consume the same underlying sporting data.

---

# Group-Scoped Betting Layer

Isolation occurs at the betting layer.

The following entities become Group-aware:

```text
TournamentWallet
Transaction
BetSlip
BetSlipItem
```

Each record contains:

```text
group_id
```

and remains logically isolated.

Example:

```text
Tournament
    |
    +-- Brazil vs Argentina
```

Single shared event.

However:

```text
Group A
    |
    +-- Bets
    +-- Wallets
    +-- Rankings
```

and

```text
Group B
    |
    +-- Bets
    +-- Wallets
    +-- Rankings
```

remain completely independent.

---

# Proposed GroupTournament Entity

Although not strictly required initially, introducing a lightweight association entity is recommended.

```text
Group
    |
    +-- GroupTournament
            |
            +-- Tournament
```

Initial structure:

```java
GroupTournament

id
group_id
tournament_id
created_at
```

---

# Why GroupTournament May Become Important

Today, a Group simply participates in a Tournament.

In the future, Group-specific tournament configuration may be required.

Examples:

* Initial bankroll amount
* Betting limits
* Cashout rules
* Market visibility
* Group-specific tournament settings

Example:

```text
Group A

Initial Bankroll:
1000
```

```text
Group B

Initial Bankroll:
5000
```

The GroupTournament entity provides a natural location for those future settings.

---

# Advantages Of This Approach

## Minimal Refactoring

Existing REAL_FOOTBALL architecture remains largely unchanged.

Current synchronization jobs continue to operate normally.

---

## Shared Sporting Data

Only one Tournament structure exists.

Only one Event structure exists.

Only one Market structure exists.

Only one Outcome structure exists.

---

## Independent Betting Ecosystems

Each Group maintains:

* Independent wallets
* Independent rankings
* Independent betting history
* Independent ROI calculations

---

## Scheduler Simplicity

Football synchronization remains unchanged.

Settlement continues operating against the same shared sporting entities.

No duplication of football data is required.

---

## Future Evolution Path

If future requirements demand stronger isolation, the platform may still evolve toward:

```text
Competition
    |
    +-- CompetitionInstance
```

without invalidating GroupTournament.

This makes the proposed model a low-risk evolutionary step rather than a dead-end architecture.

---

# Current Status

This approach is currently considered the leading candidate for implementation.

Final validation is pending further analysis of:

* Wallet architecture
* Ranking architecture
* Tournament configuration requirements
* Multi-group authorization flows

Until those areas are finalized, this section should be treated as a recommended direction rather than a definitive architectural decision.


# Conclusion

The ResenhaBET multi-group architecture adopts:

* Group as the tenancy boundary
* Player as a Group-scoped entity
* Tournament as the economic boundary
* TournamentWallet replacing global Wallet
* Sporting history persisting across tournaments
* Betting history resetting every tournament

This model best reflects the social and competitive nature of the platform while enabling future features such as multi-group support, seasonal betting rankings, ROI leaderboards, and long-term player legacies.