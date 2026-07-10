# ResenhaBET v2 — Context Handoff

This document summarizes the entire project history and current state
for a new conversation. Read it fully before making any decisions.

---

## What is ResenhaBET

A FIFA tournament management and sports betting platform for closed groups
of friends. No commercial intent. No house margin on bets.

**Strategic direction (updated):** the confirmed target is a single shared
deployment serving multiple independent friend groups. This is no longer just
a future concept: the backend already contains the first multi-group era
(`Group`, `GroupMember`, `GroupTournament`, active group on `Session`,
group-scoped `Player`, and `TournamentWallet`). The implementation follows
`MULTI_GROUP_BRIEFING.md`: `GroupTournament`, not `tournament.group_id`, is
the universal tenancy link between a group and a tournament.

The project is a complete rewrite of v1, which was a Node.js/Express/Socket.IO
app that stored all state in memory and a JSON file.

---

## Tech Stack

**Backend:** Java 17, Spring Boot 4.0.4, Spring Data JPA, Flyway, MapStruct, Lombok,
Spring WebSocket (STOMP), PostgreSQL (schema: `resenha`)

**Frontend:** Angular (standalone components, signals, Tailwind CSS),
Angular HttpClient, @stomp/stompjs

**Testing:** JUnit 5, Mockito, MockMvc

**Deploy target:** Oracle Cloud Always Free (ARM VM) for backend,
Vercel for frontend, docker-compose for orchestration. Local backend currently
uses PostgreSQL at `localhost:5432/resenhaapi` and `spring.flyway.target=99`.

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
Plays FIFA matches in tournaments. **Now group-scoped.** Fields: `id`,
`name`, `active`, `currentElo` (DECIMAL default 1000), `user` (nullable FK to
User), `group` (required FK to `Group`). Player can exist without User. User
can exist without Player. The same real person can have separate Player/Elo
records in different groups.

### User
Has app account, can place bets. Fields: `id`, `name`, `pinHash` (nullable),
`userType` (ADMIN/USER), `firstLogin`.

### Session
Login creates a UUID session token stored in DB with 24h TTL.
Frontend sends `Authorization: Bearer {uuid}`. `SessionFilter` validates it
and sets `CurrentUserContext` with both the authenticated `User` and the
session's active `Group` (`session.current_group_id`). Users with multiple
groups must switch active context via `POST /api/v1/groups/{id}/switch`.

### Group
Friend community / tenant root. Fields: `id`, `name` (unique), `active`,
`createdAt`. All group-scoped business operations require an active group.

### GroupMember
Relationship between `User` and `Group`. Fields: `id`, `group`, `user`,
`role` (`OWNER` / `ADMIN` / `MEMBER`), `playerClaimed`, `createdAt`. Unique
constraint: `(group_id, user_id)`. `OWNER` and `ADMIN` can manage normal
group-scoped resources; `OWNER` is required for ownership-level changes such
as granting another owner.

### GroupTournament
Universal tenancy layer between `Group` and `Tournament`; this is the core
multi-group decision from `MULTI_GROUP_BRIEFING.md`.

Fields: `id`, `group`, `tournament`, `createdAt`, `marketTypes` (element
collection in `group_tournament_market_type`). Unique constraint:
`(group_id, tournament_id)`.

Cardinality by tournament type:
- `FIFA_MATCH`: exactly one `GroupTournament` per Tournament, enforced in
  service code (`TournamentServiceImpl.attachCurrentGroupWithMarketTypes`)
- `REAL_FOOTBALL`: one Tournament can have many `GroupTournament`s, so multiple
  groups can share the same synced sporting truth while keeping isolated
  economies and bets

`Tournament` has no direct `group_id`.

### Wallet
Legacy global wallet table/entity still exists (`Wallet`) but the current
betting economy uses `TournamentWallet` instead. Do not build new betting
features against the global wallet unless deliberately doing legacy cleanup.

### TournamentWallet
Current economic wallet. One per `User` per `GroupTournament`.
Fields: `id`, `groupTournament`, `user`, `balance`, `initialBalance`,
`createdAt`. Unique constraint: `(group_tournament_id, user_id)`. Provisioned
when a group attaches to a tournament and when a member joins a group with
existing tournaments.

### Transaction
Types: DEPOSIT, WITHDRAWAL, BET_PLACED, BET_WON, BET_REFUND. Current flow
references `TournamentWallet` via `tournament_wallet_id`; legacy `wallet_id`
still exists and is nullable. Has nullable `betSlipId` FK for bet traceability:
transaction → bet_slip → bet_slip_item → event → tournament.

### Tournament
Fields: `id`, `name`, `type` (FIFA_MATCH / REAL_FOOTBALL), `format`
(LEAGUE/BRACKET/LEAGUE_BRACKET), `status` (CREATED→IN_PROGRESS→COMPLETED/CANCELLED),
`generationMode` (AUTO/MANUAL), `hasThirdPlaceMatch`, `numberOfGroups`,
`playersAdvancingPerGroup`, `competitionId` (nullable FK to `Competition` —
NULL for FIFA_MATCH, required for REAL_FOOTBALL; see `Competition` below).

Creation now reads `TournamentRequestDTO.type`, `competitionId`, and
`marketTypes`. For `REAL_FOOTBALL`, `competitionId` is required. If a
`REAL_FOOTBALL` Tournament already exists for the competition, the current
group is attached to that shared tournament through `GroupTournament` instead
of creating duplicate sporting data. Creating new shared `REAL_FOOTBALL`
infrastructure requires system admin (`userType == ADMIN`).

### Competition
Catalog of real-world competitions, decoupled from `Tournament` so multiple
real-world competitions can be synced concurrently without editing properties
or redeploying.

Fields: `id`, `uuid`, `name` (e.g. "Copa do Mundo 2026"), `season` (e.g.
"2026"), `apiFootballLeagueId`, `apiFootballCountryId`, `gameForecastLeagueId`,
`startDate`, `endDate`, `active` (boolean), `createdAt`.

Unique constraint: `(apiFootballLeagueId, apiFootballCountryId, season)`.
Confirmed global under the multi-group model. It is shared catalog data, not
group-scoped. One `Competition` can back a shared `REAL_FOOTBALL` Tournament
that many groups attach to via `GroupTournament`.

### TournamentRound
Fields: `id`, `tournamentId`, `name`, `multiplier`, `roundOrder`, `phaseType`
(GROUP_STAGE/KNOCKOUT), `groupNumber` (nullable, for LEAGUE_BRACKET groups).

For REAL_FOOTBALL group-stage rounds, `name` holds the group label (e.g.
"Group A") resolved via the standings-based algorithm described in
REAL_FOOTBALL Integration → Fixture sync round/group resolution.

### TournamentPlayer
Full entity (not simple join table). Links player to tournament.
Has optional `teamId` FK and `groupNumber` (which group in LEAGUE_BRACKET).

### Team
FIFA team used by player, OR a real-world national/club team for
REAL_FOOTBALL events. Fields: `id`, `name`, `abbreviation`, `badgeUrl`,
`apiFootballTeamId` (nullable — renamed from `externalApiId`),
`gameForecastTeamId` (nullable, **String** — not numeric; provider ids are
opaque identifiers, treating one as `Long` caused a real type-mismatch bug
this session, see REAL_FOOTBALL Integration), `country`, `league`. Badge
entered manually.

Two independent external-id slots because APIfootball and GameForecastAPI
use unrelated ID spaces for the same real-world team — same reasoning as
`Competition` having separate `apiFootballLeagueId`/`gameForecastLeagueId`.
`gameForecastTeamId` is resolved lazily via a self-healing cache rather than
populated up front (see REAL_FOOTBALL Integration → Cross-provider team
matching).

Confirmed global under the multi-group model. Do not duplicate Team rows per
group.

### Event
Single match. Status: CREATED→IN_PROGRESS→PENALTIES→COMPLETED, or CANCELLED.
Fields: `id`, `tournamentId`, `roundId`, `playerHomeId`, `playerAwayId`
(FIFA_MATCH), `teamHomeId`, `teamAwayId` (nullable FKs to `Team` —
REAL_FOOTBALL), `externalMatchId` (nullable — stores APIfootball's
`match_id`, REAL_FOOTBALL), `gameDatetime` (column type `TIMESTAMP`, not
`TIMESTAMPTZ` — see REAL_FOOTBALL Integration → Timezone handling for a real
bug this caused), `status`, `homeScore`, `awayScore`, `isKnockout`, `isBye`,
`homeEloBefore`, `awayEloBefore`, `penaltiesHome` (nullable), `penaltiesAway`
(nullable), `nextRoundEvent` (self-ref FK nullable),
`homeSourceEvent` (self-ref FK nullable — which prior event feeds home slot),
`awaySourceEvent` (self-ref FK nullable — which prior event feeds away slot),
`isThirdPlaceMatch` (boolean — advancing player is loser, not winner).

### Market
One per Event per market type. Auto-created at event creation.
Status: OPEN→SUSPENDED (manual admin action or scheduled)→CLOSED, or CANCELLED.
Admin can manually override via `POST /api/v1/markets/{eventId}/status`.

Current `MarketType` enum: `MATCH_RESULT`, `OVER_UNDER_25`, `OVER_UNDER_35`,
`BTTS`, `EXACT_SCORE`, `QUALIFY`. Unique constraint is effectively per event
and market type. Which market types are enabled is now stored on
`GroupTournament.marketTypes`, making market selection group-tournament scoped.

### Outcome
Three per market for MATCH_RESULT: "Vitória Casa", "Empate", "Vitória Fora".
For knockout events: only two outcomes (no draw). Odd calculated by OddsCalculator.

### BetSlip
Container for one or more bet selections.
Fields: `id`, `userId`, `groupTournamentId`, `stake`, `combinedOdd`,
`potentialReturn`, `status` (PENDING/WON/LOST/CANCELLED), `createdAt`.
The old direct `tournament_id` column is nullable/legacy; current code uses
`group_tournament_id`.

### BetSlipItem
One per outcome selection within a slip.
Fields: `id`, `betSlipId`, `eventId`, `outcomeId`, `oddSnapshot`, `status`.
Current validation prevents duplicate selections for the same market within a
slip. Migration `V42` drops the older `UNIQUE(betSlipId, eventId)` constraint
so a slip can contain different markets from the same event.

---

## Authentication

No JWT, no Spring Security filter chain. Simple session-based auth.

Login flow:
- Login screen shows all Users as selectable cards
- pinHash NOT NULL → ask for 4-digit PIN → validate SHA-256 hash
- pinHash NULL + firstLogin → ask to set PIN (skippable)
- pinHash NULL + not firstLogin → log in directly
- Success → UUID session token in DB (24h TTL), with optional active group
  restored from `Session.currentGroup`

Group context flow:
- User can belong to many groups via `GroupMember`
- `POST /api/v1/groups/{id}/switch` stores the active group on the current
  session and updates `CurrentUserContext`
- Most business endpoints require both an authenticated user and an active
  group; `CurrentUserContext.getRequiredGroup()` throws if none is selected

PIN storage: SHA-256 with per-user salt. Not plain text, not BCrypt.
Admin is seeded by `PlayerInitializer` on first startup if no admin exists
(name from `resenhabet.admin.name`).
PIN reset: `PATCH /api/v1/users/{id}/reset-pin` (admin only).

---

## Authorization Model

Two independent authority concepts exist now:

- `User.userType` (`ADMIN` / `USER`) is system-wide. It is required for
  shared-infrastructure actions: create/update `Competition`, sync
  REAL_FOOTBALL fixtures/odds, mutate shared REAL_FOOTBALL markets, and decide
  shared REAL_FOOTBALL event results.
- `GroupMember.role` (`OWNER` / `ADMIN` / `MEMBER`) is group-scoped. `OWNER`
  and `ADMIN` can manage resources inside the active group; `OWNER` is needed
  for owner-level membership operations.

Tenancy access to tournaments is type-agnostic:
```java
groupTournamentRepository.existsByTournamentIdAndGroupId(tournamentId, currentGroupId)
```

Do not branch authorization on `Tournament.type` to decide visibility. Use
`GroupTournament` for visibility, then apply `userType` only when the action's
blast radius crosses group boundaries.

---

## Odds Calculation

See ODDS_CALCULATION.md for full spec. Summary:

Static odds, calculated once at event creation. No house margin.

Five steps:
1. Base Elo probabilities using configurable scale (default 400)
2. Draw probability via draw_factor (current property value 0.28)
3. H2H blend weighted by sample size (max 20% influence, scales up to 10 matches)
4. Renormalize to sum 1.0
5. Convert to decimal odds + minimum odd guard (1.05)

**Current configuration in `application.properties`:** `elo-scale=400` and
`draw-factor=0.28`.

Elo update after each COMPLETED event:
`K = 32 × round.multiplier`
`new_elo = current_elo + K × (actual - expected)`
where expected = `1 / (1 + 10^((elo_opp - elo_player) / elo-scale))`

Configuration properties:
```properties
resenhabet.odds.elo-scale=400
resenhabet.odds.draw-factor=0.28
resenhabet.odds.max-h2h-weight=0.20
resenhabet.odds.min-odd=1.05
resenhabet.odds.h2h-match-limit=10
resenhabet.odds.avg-goals-per-side=2.5
resenhabet.odds.elo-lambda-alpha=0.40
resenhabet.odds.hist-lambda-threshold=10
resenhabet.odds.max-hist-lambda-weight=0.40
resenhabet.odds.exact-score-top-n=8
```

Note: REAL_FOOTBALL `MATCH_RESULT` odds come from GameForecastAPI's Poisson
model via the odds-import pipeline, not from `OddsCalculator` — the Elo-based
calculator remains untouched and FIFA_MATCH-only.

---

## Market Lifecycle

```
Event created → Market OPEN (bets accepted)
Admin calls startEvent → Market CLOSED (via EventService hook)
Admin calls finishEvent → bets resolved, Elo updated, Market CLOSED
```

`RealFootballScheduler` is implemented but disabled by default
(`resenhabet.scheduler.live-poll-enabled=false`). When enabled, it runs on
`resenhabet.scheduler.live-poll-fixed-delay-ms`, auto-starts due REAL_FOOTBALL
events, closes markets through `EventMarketsCloseRequestedEvent`, polls live
scores, completes finished events, and cancels/refunds cancelled events.
FIFA_MATCH markets still primarily close through manual/admin event lifecycle.

For REAL_FOOTBALL, scheduler correctness depends on `event.gameDatetime` being
timezone-correct — this was broken (APIfootball calls were missing an explicit
`timezone` parameter) and has been fixed. See REAL_FOOTBALL Integration →
Timezone handling.

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
Frontend selector parametrized by `GET /api/v1/tournaments/tournament-group-config?playerCount={n}`.
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

## REAL_FOOTBALL Integration

ResenhaBET supports a second tournament type, `REAL_FOOTBALL`, which syncs
real-world matches (e.g., Copa do Mundo) from two external providers instead
of using FIFA in-game results entered by an admin.

### Data providers

**APIfootball** — fixtures, live scores, standings.
Query-parameter style endpoints (`?action=get_events`, `?action=get_standings`),
authenticates via `&APIkey=`. **Always send `&timezone=America/Fortaleza`
explicitly on every call** — see Timezone handling below for why this is
critical.

**GameForecastAPI** — match predictions and odds (Poisson-model exact-score
probabilities the Elo-based `OddsCalculator` cannot replicate). RapidAPI-style
auth headers, `GET /events?league_id=...`. Free tier: **10 requests/day** —
treat every call as expensive. See External API Logging & Replay below for
how to avoid burning quota during development. No `timezone` query param
exists for this provider — all timestamps come back as UTC ISO-8601
(`start_at`), which must be converted explicitly in application code.

### Multi-competition support — `Competition` entity

Originally, `league_id`/`country_id` for the active real-world competition
were hardcoded in `application.properties`, decoupled from `Tournament.name`
(free text). This meant only one real-world competition could be synced at a
time, and adding a new one required editing properties and redeploying.

The `Competition` entity (see Domain Model above) solves this: the admin
selects a `Competition` from a catalog at tournament-creation time, and the
correct `league_id`/`country_id` for both providers flow automatically from
there. Adding a new competition becomes a `POST /api/v1/competitions` call —
no redeploy.

**Backend status:** implemented. Migrations V33/V34 create `competition` and
`tournament.competition_id`; `CompetitionRepository`, `CompetitionServiceImpl`,
`CompetitionController`, DTOs, and mapper exist. `TournamentRequestDTO` has
`type`, `marketTypes`, and `competitionId`. `TournamentServiceImpl.create()`
validates `competitionId` for REAL_FOOTBALL, rejects it for FIFA_MATCH, and
uses `GroupTournament` to attach the active group.

`FixtureSyncServiceImpl` and `OddsImportServiceImpl` read league/country ids
from `tournament.getCompetition()`. Provider properties keep key/base-url and
timeouts.

**Still future:** a `get_leagues`-backed discovery
endpoint so admins pick competitions from a searchable list instead of
typing league/country IDs manually. `gameForecastLeagueId` still needs
manual mapping even in Fase B — no automatic correlation exists between the
two providers' league IDs.

Full design rationale, schema, and code-level refactor plan:
`COMPETITION_MODEL_DESIGN.md`.

Multi-group decision resolved by `MULTI_GROUP_BRIEFING.md`: `Competition` is
global/shared catalog data. Group-specific participation in a real-world
competition is represented by `GroupTournament`, not by duplicating
`Competition`, `Tournament`, `Event`, `Market`, or `Outcome`.

### Fixture sync — round/group resolution

`FixtureSyncServiceImpl` creates `Team`, `Event`, and `TournamentRound` rows
from APIfootball's `get_events()` response.

**Problem discovered this session:** for competitions with a group stage
(Copa do Mundo), `get_events()` returns BOTH `match_round` and `stage_name`
as the same generic value (`"World Championship"`) for every match — the
group letter is not present in event-level data at all. The original plan
("swap priority between `match_round` and `stage_name`") doesn't work,
because neither field is granular enough for this competition type.

**Resolution:** `get_standings()` returns the correct group per TEAM
(`"league_round": "Group A"`, `"stage_name": "Group Stage"`), but it's a
per-team view, not per-match. Fix: build a `Map<externalTeamId, groupName>`
from `get_standings()` once per sync run (`ApiFootballClient.getStandings(leagueId)`
→ `List<StandingEntry>` with `teamId` + `leagueRound`), then use **group
equality between the home and away team** as the signal for "is this a
group-stage match" — NOT any field from `get_events`. Rationale: a team's
group assignment is fixed for the whole competition, and by tournament
design two teams only share a group during the group stage (knockout
fixtures always cross groups).

```java
String homeGroup = teamGroupMap.get(match.getMatchHometeamId());
String awayGroup = teamGroupMap.get(match.getMatchAwayteamId());
boolean isGroupStageMatch = homeGroup != null && homeGroup.equals(awayGroup);

if (isGroupStageMatch) {
    roundName = homeGroup;               // e.g. "Group A"
    phaseType = PhaseType.GROUP_STAGE;
} else {
    // fallback — also used when standings aren't published yet
    roundName = match.getMatchRound() != null ? match.getMatchRound() : match.getStageName();
    phaseType = PhaseType.KNOCKOUT;
}
```

`determinePhaseType()` (string-parsing) is bypassed for matches resolved via
the standings map — phase is known directly from the boolean above, and only
used in the fallback branch. `roundRepository.findByTournamentIdAndName`
reuse logic is unaffected — group names work as round names like any other
string.

**Known unresolved limitation:** once the competition reaches the knockout
phase, matches fall into the fallback branch, which still resolves to the
same generic `"World Championship"` string observed in group-stage data —
meaning knockout fixtures may currently collapse into a single round name
instead of "Round of 16", "Quarterfinals", etc. The actual `get_events()`
payload for knockout fixtures has not been observed yet — no knockout
matches exist in the live data as of this session (the competition is still
in the group stage). **Must be re-validated against real knockout data
before assuming this is solved.**

Status: implemented in `FixtureSyncServiceImpl`; still re-validate knockout
round naming once real knockout payloads are available.

### Cross-provider team matching — self-healing ID cache

**Problem:** odds import (`OddsImportServiceImpl`) needs to match
GameForecastAPI's team data against the local `Team` (created from
APIfootball). The two providers use independent, unrelated team ID spaces.
Matching by name alone is fragile (country-name divergence: `"Ivory Coast"`
vs `"Côte d'Ivoire"`, `"South Korea"` vs `"Korea Republic"`, `"USA"` vs
`"United States"`, etc.) and was the root cause of several "odds not
updating" symptoms investigated this session.

**Fix:** `Team.gameForecastTeamId` (see Domain Model) is resolved
**self-healingly**: match by cached ID first; if absent, fall back to a
name match and persist the discovered ID so future syncs never need to
re-match that team. This turns a recurring silent failure into a one-time,
visible warning per team.

```java
private boolean matchAndCacheTeam(Team team, String forecastTeamId, String forecastTeamName) {
    String cachedId = team.getGameForecastTeamId();

    if (cachedId != null) {
        return cachedId.equals(forecastTeamId);
    }

    boolean nameMatch = forecastTeamName != null && team.getName().equalsIgnoreCase(forecastTeamName);

    if (nameMatch && forecastTeamId != null) {
        team.setGameForecastTeamId(forecastTeamId);
        teamRepository.save(team);
    } else if (!nameMatch) {
        log.warn("No GameForecastAPI match for team '{}' (id={}). "
                + "Set manually via PATCH /api/v1/teams/{}/game-forecast-id",
                team.getName(), team.getId(), team.getId());
    }

    return nameMatch;
}
```

Bugs found and fixed in the first draft of this logic (worth remembering if
similar matching code is written elsewhere): missing null-check before
`.equals()` on the cached id (NPE on every first-time resolution — exactly
the case the self-healing was meant to handle); comparing `Long` to `String`
(provider ids must be typed as `String`, never numeric — see Domain Model →
Team); the discovered id being written to the wrong field
(`apiFootballTeamId` instead of `gameForecastTeamId` — a real data-corruption
risk if it had reached production); a log statement referencing the home
team's data while describing an away-team mismatch; and a warning that fired
on every sync for teams that were already correctly cached, not just on
genuine mismatches.

**Recommended companions:**
- `PATCH /api/v1/teams/{id}/game-forecast-id` admin endpoint is implemented
  for manual correction when name-matching fails
- Small `KNOWN_ALIASES` normalization map (accent/case folding + known
  country-name divergences) is still recommended to reduce how often manual
  fallback is needed

**Known open case:** `Ivory Coast vs Ecuador` — confirmed NOT a timezone
issue (ruled out via the diagnostic split described below). Most likely the
alias problem described above. Needs the actual GameForecastAPI payload for
this fixture to confirm the exact name divergence before fixing.

### Timezone handling — bug found and fixed this session

**Resolved:** the REAL_FOOTBALL odds-import issue that produced "No matching event found" errors has been fixed after storing both APIfootball and GameForecastAPI match IDs and normalizing timestamps to the same timezone., even after team-ID matching was independently
confirmed correct via cross-validation (e.g. Canada and Qatar each matched
correctly in *other* fixtures, but `Canada vs Qatar` specifically still
failed — proving the team-matching logic wasn't the cause).

**Root cause:** `event.gameDatetime` was consistently **+2 hours** ahead of
the true UTC kickoff time — confirmed as a constant offset across 8
independent examples, not a per-match midnight rollover.
`ApiFootballClientImpl` was not sending the `timezone` query parameter on
`get_events`/live-events calls, so APIfootball's `match_time` came back
under a different assumed zone than the Postgres session timezone
(`SHOW TIMEZONE` = `America/Fortaleza`), and
`FixtureSyncServiceImpl.parseMatchDateTime()` stored it naively
(`LocalDateTime.parse`, no zone conversion) into a `game_datetime TIMESTAMP`
(not `TIMESTAMPTZ`) column.

**Fix (confirmed applied):**
- `ApiFootballClientImpl.fetchEventsByLeague()` and `fetchLiveEvents()` now
  explicitly send `&timezone=America/Fortaleza`
- `OddsImportServiceImpl.parseStartDate()` (parses GameForecastAPI's UTC
  `start_at`) already correctly converts to the same zone before comparing
  dates:
  ```java
  OffsetDateTime.parse(startAt).atZoneSameInstant(ZoneId.of("America/Fortaleza")).toLocalDateTime()
  ```
  Confirmed correct — no change needed there.

**Why this matters beyond odds-matching:** `event.gameDatetime` drives the
implemented REAL_FOOTBALL live-polling / market-closing scheduler (see Market
Lifecycle). Before this fix, such logic would have
suspended markets at the wrong wall-clock time — a real integrity risk
(bets could stay open after kickoff), not just a cosmetic odds-import
annoyance.

**Diagnostic technique worth keeping:** `findMatchingEvent()` was
instrumented to log two distinct warning types instead of one generic "no
match" message — "team match found but DATE mismatch" vs. "no team match at
all". This separation is what made the constant +2h pattern visible across
multiple fixtures at once, instead of debugging one match in isolation.
Worth keeping permanently, or re-adding quickly if a similar issue resurfaces.

### Match resolution status

The historical REAL_FOOTBALL "match not found" issue has been resolved.
All relevant APIfootball and GameForecastAPI identifiers are now stored and
cross-provider matching works correctly. The root cause was timezone
normalization inconsistencies; aligning both providers to the same timezone
resolved the issue.

### GameForecastAPI client — secondary issues found (not yet fixed)

While reviewing `GameForecastClientImpl.fetchPredictions()`:
- **Pagination off-by-one:** `hasMore = response.getData().size() == pageSize`
  triggers one wasted extra API call whenever the true last page happens to
  have exactly `pageSize` items — costly given the 10 req/day quota.
- **Silent exception swallowing mid-pagination:** a failed page (e.g. quota
  exhausted) returns whatever was fetched so far with only a `log.error`,
  no signal to the caller that the result set is incomplete.
- `include_all_history=false` semantics clarified: controls whether **past
  seasons'** historical data is included, NOT whether already-finished
  matches in the current season are included. Initially suspected as the
  cause of missing finished-match forecasts; ruled out — the real cause was
  the timezone bug above.

---

## External API Logging & Replay

To debug provider payload issues (the round-resolution and timezone bugs
above were diagnosed largely from inspecting raw responses) and to avoid
burning GameForecastAPI's 10-request/day quota while iterating locally,
every external API response is persisted to Postgres as JSONB.

### Schema

```sql
-- V36__CREATE_EXTERNAL_API_LOG_TABLE.sql
CREATE TABLE resenha.external_api_log (
    id              BIGSERIAL PRIMARY KEY,
    provider        VARCHAR(30) NOT NULL,      -- 'API_FOOTBALL' | 'GAME_FORECAST'
    endpoint        VARCHAR(100) NOT NULL,     -- 'get_events', 'get_events_live', 'get_standings', '/events'
    request_key     VARCHAR(255) NOT NULL,     -- deterministic string of the call's query params
    response_body   JSONB NOT NULL,
    status_code     INT,
    fetched_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_external_api_log_lookup
    ON resenha.external_api_log (provider, endpoint, request_key, fetched_at DESC);
```

### Design principles

- **Log the raw JSON `String` BEFORE deserializing**, not the parsed DTO.
  This captures error responses too (e.g. a `{"error": 411, ...}` payload
  that would otherwise throw during parsing and never get persisted) —
  important for debugging provider-side failures, not just successful calls.
- **Replay mode** (`resenhabet.external-api.replay-mode`, default `false`,
  only set `true` in `application-local.properties`): when enabled, clients
  check `external_api_log` for the latest response matching
  `(provider, endpoint, request_key)` before making a real call. Production
  always calls the real API (and logs it); local dev can replay without
  spending quota. One real call seeds the cache; unlimited local replays
  afterward cost nothing.

### Applied to

- `GameForecastClientImpl.fetchPredictions()` — provider=`GAME_FORECAST`,
  endpoint=`/events`
- `ApiFootballClientImpl.fetchEventsByLeague()` — endpoint=`get_events`
- `ApiFootballClientImpl.fetchLiveEvents()` — endpoint=`get_events_live`
  (kept separate from the regular fixture endpoint so cache keys/audit
  trail stay unambiguous between fixture sync and live polling)
- `ApiFootballClientImpl.getStandings()` — endpoint=`get_standings`

Request key convention: deterministic string built from the call's query
params, e.g. `"league_id=%s,country_id=%s,from=%s,to=%s"`.

Status: implemented in code (`ExternalApiLog`, repository, V36 migration, and
client integrations). `application.properties` default is
`resenhabet.external-api.replay-mode=false`.

Retention: not a concern yet at this project's scale — manual cleanup is
fine, no automated purge needed for v1.

---

## Complete REST API

```
# Auth
POST   /api/v1/auth/login
GET    /api/v1/auth/me
POST   /api/v1/auth/logout
PATCH  /api/v1/auth/pin

# Groups
POST   /api/v1/groups
GET    /api/v1/groups
GET    /api/v1/groups/{id}/members
POST   /api/v1/groups/{id}/members
POST   /api/v1/groups/{id}/claim-player
GET    /api/v1/groups/{id}/players/available
GET    /api/v1/groups/{id}/ranking
POST   /api/v1/groups/{id}/recalculate-elo
PATCH  /api/v1/groups/{id}
DELETE /api/v1/groups/{id}
DELETE /api/v1/groups/{id}/members/{userId}
PATCH  /api/v1/groups/{id}/members/{userId}/role

# Users
POST   /api/v1/users
GET    /api/v1/users
PATCH  /api/v1/users/{id}/reset-pin
GET    /api/v1/users/{id}
PATCH  /api/v1/users/{id}

# Players
POST   /api/v1/players
GET    /api/v1/players
GET    /api/v1/players/{id}
PUT    /api/v1/players/{id}
PATCH  /api/v1/players/{id}/link-user
GET    /api/v1/players/{id}/stats
GET    /api/v1/players/{id}/stats?tournamentId={id}
PATCH  /api/v1/players/{id}/active
DELETE /api/v1/players/{id}

# Teams
POST   /api/v1/teams
GET    /api/v1/teams
GET    /api/v1/teams/{id}
PATCH  /api/v1/teams/{id}/game-forecast-id

# Competitions
POST   /api/v1/competitions
GET    /api/v1/competitions
GET    /api/v1/competitions/{id}
PATCH  /api/v1/competitions/{id}

# Tournaments
POST   /api/v1/tournaments
GET    /api/v1/tournaments
POST   /api/v1/tournaments/{id}/start
POST   /api/v1/tournaments/{id}/players
GET    /api/v1/tournaments/{id}/players
POST   /api/v1/tournaments/{id}/players/{playerId}/team
GET    /api/v1/tournaments/{id}/rounds
GET    /api/v1/tournaments/{id}/scoreboard
GET    /api/v1/tournaments/{id}/ranking
POST   /api/v1/tournaments/{id}/advance-to-bracket
POST   /api/v1/tournaments/{id}/force-advance-to-bracket
GET    /api/v1/tournaments/tournament-group-config?playerCount={n}
POST   /api/v1/tournaments/{id}/sync-fixtures
POST   /api/v1/tournaments/{id}/sync-odds
PATCH  /api/v1/tournaments/{id}
POST   /api/v1/tournaments/{id}/cancel
DELETE /api/v1/tournaments/{id}

# Events
POST   /api/v1/events
GET    /api/v1/events
GET    /api/v1/events?tournamentId={id}
GET    /api/v1/events?status={status}
GET    /api/v1/events/{id}
POST   /api/v1/events/{id}/start
POST   /api/v1/events/{id}/score
POST   /api/v1/events/{id}/end
PATCH  /api/v1/events/{id}/players
PATCH  /api/v1/events/{id}/penalties
PATCH  /api/v1/events/{id}
PATCH  /api/v1/events/{id}/datetime
POST   /api/v1/events/{id}/cancel
DELETE /api/v1/events/{id}

# Markets
GET    /api/v1/markets/{eventId}
POST   /api/v1/markets/{eventId}/status

# Bets
POST   /api/v1/bets
GET    /api/v1/bets/me
GET    /api/v1/bets?eventId={id}

# Wallet
GET    /api/v1/wallet?userId={id}&tournamentId={id}
POST   /api/v1/wallet/deposit
POST   /api/v1/wallet/deposit-all?tournamentId={id}&amount={value}
```

---

## Current Project State

### Phase 1 — Complete ✅
All matches API, controller tests, all frontend pages.

### Phase 2 — Mostly Complete / Backend Expanded

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
| Bracket Group | ✅ Complete — remaining work limited to optimization, cleanup and regression testing |
| Multi-Group Backend | 🔶 Partial/active — core entities, tenancy, group context and tournament wallets implemented |
| REAL_FOOTBALL Backend | 🔶 Advanced — Competition, fixture sync, odds import, replay logging and scheduler exist |
| Cleanup | ⏳ Not started |
| 12 — Deploy | ⏳ Not started |

REAL_FOOTBALL is tracked separately from this table — see REAL_FOOTBALL
Integration above and `REAL_FOOTBALL_TECHNICAL_OVERVIEW.md` for its current
state, blockers, and tech debt.

---

## Open Tasks

### Multi-Group Backend / Tenancy
- Frontend still needs to catch up with group listing, group switching,
  group membership management, claim-player flow, and group-scoped tournament
  creation/attachment UX.
- Decide whether to add DB-level hardening for the `FIFA_MATCH` 1:1
  `GroupTournament` rule. Current enforcement is service-layer only.
- Decide/clean legacy schema leftovers: global `wallet`, nullable
  `transaction.wallet_id`, and nullable old `bet_slip.tournament_id` still
  exist for migration/legacy reasons, while current code uses
  `TournamentWallet` and `group_tournament_id`.
- Discovery UX for shared `REAL_FOOTBALL` tournaments: attaching the active
  group to an existing competition-backed tournament is implemented, but the
  UI/product flow for discovering that shared tournament still needs design.
- Regression-test cross-group isolation: tournaments, players, bets, wallets,
  event visibility, and admin operations must not leak across active groups.

### Group 11 — Stats Frontend (2 tasks remaining)
- Stats on Global Players Page: call `GET /api/v1/players/{id}/stats` for each player,
  replace — placeholders, remove "coming soon" tooltip
- Stats on Tournament Players Tab: same using `?tournamentId={id}`

### Bracket Group
The previously identified TournamentServiceImpl bugs have been resolved. Remaining work is focused on performance improvements, code organization/refactoring, and additional validation/testing.


### REAL_FOOTBALL — Remaining Tasks
(see `REAL_FOOTBALL_TECHNICAL_OVERVIEW.md` and `COMPETITION_MODEL_DESIGN.md`
for full detail)

**Competition / REAL_FOOTBALL Backend:**
- Backend Competition rollout is implemented (`Competition` entity,
  V33/V34, repository, service, controller, mapper, DTOs).
- `TournamentRequestDTO` + `TournamentServiceImpl.create()` now read `type`,
  validate `competitionId`, reuse existing REAL_FOOTBALL tournament
  infrastructure per competition, and attach the active group through
  `GroupTournament`.
- Selected `marketTypes` are persisted on `GroupTournament` via
  `group_tournament_market_type` (V41). Keep this group-tournament scoped.
- Frontend still needs the competition dropdown / create-or-attach UX.

**Cross-provider matching:**
- Round/group resolution via `get_standings` group-equality heuristic is
  implemented; keep re-testing against live data
- Resolve the `Ivory Coast vs Ecuador` name-mismatch case specifically
  (likely `"Côte d'Ivoire"` vs `"Ivory Coast"`) and seed a `KNOWN_ALIASES`
  normalization map
- Use the implemented `PATCH /api/v1/teams/{id}/game-forecast-id` endpoint
  for manual corrections when alias matching fails
- Re-validate knockout-phase round naming once the competition actually
  reaches the knockout stage — current fallback may still produce a single
  generic round name for all knockout fixtures

**GameForecastAPI client robustness (identified, not yet fixed):**
- Off-by-one in `fetchPredictions` pagination (wastes 1 call on
  exact-multiple page boundaries — costly at 10 req/day)
- Silent exception swallowing mid-pagination (partial results returned
  without signaling the caller)

**Carried over from `REAL_FOOTBALL_TECHNICAL_OVERVIEW.md`, still open:**
- `RealFootballScheduler` is implemented but disabled by default; enable and
  validate operationally before relying on it in production
- Frontend has no sync UI (`TournamentsApi`/page buttons still need to call
  `POST /api/v1/tournaments/{id}/sync-fixtures` and `/sync-odds`)
- Review remaining Team entity mappings and validations after the removal of the `Team.abbreviation` unique constraint
- Fixture sync now caches existing events by `externalMatchId` and V38 adds a
  unique partial index on `(tournament_id, external_match_id)`; still test
  idempotency under repeated sync and changed provider payloads
- No retry/circuit breaker on external API clients
- API keys still in `application.properties` instead of environment variables
- No atomic transaction across the full sync pipeline

**Done this session:**
- ✅ Timezone bug fixed — APIfootball calls now send `&timezone=America/Fortaleza`
- ✅ `findMatchingEvent` diagnostic logging split into "date mismatch" vs
  "no team match" — worth keeping permanently, not just as a one-off
- ✅ Self-healing `gameForecastTeamId` matching logic designed, with bugs in
  the first draft fixed (NPE, Long/String mismatch, wrong field being
  written, wrong team in log message, spurious warnings on cache hits)
- ✅ `external_api_log` table + replay-mode mechanism designed and wired
  into both API clients
- ✅ Multi-group backend core implemented: `Group`, `GroupMember`,
  `GroupTournament`, active group on `Session`, group-scoped `Player`,
  `TournamentWallet`, `group_tournament_market_type`, and group-aware
  tournament/wallet/bet flows

### Cleanup
- Remove unused legacy frontend components:
  AdminPanel, BetHistory, GlobalHistory, Ranking, MatchPanel, BackendStatus
- Clean ResenhaBetState: remove all local mock logic

### Group 12 — Deploy
- Fix hardcoded credentials (postgres/postgres in application.properties)
- Create Dockerfile (eclipse-temurin:21-jre)
- Create .env.example with all variables documented
- Create docker-compose.yml (Spring Boot + PostgreSQL, Flyway auto-runs)
- Create setup README.md (target: working instance in under 10 minutes)

---

## Future / Follow-up Features

### MarketScheduler / Live Polling
REAL_FOOTBALL live polling exists as `RealFootballScheduler` and is disabled by
default. Future work is operational hardening, observability, and deciding
whether a separate FIFA_MATCH/manual tournament auto-suspend job is needed.

### Dynamic Odds + Cashout
Odds recalculated as score changes. Requires `outcome_odd_history` table.
Cashout: `POST /api/v1/bets/{id}/cashout` settles early at current odds.
Recommendation: add `outcome_odd_history` table now (empty) to avoid future
migration on live bet data.

### Multi-Group Platform — **CONFIRMED AND PARTIALLY IMPLEMENTED**
The backend implements the core model from `MULTI_GROUP_BRIEFING.md`:
`Group` is the tenant, `Player` is group-scoped, `GroupTournament` is the only
Group ↔ Tournament link, `TournamentWallet` is group-tournament scoped, and
`Competition`/`Team`/REAL_FOOTBALL sporting data are global/shared. Remaining
work is mostly frontend, regression hardening, schema cleanup, and product UX
around shared tournament discovery and membership.

---

## Key Architectural Decisions

**No JWT / no Spring Security filter chain.**
Session tokens (UUID in DB, 24h TTL) are adequate for a closed group of friends.

**Static odds.**
Long-duration tournaments have results entered after the fact.
Real-time odds require someone monitoring the match live, which is not always the case.

**TournamentWallet per user per GroupTournament, not global wallet.**
This is an intentional reversal of the old single-tenant decision. Betting
economies reset per group-tournament so groups sharing a REAL_FOOTBALL
tournament keep isolated balances, rankings, and betting history. The legacy
global `Wallet` table/entity still exists but is not the current betting
source of truth.

**Player and User as separate entities.**
Spectator-only bettors (User without Player) and tournament-only participants
(Player without User) are both valid use cases.

**No house margin.**
Friends platform. Admin deposits credits manually. Probabilistic odds are
more transparent and more fun.

**elo-scale=400.**
Current `application.properties` uses `resenhabet.odds.elo-scale=400`.

**draw-factor=0.28.**
Current `application.properties` uses `resenhabet.odds.draw-factor=0.28`.
Older docs mention 0.12 in places; treat code/properties as source of truth.

**Single deployment, multiple groups.**
The confirmed model is no longer one-instance-per-group. The backend now has
active group context and group-scoped data isolation primitives. Existing
self-hosted/legacy data is represented by a seeded/default group in migration
data; further deployment and migration cleanup still need product decisions.

**GroupTournament is the tenancy boundary for Tournament visibility.**
`Tournament` has no `group_id`. Every tournament access check goes through
`GroupTournamentRepository.existsByTournamentIdAndGroupId`. `FIFA_MATCH` is
1:1 with a group by service-layer rule; `REAL_FOOTBALL` can be shared by many
groups.

**Blast radius determines authorization.**
`User.userType == ADMIN` governs system-wide/shared infrastructure actions
(`Competition`, REAL_FOOTBALL fixture/odds sync, shared REAL_FOOTBALL market
mutation/result decisions). `GroupMember.role` (`OWNER`/`ADMIN`/`MEMBER`)
governs actions contained inside the active group.

**Three self-referential FKs on Event.**
nextRoundEvent (winner advancement), homeSourceEvent and awaySourceEvent
(TBD slot labels). Only IDs exposed in DTO — never nested objects to avoid
Jackson circular serialization.

**Provider IDs live on the owning entity, never hardcoded in properties.**
`Team.apiFootballTeamId`/`gameForecastTeamId`, `Competition.apiFootballLeagueId`/
`apiFootballCountryId`/`gameForecastLeagueId`. Each external provider has an
independent, opaque ID space — treating them as interchangeable or storing
them centrally in properties caused real bugs this session (single-competition
limitation, a type-mismatch matching bug).

**Self-healing cross-provider ID cache.**
Match by name once, persist the discovered ID, never re-match that entity
again. Turns a recurring silent-failure risk into a one-time, auditable
resolution. Applied to `Team.gameForecastTeamId`; same pattern recommended
for any future cross-provider matching need.

**Raw external API responses are logged before parsing, not after.**
Captures provider-side errors and malformed payloads that would otherwise
throw during deserialization and vanish without a trace. See External API
Logging & Replay.

---

## Important Files in the Project

| File | Purpose |
|---|---|
| ODDS_CALCULATION.md | Full odds formula spec with worked examples and test scenarios |
| TOURNAMENTSERVICEIMPL_BUG_REPORT.md | 8 bugs in TournamentServiceImpl with fixes and test scenarios |
| WEBSOCKET_BRIEFING.md | WebSocket implementation spec for backend and frontend |
| RESENHABET_TASK_REGISTRY_UPDATED.md | Complete task registry with status of every group |
| REAL_FOOTBALL_TECHNICAL_OVERVIEW.md | Current implementation state, blockers, and tech debt for REAL_FOOTBALL |
| COMPETITION_MODEL_DESIGN.md | `Competition` entity design + REAL_FOOTBALL multi-competition refactor plan |
| MULTI_GROUP_BRIEFING.md | Current source of truth for multi-group architecture; supersedes `MULTI_GROUP_PLATFORM_DESIGN.md` |
| MULTI_GROUP_FRONTEND_PLAN.md | Frontend implementation plan for group context and group-scoped UX |
| MULTI_GROUP_TEST_IMPLEMENTATION_PLAN.md | Test plan for multi-group authorization/isolation behavior |
