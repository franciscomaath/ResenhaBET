# ResenhaBET v2 — Context Handoff

This document summarizes the entire project history and current state
for a new conversation. Read it fully before making any decisions.

---

## What is ResenhaBET

A private, self-hosted, open source FIFA tournament management and sports
betting platform for a closed group of friends. No commercial intent.
No house margin on bets. Each group runs their own isolated instance
against their own database — no multi-tenancy.

The project is a complete rewrite of v1, which was a Node.js/Express/Socket.IO
app that stored all state in memory and a JSON file.

---

## Tech Stack

**Backend:** Java 21, Spring Boot 3.4, Spring Data JPA, Flyway, MapStruct, Lombok,
Spring WebSocket (STOMP), PostgreSQL (schema: `resenha`)

**Frontend:** Angular (standalone components, signals, Tailwind CSS),
Angular HttpClient, @stomp/stompjs

**Testing:** JUnit 5, Mockito, MockMvc

**Deploy target:** Oracle Cloud Always Free (ARM VM) for backend,
Vercel for frontend, docker-compose for orchestration

---

## Architecture

### Backend package structure
```
com.franciscomaath.resenhaapi/
├── controller/         # REST controllers + request/response DTOs
├── domain/
│   ├── entity/         # JPA entities
│   ├── enums/          # All enums
│   ├── exception/      # Custom exceptions
│   └── repository/     # Spring Data JPA repositories
├── mapper/             # MapStruct mappers
├── service/Impl/       # Service implementations
└── config/             # CORS, WebSocket, Scheduler configs
```

### Error handling
All errors return:
```json
{
  "timestamp": "...",
  "status": 404,
  "error": "Resource Not Found",
  "message": "Player with id 99 not found",
  "fieldErrors": null
}
```
Exception → HTTP: ResourceNotFoundException→404, BusinessException→400,
InvalidStateException→409, DuplicateResourceException→409,
InsufficientFundsException→402, UnauthorizedException→401

---

## Domain Model

### Player
Plays FIFA matches in tournaments. Fields: `id`, `name`, `active`, `currentElo`
(DECIMAL default 1000), `user` (nullable FK to User).
Player can exist without User. User can exist without Player.

### User
Has app account, can place bets. Fields: `id`, `name`, `pinHash` (nullable),
`userType` (ADMIN/USER), `firstLogin`.

### Session
Login creates a UUID session token stored in DB with 24h TTL.
Frontend sends `Authorization: Bearer {uuid}`. `SessionFilter` validates it.

### Wallet
One per User, global across all tournaments.
Auto-created when User is created. Fields: `id`, `userId`, `balance`.

### Transaction
Types: DEPOSIT, BET_PLACED, BET_WON. Has nullable `betSlipId` FK for full
traceability: transaction → bet_slip → bet_slip_item → event → tournament.

### Tournament
Fields: `id`, `name`, `type` (FIFA_MATCH), `format` (LEAGUE/BRACKET/LEAGUE_BRACKET),
`status` (CREATED→IN_PROGRESS→ENDED), `generationMode` (AUTO/MANUAL),
`hasThirdPlaceMatch`, `numberOfGroups`, `playersAdvancingPerGroup`.

### TournamentRound
Fields: `id`, `tournamentId`, `name`, `multiplier`, `roundOrder`, `phaseType`
(GROUP_STAGE/KNOCKOUT), `groupNumber` (nullable, for LEAGUE_BRACKET groups).

### TournamentPlayer
Full entity (not simple join table). Links player to tournament.
Has optional `teamId` FK and `groupNumber` (which group in LEAGUE_BRACKET).

### Team
FIFA team used by player. Fields: `id`, `name`, `abbreviation`, `badgeUrl`,
`externalApiId` (nullable), `country`, `league`. Badge entered manually.

### Event
Single match. Status: CREATED→IN_PROGRESS→COMPLETED.
Fields: `id`, `tournamentId`, `roundId`, `playerHomeId`, `playerAwayId`,
`gameDatetime`, `status`, `homeScore`, `awayScore`, `isKnockout`, `isBye`,
`homeEloBefore`, `awayEloBefore`, `penaltiesHome` (nullable), `penaltiesAway`
(nullable), `nextRoundEvent` (self-ref FK nullable),
`homeSourceEvent` (self-ref FK nullable — which prior event feeds home slot),
`awaySourceEvent` (self-ref FK nullable — which prior event feeds away slot),
`isThirdPlaceMatch` (boolean — advancing player is loser, not winner).

### Market
One per Event. Auto-created at event creation.
Status: OPEN→SUSPENDED (manual admin action or scheduled)→CLOSED.
Admin can manually override via `POST /api/v1/markets/{eventId}/status`.

### Outcome
Three per market: "Vitória Casa", "Empate", "Vitória Fora".
For knockout events: only two outcomes (no draw). Odd calculated by OddsCalculator.

### BetSlip
Container for one or more bet selections.
Fields: `id`, `userId`, `tournamentId`, `stake`, `combinedOdd`, `potentialReturn`,
`status` (PENDING/WON/LOST/CANCELLED), `createdAt`.

### BetSlipItem
One per outcome selection within a slip.
Fields: `id`, `betSlipId`, `eventId`, `outcomeId`, `oddSnapshot`, `status`.
Constraint: UNIQUE(betSlipId, eventId) — no two items from same event in same slip.

---

## Authentication

No JWT, no Spring Security filter chain. Simple session-based auth.

Login flow:
- Login screen shows all Users as selectable cards
- pinHash NOT NULL → ask for 4-digit PIN → validate SHA-256 hash
- pinHash NULL + firstLogin → ask to set PIN (skippable)
- pinHash NULL + not firstLogin → log in directly
- Success → UUID session token in DB (24h TTL)

PIN storage: SHA-256 with per-user salt. Not plain text, not BCrypt.
Admin is seeded by DataInitializer on first startup (name from application.properties).
PIN reset: `PATCH /api/v1/users/{id}/reset-pin` (admin only).

---

## Odds Calculation

See ODDS_CALCULATION.md for full spec. Summary:

Static odds, calculated once at event creation. No house margin.

Five steps:
1. Base Elo probabilities using configurable scale (default 600, NOT chess 400)
2. Draw probability via draw_factor (default 0.12, NOT football 0.28)
3. H2H blend weighted by sample size (max 20% influence, scales up to 10 matches)
4. Renormalize to sum 1.0
5. Convert to decimal odds + minimum odd guard (1.05)

**Important:** `application.properties` currently has `elo-scale=400` and
`draw-factor=0.28` (wrong defaults). Must be corrected to 600 and 0.12
before deploy — this is a known open task.

Elo update after each COMPLETED event:
`K = 32 × round.multiplier`
`new_elo = current_elo + K × (actual - expected)`
where expected = `1 / (1 + 10^((elo_opp - elo_player) / elo-scale))`

Configuration properties:
```properties
resenhabet.odds.elo-scale=600
resenhabet.odds.draw-factor=0.12
resenhabet.odds.max-h2h-weight=0.20
resenhabet.odds.min-odd=1.05
resenhabet.odds.h2h-match-limit=10
```

---

## Market Lifecycle

```
Event created → Market OPEN (bets accepted)
Admin calls startEvent → Market CLOSED (via EventService hook)
Admin calls finishEvent → bets resolved, Elo updated, Market CLOSED
```

Note: a MarketScheduler (@Scheduled auto-suspend by gameDatetime) is planned
as a future feature but not yet implemented. Currently market closes when
admin manually starts the event.

---

## WebSocket

Spring WebSocket + STOMP. Frontend uses @stomp/stompjs.

Topic: `/topic/events/{id}` — broadcasts EventResponseDTO on:
- startEvent (after transaction commits)
- updateScore (after save)
- finishEvent (after full transaction commits via @TransactionalEventListener AFTER_COMMIT)

Additional topics (implemented): `/topic/markets/{eventId}`, `/topic/wallet/{userId}`

Frontend connects WebSocket only on Event page (ngOnInit), disconnects on ngOnDestroy.
Dashboard "Live Now" modal uses a plain `GET /api/v1/events?status=IN_PROGRESS`
on page load — no WebSocket on Dashboard.

---

## Tournament Formats

### LEAGUE
Round-robin (double: home and away). Auto-generates `(n-1)*2` rounds.
Standings: points (3W+1D), goal difference, goals scored, Elo as tiebreaker.

### BRACKET
Pure knockout. Seeded by currentElo descending.
Handles byes for non-power-of-2 player counts.
Supports 3rd place match (hasThirdPlaceMatch flag).
Knockout events: no draw outcome, penalties required if scores tied at end.
Winner auto-advances via nextRoundEvent chain.

### LEAGUE_BRACKET
Group stage (LEAGUE) followed by knockout phase (BRACKET).
numberOfGroups configurable. playersAdvancingPerGroup configurable.
Validation: floor(playerCount / numberOfGroups) >= 2 (no empty or single-player groups).
Frontend selector parametrized by `GET /api/v1/tournaments/group-config?playerCount={n}`.
Transition via `POST /api/v1/tournaments/{id}/advance-to-bracket` (admin only).
This endpoint requires ALL group stage events to be COMPLETED — throws
InvalidStateException otherwise (no silent auto-completion).

### generationMode
AUTO: system generates all rounds and events with players assigned.
MANUAL: system creates round structure and empty event slots; admin assigns players
via `PATCH /api/v1/events/{id}/players`.

---

## Self-referential Event fields

Three FKs on Event pointing back to Event:
- `nextRoundEvent` — where the winner of this event goes (set at generation)
- `homeSourceEvent` — which prior event supplies the home player slot
- `awaySourceEvent` — which prior event supplies the away player slot

These fields are stored as IDs in EventResponseDTO (never nested objects).
The frontend uses homeSourceEventId and awaySourceEventId to compute TBD labels:
- if sourceEvent.status = COMPLETED → show winner/loser name
- if sourceEvent has both players but not COMPLETED → show "PlayerA ou PlayerB"
- otherwise → show "Vencedor de [roundName]"
isThirdPlaceMatch = true means advancing player is the LOSER of the source event.

---

## Complete REST API

```
# Auth
POST   /api/v1/auth/login
GET    /api/v1/auth/me
POST   /api/v1/auth/logout
PATCH  /api/v1/auth/pin

# Users
POST   /api/v1/users
GET    /api/v1/users
PATCH  /api/v1/users/{id}/reset-pin

# Players
POST   /api/v1/players
GET    /api/v1/players
GET    /api/v1/players/{id}
PUT    /api/v1/players/{id}
PATCH  /api/v1/players/{id}/link-user
GET    /api/v1/players/{id}/stats
GET    /api/v1/players/{id}/stats?tournamentId={id}

# Teams
POST   /api/v1/teams
GET    /api/v1/teams
GET    /api/v1/teams/{id}

# Tournaments
POST   /api/v1/tournaments
GET    /api/v1/tournaments
POST   /api/v1/tournaments/{id}/start
POST   /api/v1/tournaments/{id}/players
GET    /api/v1/tournaments/{id}/players
PATCH  /api/v1/tournaments/{id}/players/{playerId}/team
GET    /api/v1/tournaments/{id}/rounds
GET    /api/v1/tournaments/{id}/scoreboard
POST   /api/v1/tournaments/{id}/advance-to-bracket
GET    /api/v1/tournaments/group-config?playerCount={n}

# Events
POST   /api/v1/events
GET    /api/v1/events
GET    /api/v1/events?tournamentId={id}
GET    /api/v1/events?status={status}
GET    /api/v1/events/{id}
PATCH  /api/v1/events/{id}/start
PATCH  /api/v1/events/{id}/score
PATCH  /api/v1/events/{id}/end
PATCH  /api/v1/events/{id}/players

# Markets
GET    /api/v1/markets/{eventId}
POST   /api/v1/markets/{eventId}/status

# Bets
POST   /api/v1/bets
GET    /api/v1/bets/me
GET    /api/v1/bets?eventId={id}

# Wallet
GET    /api/v1/wallet?userId={id}
POST   /api/v1/wallet/deposit
POST   /api/v1/wallet/deposit-all?amount={value}
```

---

## Current Project State

### Phase 1 — Complete ✅
All matches API, controller tests, all frontend pages.

### Phase 2 — Mostly Complete

| Group | Status |
|---|---|
| 1 — Identity Model | ✅ Complete |
| 2 — Wallet & Transactions | ✅ Complete |
| 3 — Markets & Odds | ✅ Complete |
| 4 — Betting System | ✅ Complete |
| 5 — Player Statistics | ✅ Complete |
| 6 — WebSocket STOMP | ✅ Complete |
| 7 — Backend Tests | ✅ Complete |
| 8 — Auth Frontend | ✅ Complete |
| 9 — Betting Frontend | ✅ Complete |
| 10 — WebSocket Frontend | ✅ Complete |
| 11 — Stats & Standings Frontend | 🔶 Partial — stats on Players pages missing |
| Bracket Group | 🔶 Partial — bugs identified in TournamentServiceImpl |
| Cleanup | ⏳ Not started |
| 12 — Deploy | ⏳ Not started |

---

## Open Tasks

### Group 11 — Stats Frontend (2 tasks remaining)
- Stats on Global Players Page: call `GET /api/v1/players/{id}/stats` for each player,
  replace — placeholders, remove "coming soon" tooltip
- Stats on Tournament Players Tab: same using `?tournamentId={id}`

### Bracket Bugs (see TOURNAMENTSERVICEIMPL_BUG_REPORT.md for full details)
Eight bugs identified in TournamentServiceImpl, ordered by priority:

**BUG-01 CRITICAL** — `advanceToBracket()` includes GROUP_STAGE rounds in
`standardRounds`. Fix: filter by `phaseType == KNOCKOUT && name != "3rd Place"`.

**BUG-02 CRITICAL** — KNOCKOUT `roundOrder` collides with GROUP_STAGE.
Fix: offset from `max(existing roundOrders) + 1` when creating KNOCKOUT rounds.

**BUG-03 CRITICAL** — `homeSourceEvent` and `awaySourceEvent` never set during
bracket generation. Fix: in the nextRoundEvent chaining loop, set source events
on the destination event using `j % 2` to determine home vs away slot.
3rd place event needs `homeSourceEvent = SF1, awaySourceEvent = SF2, isThirdPlaceMatch = true`.

**BUG-04 CRITICAL** — Odd-sized groups do not generate all matchups.
Fix: add null dummy player to make n even, skip null matches.
Round count formula: `effectiveSize = n % 2 == 0 ? n : n+1; rounds = (effectiveSize-1)*2`.

**BUG-05 IMPORTANT** — Bye event score always 1-0 regardless of player position.
Fix: check which slot is non-null and set score accordingly.

**BUG-06 IMPORTANT** — `autoCompleteGroupStageEvents()` silently creates fake
0x0 results, corrupting Elo and stats. Fix: throw InvalidStateException instead.
Delete the method.

**BUG-07 MINOR** — Round lists used by index are not sorted by roundOrder.
Fix: add `.sorted(Comparator.comparingInt(TournamentRound::getRoundOrder))` everywhere.

**BUG-08 MINOR** — `nextPowerOf2` and `numRounds` calculated twice in
`startTournament()`. Fix: extract to local variables and reuse.

### Cleanup
- Remove unused legacy frontend components:
  AdminPanel, BetHistory, GlobalHistory, Ranking, MatchPanel, BackendStatus
- Clean ResenhaBetState: remove all local mock logic

### Group 12 — Deploy
- Fix hardcoded credentials (postgres/postgres in application.properties)
- Fix odds defaults: elo-scale 400→600, draw-factor 0.28→0.12
- Create Dockerfile (eclipse-temurin:21-jre)
- Create .env.example with all variables documented
- Create docker-compose.yml (Spring Boot + PostgreSQL, Flyway auto-runs)
- Create setup README.md (target: working instance in under 10 minutes)

---

## Future Features (not started, documented for later)

### MarketScheduler
@Scheduled job that auto-suspends OPEN markets when event.gameDatetime passes.
Important for long-duration tournaments where admin doesn't manually start events.

### Dynamic Odds + Cashout
Odds recalculated as score changes. Requires `outcome_odd_history` table.
Cashout: `POST /api/v1/bets/{id}/cashout` settles early at current odds.
Recommendation: add `outcome_odd_history` table now (empty) to avoid future
migration on live bet data.

### Multi-Group Platform
Full multi-tenant refactor. Group entity, GroupMember entity, group-scoped wallets
and players, CurrentContext carrying groupId. Do not start until Phase 2 deployed.

---

## Key Architectural Decisions

**No JWT / no Spring Security filter chain.**
Session tokens (UUID in DB, 24h TTL) are adequate for a closed group of friends.

**Static odds.**
Long-duration tournaments have results entered after the fact.
Real-time odds require someone monitoring the match live, which is not always the case.

**Wallet global per user, not per tournament.**
Credits earned in one tournament should be usable in the next.

**Player and User as separate entities.**
Spectator-only bettors (User without Player) and tournament-only participants
(Player without User) are both valid use cases.

**No house margin.**
Friends platform. Admin deposits credits manually. Probabilistic odds are
more transparent and more fun.

**elo-scale=600 (not 400).**
FIFA among friends has more variance than chess. Higher scale compresses odds,
keeping all three outcomes interesting to bet on.

**draw-factor=0.12 (not 0.28).**
FIFA matches end in draws far less often than professional football.
Calibrate against your group's historical draw rate.

**Self-hosted, one instance per group.**
Not multi-tenant. Switching groups = spinning up a new instance with a new DB.
Docker-compose makes this simple.

**Three self-referential FKs on Event.**
nextRoundEvent (winner advancement), homeSourceEvent and awaySourceEvent
(TBD slot labels). Only IDs exposed in DTO — never nested objects to avoid
Jackson circular serialization.

---

## Important Files in the Project

| File | Purpose |
|---|---|
| ODDS_CALCULATION.md | Full odds formula spec with worked examples and test scenarios |
| TOURNAMENTSERVICEIMPL_BUG_REPORT.md | 8 bugs in TournamentServiceImpl with fixes and test scenarios |
| WEBSOCKET_BRIEFING.md | WebSocket implementation spec for backend and frontend |
| RESENHABET_TASK_REGISTRY_UPDATED.md | Complete task registry with status of every group |
