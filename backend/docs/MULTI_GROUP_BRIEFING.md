# MULTI_GROUP_BRIEFING.md

## Overview

This document consolidates the Multi-Group Platform architecture for ResenhaBET.

It supersedes `MULTI_GROUP_PLATFORM_DESIGN.md`. That document left its most important question — how Groups participate in shared real-world competitions — as "under evaluation" (Appendix A). This briefing resolves that question, confirms the model, and adds the authorization rules needed to operate it safely.

Everything in the original document that was already solid (Group as tenant, Player as group-scoped, Tournament as the economic boundary, sporting history vs betting history) is preserved here. What changes is **how** group-tenancy attaches to `Tournament`, and a new section defining **who** is allowed to perform cross-group actions.

If you are reading this without the prior conversation in context: treat this document as the current source of truth for the Multi-Group refactor. `MULTI_GROUP_PLATFORM_DESIGN.md` should be considered historical — kept for the reasoning trail in Appendix A, not as the implementation plan.

---

# Goals

## Functional Goals

Allow multiple friend groups to coexist in the same deployment.

Examples:

* Copa Gegê
* Resenha da Faculdade
* FIFA da Empresa

Each group must have:

* Independent tournaments (for FIFA_MATCH)
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

```text
GROUP = TENANT
```

Every business operation must execute within a Group context.

No entity belonging to one group may be visible or accessible from another group — **with one explicit exception**: a `Tournament` built from a real-world `Competition` (REAL_FOOTBALL) may be shared by multiple groups at once, by design. That sharing is mediated entirely through `GroupTournament` (see below) and never leaks into group-owned data (wallets, bets, rankings).

---

# High-Level Domain Model

```text
User (userType: ADMIN | USER — system-wide)
 │
 ├── Session
 │
  └── GroupMember (role: OWNER | ADMIN | MEMBER — group-scoped)
       │
       ▼
     Group
       │
       ├── Player                      (sporting history, group-scoped)
       │
       └── GroupTournament             (the tenancy boundary)
               │
               ▼
            Tournament                 (shared sporting truth)
               │
               ├── Round
               ├── Event
               ├── Market
               └── Outcome

GroupTournament
       │
       ├── TournamentWallet            (per user, per group, per tournament)
       ├── Transaction
       ├── BetSlip
       └── BetSlipItem  (via BetSlip → Event/Outcome, which are shared)
```

The key shift from the original document: `Tournament` is no longer a direct child of `Group`. It sits behind `GroupTournament`, which is the only thing that knows "which group(s) can see this tournament."

---

# Entity Classification

---

# Global Entities

Global entities are shared across the entire platform. They do not belong to any group.

## User

Represents a platform account. A User may participate in multiple Groups.

Carries `userType` (`ADMIN` | `USER`), unchanged from the current single-tenant model. This field governs **system-wide** actions and is independent of group membership — see Authorization Model below.

---

## Session

Authentication session. Belongs to a User only.

---

## Competition

Represents real-world competitions (World Cup 2026, Champions League 2026, Premier League 2026).

**Confirmed global.** This resolves the open question left in `RESENHABET_CONTEXT_HANDOFF.md` ("does Competition stay a single global catalog or become group-scoped?") — it stays global. Duplicating real-world catalog data per group provides no benefit, and `Competition` already exists independently of any single group's data.

---

## Group

Represents a friend community (Copa Gegê, FIFA da Faculdade). Acts as the tenancy root for everything group-scoped.

---

## GroupMember

Relationship entity between User and Group. Responsible for membership and group-scoped permissions.

**Confirmed field:** `role` (`OWNER` | `ADMIN` | `MEMBER`). This role governs actions **within a single group's boundary only** — it has no authority over system-wide or cross-group operations. `OWNER` is the highest group-scoped role, intended for group creators or trusted maintainers; `ADMIN` can manage group-scoped resources; `MEMBER` is a normal participant. See Authorization Model below for the full rule and rationale.

---

## Team

**Confirmed global** (this was ambiguously placed under "Group-Scoped Entities" in the original document; corrected here). Mostly catalog data — FIFA teams, national teams, club teams. No meaningful business value in duplicating teams per group.

---

# Group-Scoped Entities

## Player

Player is group-scoped. This is unchanged from the original document and remains one of the most important decisions in this refactor.

```text
Group
 │
 └── Player
         │
         └── User (optional)
```

A player's sporting history belongs to a Group. The same person can have a different Elo in different groups:

```text
Group A: Francisco, Elo = 1400
Group B: Francisco, Elo = 980
```

Both are valid simultaneously.

---

# Tenancy Layer — GroupTournament

This is the central change in this briefing relative to the original document.

## The problem with a direct `tournament.group_id`

The original document proposed, in its main migration plan:

```java
@ManyToOne
private Group group;   // direct FK on Tournament
```

Appendix A of that same document then proposed the opposite for REAL_FOOTBALL: a single `Tournament` shared by N groups, to avoid cloning sporting data (see Appendix A below for why cloning was rejected).

Taken together, those two sections left `Tournament` with **two incompatible ownership models depending on `type`** — a direct FK for `FIFA_MATCH`, a join table for `REAL_FOOTBALL`. Implemented literally, every query, every service method, and every authorization check touching `Tournament` would need to branch on `type` to know how to determine visibility:

```java
// What we are NOT doing
if (tournament.getType() == FIFA_MATCH) {
    if (!tournament.getGroup().getId().equals(currentGroupId)) throw new UnauthorizedException();
} else {
    if (!groupTournamentRepository.existsByTournamentIdAndGroupId(tournament.getId(), currentGroupId)) {
        throw new UnauthorizedException();
    }
}
```

This kind of branching is exactly where security bugs live — a missing `else` in one new endpoint is a cross-group data leak.

## The resolution: GroupTournament is universal

`Tournament` carries **no direct group reference at all**. Group ↔ Tournament is mediated exclusively through `GroupTournament`, for every tournament type, with cardinality differing by type:

* **`FIFA_MATCH`** — exactly one `GroupTournament` row per Tournament. A friend group's FIFA tournament is never shared; its Players and Events belong to that group's roster alone.
* **`REAL_FOOTBALL`** — one or more `GroupTournament` rows per Tournament. Multiple groups may participate in the same real-world `Competition`'s synced `Tournament` simultaneously.

This gives a **single, type-agnostic authorization check everywhere**:

```java
boolean hasAccess = groupTournamentRepository
    .existsByTournamentIdAndGroupId(tournamentId, currentGroupId);

if (!hasAccess) {
    throw new UnauthorizedException();
}
```

No branching, no per-type logic, no risk of someone forgetting the `else`.

## GroupTournament — fields

```java
GroupTournament

id
group_id
tournament_id
created_at
```

Unique constraint:

```sql
-- Enforced at the service layer for FIFA_MATCH (see below);
-- no DB-level constraint needed since cardinality differs by type.
```

## Enforcing the FIFA_MATCH 1:1 rule

There is no clean single SQL constraint that says "unique per tournament only when `tournament.type = FIFA_MATCH`" without a partial index or trigger. The pragmatic choice for this project's scale is a service-layer check at the moment a `GroupTournament` is created:

```java
@Transactional
public GroupTournament attachGroupToTournament(Long groupId, Long tournamentId) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
        .orElseThrow(() -> new ResourceNotFoundException("Tournament not found"));

    if (tournament.getType() == TournamentType.FIFA_MATCH) {
        boolean alreadyAttached = groupTournamentRepository.existsByTournamentId(tournamentId);
        if (alreadyAttached) {
            throw new BusinessException("FIFA_MATCH tournaments can belong to only one group.");
        }
    }

    return groupTournamentRepository.save(new GroupTournament(groupId, tournamentId));
}
```

**Open implementation detail (not yet decided):** whether to later harden this with a partial unique index at the DB level as a second line of defense. Not blocking — flagged in Open Questions below.

## Why GroupTournament matters beyond tenancy

As already noted in the original document's Appendix A, `GroupTournament` is also the natural home for **future per-group tournament settings** once they're needed:

* Initial bankroll per group
* Betting limits
* Cashout rules
* Market visibility

```text
Group A on World Cup 2026 — Initial Bankroll: 1000
Group B on World Cup 2026 — Initial Bankroll: 5000
```

No schema for this exists yet — this is an identified extension point, not a current requirement.

---

# Shared Sporting Truth

The following entities remain **singular and group-agnostic** regardless of how many groups participate in a `REAL_FOOTBALL` tournament:

```text
Tournament
Round
Event
Market
Outcome
```

These entities have no notion of "group" at all — they don't carry a `group_id`, don't need one, and the services that own them (`FixtureSyncServiceImpl`, `OddsImportServiceImpl`, `MarketSuspensionJob`, Elo update on `finishEvent`) require **zero changes** for multi-group support. This is the core efficiency win of this model: one synced `Event`, one `Market`, one set of `Outcome`s serve every participating group identically. No duplicated GameForecastAPI calls (relevant given the 10 req/day quota), no duplicated scheduler work, no duplicated settlement infrastructure.

`TournamentPlayer` is **not** part of this shared layer — it only applies to `FIFA_MATCH` tournaments, where `Event` references `Player`, not `Team`. For `REAL_FOOTBALL`, `TournamentPlayer` is not used at all.

---

# Tournament Financial Entities

These entities sit on the other side of `GroupTournament` — they are where group isolation actually happens.

## TournamentWallet

```text
Tournament
 │
 GroupTournament
 │
 TournamentWallet
 │
 User
```

```sql
UNIQUE (group_tournament_id, user_id)
```

**This corrects a real bug in the original document.** That document proposed `UNIQUE(tournament_id, user_id)`, defined before Appendix A introduced tournament sharing. Once a single `Tournament` can belong to multiple groups, `(tournament_id, user_id)` is not unique: a user in both Group A and Group B, both betting on the same shared World Cup `Tournament`, would collide on a single wallet row instead of getting two independent ones. Keying off `group_tournament_id` instead resolves this automatically, since it already encodes the unique (tournament, group) pair.

### Rationale (unchanged from original document)

ResenhaBET does not use real money. Persistent balances across tournaments create undesirable effects: wealth accumulation, new-player disadvantage, mixed historical data, difficult tournament comparisons. Tournament-scoped wallets solve this naturally — each tournament starts with a fresh economy, while sporting history (Elo) persists.

**Note:** this is a deliberate reversal of the previously locked-in "wallet is global per user" decision. That decision made sense in a single-tenant, single-economy world; it doesn't survive the move to seasonal, group-scoped betting competitions. Flagging this explicitly so it's understood as an intentional trade-off, not an oversight.

## Transaction

References `TournamentWallet` directly (the field that today points at the global `Wallet`). **No new column is needed on `Transaction`.** Group context is inherited transitively: `Transaction → TournamentWallet → group_tournament_id`. This matters because `DEPOSIT`-type transactions have no `betSlipId` at all (it's nullable) — the wallet reference, not the bet slip, is `Transaction`'s actual scoping anchor.

`betSlipId` remains for traceability on bet-related transactions specifically: `transaction → bet_slip → bet_slip_item → event → tournament`.

## BetSlip

```sql
-- Before (original document)
bet_slip.tournament_id

-- Now (confirmed)
bet_slip.group_tournament_id
```

The direct `tournament_id` field is dropped in favor of `group_tournament_id` — the underlying `tournament_id` remains reachable via a join through `GroupTournament` when needed, avoiding two FKs that would otherwise have to agree with each other.

## BetSlipItem

**Unchanged, no new column.** Still scoped by `betSlipId`, `eventId`, `outcomeId`. Group context is inherited transitively via `BetSlip`.

---

# FK Placement Summary

| Entity | New `group_tournament_id`? | Why |
|---|---|---|
| `BetSlip` | **Yes** — replaces `tournament_id` | Source of truth for "which group placed this bet" |
| `TournamentWallet` | **Yes** | Source of truth for "whose money, in which group's economy" |
| `Transaction` | No | Inherits via its wallet reference |
| `BetSlipItem` | No | Inherits via `betSlipId` |
| `Tournament` / `Round` / `Event` / `Market` / `Outcome` | No | Shared sporting truth, group-agnostic by design |

---

# Bet Settlement Under the Group-Tournament Model

## What does NOT change

`finishEvent()` — locating the `Market`, determining the winning `Outcome`, and marking `BetSlipItem`s as `WON`/`LOST` — requires no changes at all:

```java
List<BetSlipItem> items = betSlipItemRepository.findByEventIdAndStatus(eventId, PENDING);
```

This query is keyed by `eventId`, not `tournamentId` — it never had a notion of group. Because of that, it already returns the `BetSlipItem`s of **every group** that bet on this event, with zero modification. The multi-group fan-out happens for free; it's a structural consequence of `BetSlipItem` pointing at `Event` directly rather than through `Tournament`.

## What DOES change — exactly one step

The only place group context enters is wallet resolution, inside slip resolution:

```java
// Before
void resolveSlip(Long slipId) {
    BetSlip slip = betSlipRepository.findById(slipId);
    // ... mark slip WON/LOST based on its items ...
    if (slip.getStatus() == WON) {
        Wallet wallet = walletRepository.findByUserId(slip.getUserId());
        wallet.credit(slip.getPotentialReturn());
        transactionRepository.save(new Transaction(BET_WON, wallet.getId(), slip.getPotentialReturn(), slipId));
    }
}
```

```java
// After
void resolveSlip(Long slipId) {
    BetSlip slip = betSlipRepository.findById(slipId);
    // ... mark slip WON/LOST based on its items — unchanged ...
    if (slip.getStatus() == WON) {
        TournamentWallet wallet = tournamentWalletRepository
            .findByGroupTournamentIdAndUserId(slip.getGroupTournamentId(), slip.getUserId());
        wallet.credit(slip.getPotentialReturn());
        transactionRepository.save(new Transaction(BET_WON, wallet.getId(), slip.getPotentialReturn(), slipId));
    }
}
```

One line. The same substitution applies to `BET_PLACED` (debit at placement time) — the wallet lookup key changes, nothing else.

## New operational considerations introduced by fan-out

These don't change the logic above, but are worth deciding consciously before "Grupo 4" (Betting System) settlement work formally starts, since it hasn't begun yet:

**Settlement scale.** A popular shared `Event` (e.g. Brazil vs Argentina in a World Cup synced across many groups) can now produce far more `BetSlipItem`s to resolve in one `finishEvent` call than in the single-tenant model. The per-item loop with individual `save()` calls is fine at current friend-group scale; flagged as a future upgrade path to a batch `@Modifying` update if group count grows meaningfully.

**Idempotency.** `findByEventIdAndStatus(eventId, PENDING)` already provides a natural guard — already-resolved items won't reappear on a re-run. This property already existed in the single-tenant model; it matters more now because a single re-entrant call touches more independent records (across groups) at once. Keep the whole event settlement inside one `@Transactional` boundary.

**Voiding a shared market.** Because `Market` is shared, voiding it affects every participating group simultaneously. This is a governance question, not a settlement-logic question — see Authorization Model below for the confirmed rule.

---

# Authorization Model — userType vs groupRole

Confirmed this session. This is the rule that governs every action introduced by the Multi-Group refactor.

## The principle

`User.userType` (`ADMIN` | `USER`) is unchanged from the current single-tenant system. It governs actions that **affect the system as a whole** — anything whose effect is not contained within a single group's boundary.

`GroupMember.role` (`OWNER` | `ADMIN` | `MEMBER`) governs actions **scoped exclusively to one group** — it carries no authority outside that group's boundary, and specifically **does not** substitute for `userType == ADMIN` on system-wide actions.

A user can be a `groupRole == OWNER` or `groupRole == ADMIN` of Group A with control over Group A's own affairs, and simultaneously have no more system-wide authority than any other `USER`.

## Confirmed rule

Everything that creates or mutates shared infrastructure — `Competition` creation/update, market voiding, fixture/odds sync, and result decisions on shared infrastructure — requires `userType == ADMIN`. A group-scoped admin role is explicitly **not** sufficient for these, regardless of which group the user administers.

Attaching the active group to an already-existing `REAL_FOOTBALL` `Tournament` is different: it creates only a new `GroupTournament` row and that group's isolated economy. It does not create or mutate the shared `Competition`, `Tournament`, `Event`, `Market`, or `Outcome` records. That action is therefore group-scoped and may be performed by `groupRole == OWNER` or `groupRole == ADMIN` of the active group.

## Why this maps cleanly onto the GroupTournament cardinality

This rule isn't arbitrary — it follows directly from the cardinality difference established above:

* `FIFA_MATCH` tournaments have exactly one `GroupTournament` (one group). Any action on them — including entering a final score — is naturally contained to that one group. `groupRole == OWNER` or `groupRole == ADMIN` of that group is sufficient.
* `REAL_FOOTBALL` tournaments can have N `GroupTournament`s. Any action on their shared infrastructure (`Competition`, `Market`, `Event` result) ripples into every participating group's settlement at once. Only `userType == ADMIN` — a platform-wide authority — should be able to trigger that.

In short: **blast radius determines which check applies.** If an action can only ever affect one group, `groupRole` governs it. If an action can affect more than one group, `userType == ADMIN` governs it, unconditionally.

## Concrete mapping

| Action | Endpoint | Required |
|---|---|---|
| Create / update `Competition` | `POST /api/v1/competitions`, `PATCH /api/v1/competitions/{id}` | `userType == ADMIN` |
| Create a new shared `REAL_FOOTBALL` tournament infrastructure | `POST /api/v1/tournaments` with no existing tournament for the `competitionId` | `userType == ADMIN` and `groupRole == OWNER` or `groupRole == ADMIN` |
| Attach active group to an existing `REAL_FOOTBALL` tournament | `POST /api/v1/tournaments` when a tournament already exists for the `competitionId` | `groupRole == OWNER` or `groupRole == ADMIN` |
| Sync fixtures / odds for a `REAL_FOOTBALL` tournament | `POST .../sync-fixtures`, `POST .../sync-odds` | `userType == ADMIN` |
| Void / change status of a shared (`REAL_FOOTBALL`) market | `POST /api/v1/markets/{eventId}/status` | `userType == ADMIN` |
| Decide result of a shared (`REAL_FOOTBALL`) event | `PATCH /api/v1/events/{id}/end` | `userType == ADMIN` |
| Void a `CUSTOM_STAT` market on a `FIFA_MATCH` tournament | future void endpoint | `groupRole == OWNER` or `groupRole == ADMIN` of the owning group |
| Decide result of a `FIFA_MATCH` event | `PATCH /api/v1/events/{id}/end` | `groupRole == OWNER` or `groupRole == ADMIN` of the owning group |
| Manage group membership, `GroupTournament` settings | `POST /api/v1/groups/{id}/members`, future settings endpoint | `groupRole == OWNER` or `groupRole == ADMIN`; assigning `OWNER` requires `OWNER` |
| Switch active group context | `POST /api/v1/groups/{id}/switch` | Any `GroupMember` |

`OWNER` and `ADMIN` are both group-scoped administrative roles. `OWNER` exists to protect ownership-level operations, such as granting another user `OWNER`.

## Scheduled / system jobs are exempt

`MarketSuspensionJob`, the `completeEvent` automatic hook, and similar `@Scheduled` processes act without a "current user" — they are not subject to this authorization model at all. Authorization only applies to user-triggered endpoints.

---

# Security Model

Every group-scoped or tenancy-checked request now applies up to two independent checks:

```java
// 1. Tenancy — does the current group have visibility into this resource?
if (!groupTournamentRepository.existsByTournamentIdAndGroupId(tournamentId, currentGroupId)) {
    throw new UnauthorizedException();
}

// 2. Authorization — for system-wide actions only
if (requiresSystemAdmin && currentUser.getUserType() != UserType.ADMIN) {
    throw new UnauthorizedException();
}
```

Check 1 applies to: Players, Tournaments (via GroupTournament), Events, Bets, Statistics — anything read or written within a group's own data.

Check 2 applies only to the actions enumerated in the Authorization Model table above, independent of group membership.

---

# Group Context

The application must maintain an active Group context. A User may belong to multiple Groups simultaneously, so authentication alone is insufficient.

```text
Session
 ├── User
 └── Current Group
```

## Context Switching

```http
POST /api/v1/groups/{id}/switch
```

```json
{
  "groupId": 5,
  "groupName": "Copa Gegê"
}
```

The selected Group becomes the active context for subsequent requests, including which `TournamentWallet` gets debited/credited when the user places or wins a bet on a shared `Tournament`.

---

# Tournament Statistics

Unchanged from the original document, now explicitly scoped through `GroupTournament`.

## Betting Ranking

```text
1° Francisco
2° Jonas
3° Rafael
```

Based on final `TournamentWallet` balance, within one `GroupTournament`.

## ROI Ranking

```text
ROI = Profit / Initial Balance
```

## Highest Profit

```text
Final Balance - Initial Balance
```

## Most Aggressive Bettor

Based on total wagered amount.

## Hit Rate

```text
Winning Bets / Total Bets
```

---

# Sporting vs Betting History

Unchanged — this separation is the social core of the whole refactor.

## Sporting History

Group-scoped. Persists across tournaments. Elo, wins, losses, goals, championships.

## Betting History

Now `GroupTournament`-scoped (was tournament-scoped in the original document; the distinction matters once a tournament can be shared). Resets every tournament. Balance, profit, ROI, betting rankings.

Players build long-term sporting legacies. Betting competitions restart every season.

---

# Migration Strategy

Revised sequencing relative to the original document, given the GroupTournament correction.

## Phase 1 — Infrastructure

Create `Group`, `GroupMember` (including the `role` field). Implement group creation, joining, listing, switching. No business logic changes. **No dependencies — can start immediately.**

## Phase 2 — Tournament Tenancy via GroupTournament (revised)

This phase **replaces** the original plan's `tournament.group_id` direct FK. Implement `GroupTournament` as the sole tenancy mechanism, including the `FIFA_MATCH` 1:1 service-layer rule. All tournament queries become group-aware exclusively through this join — never through a column on `Tournament` itself.

Because neither this phase nor Phase 4 had been implemented yet at the time of this correction, there is no migration-of-a-migration involved — this is a plan correction, not a schema rollback.

## Phase 3 — Player Isolation

Add `player.group_id`. Move sporting statistics into Group scope. Independent of Phase 2 — can run in parallel.

## Phase 4 — Tournament Economy (revised — depends on Phase 2)

Replace `Wallet` with `TournamentWallet`, keyed by `group_tournament_id`. Migrate `Transaction`, `BetSlip` (`tournament_id` → `group_tournament_id`), `BetSlipItem` (no schema change). The multi-group bet settlement design described above should be finalized as part of this phase, before "Grupo 4" (Betting System) work formally begins.

## Phase 5 — Authorization Hardening (revised)

Introduce `CurrentGroupContext`. Implement the dual-check model from the Authorization Model section above — both the tenancy check (via GroupTournament) and the `userType == ADMIN` check for system-wide actions, applied consistently across services.

---

# Final Architecture

```text
GLOBAL

User (userType: ADMIN | USER)
Session
Competition
Group
GroupMember (role: OWNER | ADMIN | MEMBER)
Team

--------------------------------

GROUP-SCOPED

Player

--------------------------------

TENANCY LAYER

GroupTournament
  — 1:1 with Tournament for FIFA_MATCH
  — N:1 with Tournament for REAL_FOOTBALL

--------------------------------

SHARED SPORTING TRUTH (group-agnostic)

Tournament
Round
Event
Market
Outcome

--------------------------------

GROUP-TOURNAMENT-SCOPED (via GroupTournament)

TournamentPlayer        — FIFA_MATCH only
TournamentWallet
Transaction
BetSlip
BetSlipItem
Tournament Statistics
Betting Rankings
```

---

# Appendix A — History: Why Cloning and CompetitionInstance Were Rejected

Condensed from the original document's Appendix A. Preserved for institutional memory — this reasoning is what led to the `GroupTournament` model above, and shouldn't be re-litigated without revisiting these numbers.

**Status: RESOLVED.** Superseded by the `GroupTournament` universal model documented in the main body of this briefing.

## Tournament Cloning — rejected

Considered model: each participating Group gets its own cloned `Tournament`, `Event`, `Market`, `Outcome` set.

For a World Cup with 64 matches and 100 participating groups, this produces 6,400 `Event` rows instead of 64. The same multiplier hits `Market`, `Outcome`, and every scheduler/sync job, which would need to update every clone individually. Beyond the row-count problem, this would multiply GameForecastAPI odds-import calls by the number of groups — directly incompatible with that provider's 10 request/day quota.

## CompetitionInstance — rejected (for now)

Considered model: a fully normalized `Competition → CompetitionInstance → Events/Markets/Outcomes` hierarchy, decoupling shared sporting data from `Tournament` entirely.

Architecturally cleaner on paper, but would require rewriting the scheduler, settlement, market generation, event synchronization, APIs, and frontend flows that are already built and working — for a scalability ceiling (hundreds of independent tenants) explicitly outside this project's stated non-goals (not a SaaS platform). Noted as a possible future evolution path if requirements ever change, but not pursued now.

## GroupTournament — accepted, now confirmed

The middle ground: keep `Tournament`/`Event`/`Market`/`Outcome` as the existing, already-working shared entities; isolate only the betting/financial layer through a lightweight join entity. This is what the rest of this briefing documents as the confirmed model.

---

# Open Questions

Not yet decided — flagged so they aren't silently assumed during implementation.

* **DB-level enforcement of the FIFA_MATCH 1:1 rule.** Currently service-layer only; whether to add a partial unique index as a second line of defense is undecided.
* **Discovery UX for joining a shared Tournament.** Authorization is decided: attaching the active group to an existing `REAL_FOOTBALL` tournament is group-scoped and allowed for `OWNER` or `ADMIN`. The remaining open UX question is how Group B discovers that Group A already started the competition before submitting the create/attach request.
* **Settlement batch-update strategy.** Not urgent at current scale; flagged as a future upgrade once group count grows (see Bet Settlement section).
* **`GroupTournament` settings schema.** Initial bankroll, betting limits, etc. — the extension point is identified, the schema is not designed.

---

# Conclusion

The ResenhaBET Multi-Group architecture, as confirmed in this briefing, adopts:

* Group as the tenancy boundary
* Player as a Group-scoped entity
* `GroupTournament` — not a direct FK — as the **sole** mechanism connecting Group to Tournament, with cardinality differing by tournament type (1:1 for FIFA_MATCH, N:1 for REAL_FOOTBALL)
* Tournament, Round, Event, Market, and Outcome remaining shared, group-agnostic sporting truth — no rewrite of existing REAL_FOOTBALL infrastructure required
* TournamentWallet, Transaction, BetSlip, and BetSlipItem as the isolated betting/financial layer, scoped through `group_tournament_id`
* Bet settlement requiring no change to event/market resolution logic — only wallet lookup changes, and multi-group fan-out happens structurally for free
* `User.userType` governing system-wide actions (Competition, shared-market voiding, shared-event results) and `GroupMember.role` governing group-scoped actions only, with blast radius as the deciding principle between the two
* Sporting history persisting across tournaments; betting history resetting every tournament

This model resolves the tenancy contradiction left open in the original design document while preserving everything that already works in the REAL_FOOTBALL integration, and gives the platform one consistent authorization pattern instead of type-based branching.
