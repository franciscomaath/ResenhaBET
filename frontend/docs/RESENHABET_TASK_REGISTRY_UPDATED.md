# ResenhaBET v2 — Updated Task Registry

> Last updated: June 13 2026 (Frontend Phase 7 complete, pending cleanup + Group 11 stats)
> Legend: ✅ Done | ⏳ Not Started | 🔶 Partially Done | 🔄 Changed from original plan | 🆕 Added

---

## PHASE 1 — Matches API

### ✅ Backend — All Complete

- Create Player entity, repository, service, controller (CRUD)
- Create Team entity, repository, service, controller (CRUD)
- Create Tournament entity with TournamentFormat enum (LEAGUE, BRACKET)
- Create TournamentRound entity with multiplier and roundOrder
- Create TournamentPlayer entity (promoted from join table, with optional team FK)
- Create Event entity with status lifecycle (CREATED → IN_PROGRESS → COMPLETED)
- Flyway migrations V1–V14 for all Phase 1 tables
- POST /api/v1/players
- GET /api/v1/players
- GET /api/v1/players/{id}
- PUT /api/v1/players/{id}
- POST /api/v1/teams
- GET /api/v1/teams
- GET /api/v1/teams/{id}
- POST /api/v1/tournaments
- GET /api/v1/tournaments
- POST /api/v1/tournaments/{id}/start (auto-generates rounds for LEAGUE format)
- POST /api/v1/tournaments/{id}/players
- GET /api/v1/tournaments/{id}/players
- PATCH /api/v1/tournaments/{id}/players/{playerId}/team
- GET /api/v1/tournaments/{id}/rounds
- POST /api/v1/events
- GET /api/v1/events
- GET /api/v1/events?tournamentId={id}
- PATCH /api/v1/events/{id}/start
- PATCH /api/v1/events/{id}/score
- PATCH /api/v1/events/{id}/end
- GlobalExceptionHandler with all custom exceptions
- Controller tests for Player, Team, Tournament, Event

### ✅ Frontend — All Complete

- Angular project setup (standalone components, signals, Tailwind CSS)
- api.models.ts with all Phase 1 TypeScript DTOs
- PlayersApi, TournamentsApi, EventsApi HTTP services
- Player selection login screen (cards, initials avatar, Francisco hardcoded admin)
- Dashboard page (tournament cards, status badges, create modal, empty/loading/error)
- Tournament page (matches tab grouped by round, players tab, leaderboard placeholder,
  start tournament, add player, create match modal, team badge display, assign team UI)
- Event page (match header, live score via polling, start/score/end admin controls,
  completed state with final scoreboard)
- Global players page (list, create, edit, activate/deactivate)
- CORS configured for Angular dev server

---

## PHASE 2 — Betting Platform

### Group 1 — Identity Model ✅ Complete

- Migration V16: user_id FK on player (nullable, unique)
- Migration V17: alter user table (pin_hash, user_type, first_login, remove email)
- Migration V18: create session table (token UUID, expires_at)
- Update Player entity: ManyToOne User (nullable)
- Update User entity: pinHash, UserType enum (ADMIN/USER), firstLogin
- Create PinService (SHA-256 + per-user salt, not BCrypt)
- Create SessionFilter (validates Bearer UUID token, populates user context)
- POST /api/v1/auth/login
- GET /api/v1/auth/me
- POST /api/v1/auth/logout
- PATCH /api/v1/auth/pin
- PATCH /api/v1/users/{id}/reset-pin (admin only)
- POST /api/v1/users (self-registration, auto-creates wallet)
- GET /api/v1/users
- PATCH /api/v1/players/{id}/link-user (admin only)
- DataInitializer: seed admin user on first startup

### Group 2 — Wallet and Transactions ✅ Complete

- Wallet auto-created on POST /api/v1/users
- GET /api/v1/wallet?userId={id}
- POST /api/v1/wallet/deposit (admin only, { userId, amount })
- POST /api/v1/wallet/deposit-all (admin only, ?amount= query param) 🔄 uses query param not body
- bet_slip_id FK added to transaction table

### Group 3 — Markets and Odds ✅ Complete

- Migration V19: current_elo on player (was part of V4 baseline)
- H2HRecord and OddsResult value objects
- findDirectConfrontations query on EventRepository
- OddsCalculator (pure service, named OddsCalculatorServiceImpl)
  🔄 elo-scale=400 (discussed changing to 600 — deferred to Group 12)
  🔄 draw-factor=0.28 (discussed changing to 0.12 — deferred to Group 12)
- EloService (called only from finishEvent)
- Hook in EventService.create: Market (OPEN) + three Outcomes
- Hook in EventService.finishEvent: EloService update
- GET /api/v1/markets/{eventId}
- POST /api/v1/markets/{eventId}/status (admin only) 🆕
- OddsCalculator unit tests (9 scenarios)
- EloService unit tests (9 scenarios)

### Group 4 — Betting System ✅ Complete

- bet_slip table: id, user_id, tournament_id, stake, combined_odd,
  potential_return, status (PENDING/WON/LOST/CANCELLED), created_at
- bet_slip_item table: id, bet_slip_id, event_id, outcome_id, odd_snapshot, status
  🔄 no VOID status (removed from original plan)
- UNIQUE(bet_slip_id, event_id) on bet_slip_item
- Transaction references bet_slip_id
- POST /api/v1/bets 🔄 (was /bet-slips)
- GET /api/v1/bets/me 🔄 (was /bet-slips/me)
- GET /api/v1/bets?eventId={id} (admin only)
- Hook in EventService.finishEvent: resolve BetSlipItems, pay winners
- TransactionType: DEPOSIT, WITHDRAWAL, BET_PLACED, BET_WON 🔄

### Group 5 — Player Statistics ✅ Complete

- GET /api/v1/players/{id}/stats (all-time)
- GET /api/v1/players/{id}/stats?tournamentId={id} (tournament-scoped)
- GET /api/v1/tournaments/{id}/scoreboard 🆕 (aggregation endpoint)

### Group 6 — WebSocket STOMP ✅ Complete

- spring-boot-starter-websocket added
- WebSocketConfig: endpoint /ws, broker /topic, SockJS fallback, CORS
- Spring ApplicationEvent pattern (EventChangeEvent, MarketChangeEvent, WalletChangeEvent) 🔄
- Broadcast to /topic/events/{id} via EventChangeEvent
- Broadcast to /topic/markets/{eventId} via MarketChangeEvent 🆕
- Broadcast to /topic/wallet/{userId} via WalletChangeEvent 🆕
- All broadcasts use @TransactionalEventListener AFTER_COMMIT 🔄

### Group 7 — Phase 2 Backend Tests ✅ Complete

✅ PinService tests (5 scenarios)
✅ OddsCalculator tests (9 scenarios)
✅ EloService tests (9 scenarios)
✅ AuthService tests
✅ UserService tests
✅ MarketService tests
✅ EventService tests
✅ Controller tests: Player, Event, Market, Team, Tournament
✅ BetSlipService/BetService and WalletService tests

### Group 8 — Auth and Identity (Frontend) ✅ Complete

- AuthService (login, logout, getMe, isLoggedIn, isAdmin, currentUser signal)
- UsersApi, PlayersApi updated
- Login screen refactored (cards, PIN modal, self-registration)
- AdminOnlyDirective (*appAdminOnly)
- AuthTokenInterceptor (auto-attaches Bearer token)
- ApiErrorToastInterceptor (global error handling)
- Wallet chip in header (R$ format, WebSocket updates)
- 🔄 No dedicated AuthGuard — conditional rendering in root component instead

### Group 9 — Betting (Frontend) ✅ Complete

✅ BetsApi (placeBet, getMyBets, getEventBets)
✅ MarketsApi (getMarket, setMarketStatus)
✅ WalletApi (getWallet, deposit, depositAll)
✅ Single-event bet on Event Page (outcome select, stake input, MAX button, confirmation)
✅ Deposit UI (admin, inside EventPage admin section)
✅ "Give everyone" button → depositAll
✅ Multi-event bet cart (select outcomes across multiple events, combined odd display)
✅ Warning for existing PENDING item on same event
✅ Admin bet history table on Event Page

### Group 10 — WebSocket (Frontend) ✅ Complete

- @stomp/stompjs + sockjs-client installed
- WebSocketService (connect, subscribe<T>, disconnect)
- Event Page: subscribes /topic/events/{id}, /topic/markets/{eventId}, /topic/wallet/{userId}
- Handles startEvent, updateScore, finishEvent broadcasts
- Polling removed
- Disconnect on ngOnDestroy
- Dashboard "Live Now" modal: GET /api/v1/events?status=IN_PROGRESS on load,
  dismissible via component signal, no WebSocket on Dashboard

### Group 11 — Stats and Standings (Frontend) 🔶 Partially Done

✅ **Standings table on Tournament Page** — Done as part of Phase 7 B24.
  - Uses `GET /api/v1/tournaments/{id}/scoreboard` (endpoint exists)
  - Format-specific rendering: LEAGUE (single table), LEAGUE_BRACKET (per-group tables),
    BRACKET (placements list)
  - Team badge/initials display on standings
  - "Classificacao" tab

❌ **Stats on Global Players Page** (replace placeholders)
- Use `GET /api/v1/players/{id}/stats` (all-time stats)
- Display: matches played, wins, draws, losses, goals scored, goals conceded,
  win rate, tournament history

❌ **Stats on Tournament Players Tab**
- Show player stats scoped to the current tournament
- Use `GET /api/v1/players/{id}/stats?tournamentId={id}`
- Tournament-specific: matches played, goals, points, goal difference

### Group 12 — Deploy and Infrastructure ⏳ Not Started

❌ Fix hardcoded credentials (postgres/postgres in application.properties)
❌ Fix odds defaults: elo-scale 400→600, draw-factor 0.28→0.12
❌ Create Dockerfile (eclipse-temurin:21-jre)
❌ Move all config to environment variables
(DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD, ADMIN_NAME,
resenhabet.odds.elo-scale, resenhabet.odds.draw-factor)
❌ Create .env.example with all variables documented
❌ Create docker-compose.yml (Spring Boot + PostgreSQL, Flyway auto-runs)
❌ Create setup README.md (target: working instance in under 10 minutes)

---

## Cleanup ⏳ Not Started

❌ Remove unused legacy frontend components:
AdminPanel, BetHistory, GlobalHistory, Ranking, MatchPanel, BackendStatus
(none are used by any routed page — all reference old ResenhaBetState local logic)
❌ Clean ResenhaBetState:
remove local mock logic (bettors, currentMatch, betHistory, odds calculation),
keep only what bridges to API-based auth and wallet,
document what remains and why

---

## Group Bracket — Tournament Format Expansion ✅ Frontend Phase 7 Complete

---

### Domain decisions (closed)

**Penalties:** Option B — `penaltiesHome` and `penaltiesAway` fields on Event.
In knockout, if `homeScore == awayScore`, the event transitions to `PENALTIES` status.
The `PATCH /events/{id}/penalties` endpoint handles real-time penalty scoreboard updates.
When the frontend sends `status: COMPLETED`, the event is finalized.

**Third place match:** Configurable via `hasThirdPlaceMatch` boolean on Tournament.

**Generation mode:** Two modes — AUTO (system generates all rounds and events)
and MANUAL (system creates structure, admin fills players manually).
Both work for LEAGUE, BRACKET, and LEAGUE_BRACKET.

**Formats supported:**
LEAGUE — round-robin only
BRACKET — pure knockout
LEAGUE_BRACKET — group stage (league) followed by knockout phase

**Advance-to-bracket rule:**
Admin can advance at **any time** during the group stage.
If not all events are completed, remaining events are auto-completed as **0x0 draws**.
Standings are calculated based on current results (including the auto-completed draws).
Tie-breaking: Points → Goal Difference → Goals For → Goals Against (lower) → Elo.
This is the same tie-breaking logic used for the final scoreboard.

---

### Group configuration model (LEAGUE_BRACKET)

`numberOfGroups` is configurable on Tournament (default 1).

Rules:
- minimum 2 players per group: floor(playerCount / numberOfGroups) >= 2
- no empty groups
- groups are balanced: floor(p/g) players each, with (p mod g) groups getting +1
- valid values of numberOfGroups for p players: all g in [1, floor(p/2)]
- frontend selector is parametrized by this formula against enrolled player count

`playersAdvancingPerGroup` is configurable (how many from each group advance
to the knockout phase). Total advancing = numberOfGroups × playersAdvancingPerGroup.
This total does not need to be a power of 2 — byes handle the remainder.

For 1 group: full league + knockout with top N players (N = playersAdvancingPerGroup).
For 2+ groups: group stage + knockout with top N from each group.

---

### Phase 1 — Infrastructure and Schema ✅ COMPLETE

**B1. New fields on Tournament entity and migration**
✅ V20 migration: `generation_mode` (AUTO/MANUAL), `has_third_place_match` (BOOLEAN),
`number_of_groups` (INTEGER), `players_advancing_per_group` (INTEGER).
`TournamentFormat` enum expanded with `LEAGUE_BRACKET`.
`Tournament` entity updated with 4 new fields.

**B2. New field on TournamentRound entity and migration**
✅ V21 migration: `phase_type` (GROUP_STAGE/KNOCKOUT) on `tournament_round`.
All existing rounds default to GROUP_STAGE. BRACKET rounds created as KNOCKOUT.

**B3. New fields on Event entity and migration**
✅ V22 migration: `penalties_home` (INTEGER NULL), `penalties_away` (INTEGER NULL),
`next_round_event_id` (BIGINT NULL FK to event).
✅ V25 migration: `player_home_id`, `player_away_id`, `game_datetime` made nullable
(to support BRACKET MANUAL empty event slots).

**B4. Update TournamentRequestDTO**
✅ `format` field now required (LEAGUE/BRACKET/LEAGUE_BRACKET).
`generationMode` (AUTO/MANUAL), `hasThirdPlaceMatch`, `numberOfGroups`, `playersAdvancingPerGroup`.
`type` still hardcoded to `FIFA_MATCH` in `TournamentServiceImpl.create()`.
🔄 **Changed:** `numberOfGroups` and `playersAdvancingPerGroup` moved to
`StartTournamentRequestDTO` (sent when starting the tournament, not at creation).
Frontend config modal opens on "Iniciar Torneio" for LEAGUE_BRACKET.

**B5. Group configuration validator**
✅ `TournamentGroupConfigValidator` (pure service) created with `validate()` and `getValidConfigs()`.
✅ `GET /api/v1/tournaments/tournament-group-config?playerCount={n}` endpoint returns
valid group counts and player distribution.

---

### Phase 2 — Auto-generation (AUTO mode) ✅ COMPLETE

**B6. LEAGUE AUTO generation in startTournament()**
✅ When format=LEAGUE and generationMode=AUTO:
Shuffle players using Fisher-Yates, generate (n-1)*2 rounds.
Generate double round-robin events using circle method (Berger tables).
Create markets with 3 outcomes for all events.
`gameDatetime` left null.

**B7. BRACKET AUTO generation in startTournament()**
✅ When format=BRACKET and generationMode=AUTO:
Sort players by `currentElo` descending for seeding.
Calculate `nextPowerOf2`, number of byes = nextPowerOf2 - playerCount.
Seeded matchups: 1vN, 2v(N-1), etc.
Bye events: `isBye=true`, `status=COMPLETED`, auto-advance winner to next round.
**Skip ELO, bet resolution, market creation for byes** (performance optimization).
Create empty events for subsequent rounds with `nextRoundEvent` chaining.
3rd Place event if `hasThirdPlaceMatch=true`.
Markets auto-created for real matchups (2 outcomes).

**B8. LEAGUE_BRACKET AUTO generation in startTournament()**
✅ When format=LEAGUE_BRACKET and generationMode=AUTO:
Validate `numberOfGroups` via `TournamentGroupConfigValidator`.
Shuffle players, distribute into balanced groups, store `groupNumber` on `TournamentPlayer`.
Group-stage round naming: `"Rodada 1 - Grupo A"`, `"Rodada 1 - Grupo B"`, etc.
Generate round-robin events per group, markets auto-created (3 outcomes).
**Do NOT create knockout rounds** — they come from `advance-to-bracket`.

**V26 migration:** `group_number` INTEGER NULL added to `tournament_round`.
**Entity update:** `TournamentRound.groupNumber` field.
**Response update:** `TournamentRoundResponseDTO.groupNumber`.

---

### Phase 3 — Manual mode ✅ COMPLETE

**B9. LEAGUE MANUAL in startTournament()**
✅ When format=LEAGUE and generationMode=MANUAL:
Create rounds based on (n-1)*2 formula.
All rounds get `phaseType=GROUP_STAGE`.
Create empty event slots (no players assigned).

**B10. BRACKET MANUAL in startTournament()**
✅ When format=BRACKET and generationMode=MANUAL:
Create knockout rounds with semantic names and progressive multipliers.
Multipliers: Round 1 (1.0), Round 2 (1.2), ... Final (2.0).
🔄 Third place round inserted BEFORE the Final round (not after).
Create chained empty event slots via `nextRoundEventId`.
Admin assigns players to first-round events via `PATCH /events/{id}/players`.
System advances winner automatically after each `finishEvent()`.

**B11. LEAGUE_BRACKET MANUAL in startTournament()**
✅ When format=LEAGUE_BRACKET and generationMode=MANUAL:
Create GROUP_STAGE rounds with empty event slots (named "Rodada X - Grupo Y").
Admin fills group stage matchups via `PATCH /events/{id}/players`.
After advance-to-bracket: create KNOCKOUT rounds and empty events.

**B12. Create PATCH /api/v1/events/{id}/players**
✅ `PATCH /api/v1/events/{id}/players` endpoint created.
Admin only. Assigns `playerHome` and/or `playerAway` on a `CREATED` event.
Validates players are enrolled in the tournament.
If both players are now assigned, auto-creates market.
For knockout events: creates 2 outcomes (no draw). For league: 3 outcomes.

**B13. TournamentPlayer groupNumber field**
✅ V23 migration: `group_number` INTEGER NULL on `tournament_player`.
✅ `TournamentPlayerResponseDTO` exposes `groupNumber`.

---

### Phase 4 — Event behavior in knockout ✅ COMPLETE

**B14. Penalty handling in finishEvent()**
✅ `finishEvent(Long eventId)` now detects knockout draws:
- If `isKnockout && homeScore == awayScore` → transitions to `PENALTIES` status.
  No bet resolution, no ELO, no advancement at this stage.
- If `isKnockout && homeScore != awayScore` → `COMPLETED` (resolves bets, ELO, advances winner).
- If not knockout → `COMPLETED` (existing behavior).

✅ `PATCH /api/v1/events/{id}/penalties` endpoint:
- Accepts `FinishEventRequestDTO` (penaltiesHome, penaltiesAway, status).
- Real-time updates: if `status` is null or `PENALTIES` → just updates penalty scores.
- Finalization: if `status == COMPLETED` → validates different scores, sets `COMPLETED`,
  resolves bets, ELO, advances winner.

**B15. Winner advancement in finishEvent()**
✅ `advanceKnockoutWinner(Event event)` private method:
- Determines winner by regular score OR penalties (if set).
- Winner assigned to `nextRoundEvent`: `playerHome` first, then `playerAway`.
- If both slots filled → auto-creates market via `createMarketAndOutcomesForEvent()`.
- If `nextRoundEvent` is the Final round AND `hasThirdPlaceMatch=true`:
  loser auto-assigned to 3rd Place event (same slot logic).
- Tournament completion: `checkTournamentCompletion()` sets `COMPLETED` only when
  **ALL** events in the tournament are `COMPLETED` (not just the final).

**B16. No draw outcome for knockout markets**
✅ Already implemented in Phase 3. `createMarketAndOutcomesForEvent()`:
- If `isKnockout` → calls `oddsCalculatorService.calculateNoDraw()` (splits probability
  home/away only, no draw factor).
- Creates 2 outcomes: "Vitória Casa", "Vitória Fora".
- For league: 3 outcomes including "Empate".

**B17. Draw validation in updateScore()**
✅ No changes needed. `updateScore()` already allows equal scores without validation.
For knockout events, equal scores are valid during normal time and trigger `PENALTIES`
status at `finishEvent` time.

---

### Phase 5 — Group stage transition (LEAGUE_BRACKET) ✅ COMPLETE

**B18. POST /api/v1/tournaments/{id}/advance-to-bracket**
✅ Admin-only endpoint implemented.
Validates: `format=LEAGUE_BRACKET`, `status=IN_PROGRESS`.
Validates: knockout phase doesn't already exist.
🔄 **Changed from original plan:** Admin can advance at **any time** — even if group stage isn't complete.
If incomplete events exist: auto-completes them as **0x0 draws**.
Group standings calculated, top `playersAdvancingPerGroup` per group advance.
Tie-breaking: Points → Goal Difference → Goals For → Goals Against (lower) → Elo.
Seeded into bracket: group winners seeded first, then runners-up, etc.
`AUTO` mode: seeded players auto-assigned, byes handled, markets created.
`MANUAL` mode: empty events created for admin to fill.
3rd Place event created if `hasThirdPlaceMatch=true`.

**Auto-trigger:** When `generationMode=AUTO` and all group events are `COMPLETED`,
`checkTournamentCompletion()` auto-calls `advanceToBracket()`.

**B19. Group standings calculation**
✅ `calculateGroupStandings()` private method: mini-standings per group.
Tie-breaking: Points → Goal Difference → Goals For → Goals Against (lower) → Elo.
Exposed via `GET /api/v1/tournaments/{id}/scoreboard` (see B20).

---

### Phase 6 — Scoreboard ✅ COMPLETE

**B20. Scoreboard differentiated by format**
✅ Polymorphic response using `@JsonInclude(JsonInclude.Include.NON_NULL)`:

**LEAGUE:** `entries` — points table (existing behavior, unchanged).

**BRACKET:** `placements` — elimination round placements.
- Champion (Final winner, position 1)
- Runner-up (Final loser, position 2)
- 3rd/4th Place (from 3rd place match, if exists)
- Remaining sorted by furthest round reached, then Elo.

**LEAGUE_BRACKET:** `groups` + `placements` (null until knockout phase).
- `groups`: per-group standings tables
- `placements`: bracket placements (null until `advance-to-bracket` called)

**New DTOs:**
- `GroupStandingsDTO`: `groupNumber`, `List<PlayerStatsResponseDTO> standings`
- `BracketPlacementDTO`: `playerId`, `playerName`, `position`, `eliminationRound`

**B21. Scoreboard endpoint response shape**
```json
// LEAGUE_BRACKET during group stage
{
  "tournamentId": 1,
  "format": "LEAGUE_BRACKET",
  "groups": [
    {
      "groupNumber": 1,
      "standings": [
        { "playerId": 3, "playerName": "Ana", "matchesPlayed": 3,
          "wins": 2, "draws": 1, "losses": 0, "goalsScored": 6,
          "goalsConceded": 2, "goalDifference": 4, "points": 7 }
      ]
    }
  ]
}

// LEAGUE_BRACKET after advance-to-bracket
{
  "tournamentId": 1,
  "format": "LEAGUE_BRACKET",
  "groups": [ ... ],
  "placements": [
    { "playerId": 3, "playerName": "Ana", "position": 1, "eliminationRound": "Champion" },
    { "playerId": 5, "playerName": "Bia", "position": 2, "eliminationRound": "Runner-up" }
  ]
}
```

---

### Phase 7 — Frontend ✅ COMPLETE

**B22. Update tournament creation modal on Dashboard**
✅ Format selector: LEAGUE / BRACKET / LEAGUE_BRACKET with format descriptions
✅ Generation Mode: toggle "Gerar automaticamente" / "Preencher manualmente" with explanation
✅ Third Place Match: checkbox, visible only for BRACKET and LEAGUE_BRACKET
✅ Number of Groups: dropdown (not number input), visible only for LEAGUE_BRACKET.
  Populated dynamically from `GET /api/v1/tournaments/group-config` after players are added.
  Shows valid options only. Displays player distribution preview.
✅ Players Advancing Per Group: number input, visible only for LEAGUE_BRACKET.
  Shows total advancing preview.

**B23. Bracket visualization component on Tournament Page**
✅ "Mata-mata" tab conditionally shown (BRACKET always, LEAGUE_BRACKET after knockout exists).
✅ Horizontal round tabs: click to switch between bracket rounds.
✅ Vertical match list per round with swipeable content.
✅ Match cards show: player name + team badge, score, winner highlighting (cyan border + name).
✅ Penalties shown in match header: "Penaltis: X x Y" below the score.
✅ Empty slots (TBD): show "?" placeholder with gray styling.
✅ Bye events: show "Avancou automaticamente (BYE)" label.
✅ Auto-selects first knockout round when bracket data loads.
✅ Data from `GET /api/v1/tournaments/{id}/rounds` + `GET /api/v1/events?tournamentId={id}`.
🔄 **Changed from original plan:** Not a full bracket grid. Instead: horizontal round tabs
with vertical match list per round. Simpler and more mobile-friendly.

**B24. Group stage standings on Tournament Page**
✅ "Classificacao" tab added as second tab (after "Partidas").
✅ LEAGUE: single standings table (points, wins, draws, losses, goal difference, goals for/against).
✅ LEAGUE_BRACKET: per-group standings tables (each group with its own table) +
  bracket placements section (appears after advance-to-bracket).
✅ BRACKET: elimination round placements (Champion, Runner-up, etc.) with position badges.
✅ Team badges/initials shown next to player names in all standings.
✅ Uses `GET /api/v1/tournaments/{id}/scoreboard`.

**B25. Penalties input on Event Page**
✅ Two-step penalty flow:
  1. Admin clicks "Encerrar Partida" on knockout draw → `POST /events/{id}/end` called
     (transitions to `PENALTIES` status with yellow badge).
  2. Penalty modal opens with input fields (penaltiesHome, penaltiesAway).
  3. "Atualizar Penaltis" button: PATCH with `status: PENALTIES` → real-time scoreboard update.
  4. "Finalizar Partida" button: PATCH with `status: COMPLETED` → validates different scores,
     resolves bets, ELO, advances winner.
✅ PENALTIES status badge displayed on event page (yellow background).
✅ Penalty scores shown in event header next to regular scores.
✅ Disabled state when scores are equal (validates before enabling Finalizar).

**B26. "Advance to Bracket" button on Tournament Page**
✅ Admin-only button visible when `canAdvanceToBracket()` is true:
  `format=LEAGUE_BRACKET`, `status=IN_PROGRESS`, no knockout rounds yet.
✅ Confirmation modal with explanation: "A fase de grupos sera encerrada...
  Jogadores serao classificados automaticamente e o mata-mata sera gerado."
✅ Calls `POST /api/v1/tournaments/{id}/advance-to-bracket`.
✅ On success: refreshes tournament data, shows bracket tab.
✅ Error handling with user-friendly messages.

**B27. Group number display**
✅ Players tab: "Grupo" column shown for LEAGUE_BRACKET format.
✅ Matches tab: group labels shown under round names for LEAGUE_BRACKET group stage.
✅ `groupLabel()` helper: "Grupo A", "Grupo B", etc.

**Frontend API Models added/updated:**
✅ `TournamentResponseDto`: added `format`, `generationMode`, `hasThirdPlaceMatch`,
  `numberOfGroups`, `playersAdvancingPerGroup`, `rounds`
✅ `TournamentRoundResponseDto`: added `phaseType`, `groupNumber`
✅ `TournamentPlayerResponseDto`: added `groupNumber`
✅ `EventResponseDto`: added `isKnockout`, `isBye`, `penaltiesHome`, `penaltiesAway`, `nextRoundEventId`
✅ `CreateTournamentRequestDto`: added `format`, `generationMode`, `hasThirdPlaceMatch`
✅ `StartTournamentRequestDto` 🆕: `numberOfGroups`, `playersAdvancingPerGroup`
✅ `TournamentGroupConfigResponseDto` 🆕: `validGroupCounts`, `playerCount`, `options`
✅ `TournamentGroupOptionDto` 🆕: `groupCount`, `playersPerGroup`, `remainderPlayers`, `totalGroups`
✅ `PlayerStatsResponseDto` 🆕: `playerId`, `playerName`, `matchesPlayed`, `wins`, `draws`, `losses`,
  `goalsScored`, `goalsConceded`, `goalDifference`, `points`
✅ `GroupStandingsDto` 🆕: `groupNumber`, `standings`
✅ `BracketPlacementDto` 🆕: `playerId`, `playerName`, `position`, `eliminationRound`
✅ `TournamentScoreboardResponseDto` 🆕: `tournamentId`, `format`, `entries`, `groups`, `placements`
✅ `FinishEventRequestDto` 🆕: `penaltiesHome`, `penaltiesAway`, `status`
✅ `PatchEventPlayersRequestDto` 🆕: `playerHomeId`, `playerAwayId`

**Frontend API Services added/updated:**
✅ `tournaments-api.ts`: `getGroupConfig()`, `getScoreboard()`, `advanceToBracket()`,
  `start()` updated to accept `StartTournamentRequestDto`
✅ `events-api.ts`: `recordPenalties()`, `updatePlayers()` (PATCH)

**Frontend Components updated:**
✅ `home-page.ts` + `home-page.html`: Creation modal with format selector, generation mode,
  third place match checkbox, group config dropdown
✅ `tournament-page.ts` + `tournament-page.html`: 
  - Tab reorder: Partidas | Classificacao | Mata-mata* | Jogadores
  - Scoreboard loading, standings tab with format-specific rendering
  - Bracket tab with round tabs and match cards
  - Advance-to-bracket modal and button
  - Start config modal for LEAGUE_BRACKET (group count, players advancing)
  - Group label display in matches tab
  - Team badges in standings tables
  - `matchWinner()` helper for winner highlighting
  - `groupLabel()` helper for "Grupo A", "Grupo B"
  - `eliminationRoundLabel()` helper for placement labels
  - `knockoutRounds()` computed: filters KNOCKOUT phaseType
  - `showBracketTab()` computed: shows bracket tab for BRACKET or LEAGUE_BRACKET with knockout rounds
  - `activeBracketRound` signal: tracks selected round in bracket view
  - Auto-select first bracket round effect
✅ `event-page.ts` + `event-page.html`:
  - Two-step penalty flow: `endMatch()` → `PENALTIES` status → penalty modal
  - `submitPenalties()` and `finalizeWithPenalties()` methods
  - Penalty modal with "Atualizar Penaltis" and "Finalizar Partida" buttons
  - `winnerLabel()` updated for penalties
  - PENALTIES status display (yellow badge, penalty scores in header)

---

### Phase 8 — Tests ✅ COMPLETE

**B28. GroupConfigValidator unit tests ✅**
✅ `TournamentGroupConfigValidatorTest.java` (6 tests)
- 6 players / 1 group → valid
- 6 players / 2 groups → valid
- 6 players / 3 groups → valid
- 6 players / 4 groups → invalid
- 3 players / 2 groups → invalid
- 1 player / 1 group → invalid (minimum 2 players per group)
- `getValidConfigs` for 6 and 7 players with correct remainders

**B29. Bracket generation unit tests ✅**
✅ `TournamentServiceImplTest.java` (3 tests)
- BRACKET MANUAL 2 players: final only, correct round count
- BRACKET MANUAL 4 players: Semi-Finals + Final, correct round names
- BRACKET MANUAL 4 players with 3rd place: "3rd Place" inserted before Final

**B30. Knockout event behavior tests ✅**
✅ `EventServiceImplTest.java` (7 tests)
- finishEvent knockout with clear winner → COMPLETED, advances, no penalties
- finishEvent knockout with draw → PENALTIES status, no bets/ELO
- recordPenalties real-time update → status stays PENALTIES
- recordPenalties finalization with COMPLETED → resolves bets, ELO, advances
- recordPenalties equal scores → BusinessException
- recordPenalties not in PENALTIES status → BusinessException
- recordPenalties not knockout → BusinessException

**B31. LEAGUE_BRACKET flow tests ✅**
✅ `TournamentServiceImplTest.java` (4 tests)
- advance-to-bracket with incomplete group stage → auto-completes as 0x0 draws
- advance-to-bracket when knockout already exists → BusinessException
- advance-to-bracket when not LEAGUE_BRACKET → BusinessException
- advance-to-bracket when not IN_PROGRESS → BusinessException

**B32. Regression tests ✅**
✅ All 146 tests pass (123 existing + 23 new)
- LEAGUE MANUAL still works (startTournament tests)
- Existing odds calculation unaffected (OddsCalculator tests)
- Existing event flow unaffected (EventService tests)
- Existing tournament flow unaffected (TournamentService tests)

**Test coverage:**
- `TournamentGroupConfigValidatorTest`: 6 tests
- `TournamentServiceImplTest`: 12 tests (including 3 bracket + 4 advance-to-bracket)
- `EventServiceImplTest`: 12 tests (including 7 knockout behavior)
- Total: 146 tests, 0 failures, 0 errors

---

### Group Bracket Implementation Status

✅ **Phase 0 (Bug Fix)** — `checkTournamentCompletion` fixed for LEAGUE_BRACKET.
✅ **Phase 1 (Schema)** — Complete. V20–V26 migrations applied.
✅ **Phase 2 (Auto-generation)** — Complete. B6, B7, B8 implemented.
✅ **Phase 3 (Manual Mode)** — Complete. LEAGUE MANUAL, BRACKET MANUAL, PATCH /events/{id}/players.
✅ **Phase 4 (Knockout Behavior)** — Complete. B14–B17 all implemented.
✅ **Phase 5 (LEAGUE_BRACKET transition)** — Complete. B18, B19 implemented.
✅ **Phase 6 (Scoreboard)** — Complete. B20, B21 implemented.
✅ **Phase 7 (Frontend)** — Complete. B22–B27 all implemented.
✅ **Phase 8 (Tests)** — Complete. B28–B32 implemented. 146 tests passing.

### Backend Implementation Summary

**Files added:**
- `V26__ADD_TOURNAMENT_ROUND_GROUP_NUMBER.sql`
- `GroupStandingsDTO.java`
- `BracketPlacementDTO.java`
- `TournamentGroupConfigValidatorTest.java`

**Files modified (key changes):**
- `TournamentServiceImpl.java`: LEAGUE AUTO, BRACKET AUTO, LEAGUE_BRACKET AUTO, advance-to-bracket, scoreboard, group standings
- `EventServiceImpl.java`: Penalty handling, winner advancement, tournament completion
- `BetServiceImpl.java`: Penalty-aware bet resolution
- `TournamentService.java`: `advanceToBracket()` interface
- `TournamentController.java`: `POST /{id}/advance-to-bracket` endpoint
- `EventController.java`: `PATCH /{id}/penalties` endpoint
- `EventRepository.java`: `findByTournamentIdAndRoundName`, `findAllByTournamentId`
- `TournamentRound.java`: `groupNumber` field
- `TournamentRoundResponseDTO.java`: `groupNumber` field
- `TournamentScoreboardResponseDTO.java`: Polymorphic response
- `TournamentServiceImplTest.java`: Bracket + advance-to-bracket tests
- `EventServiceImplTest.java`: Knockout behavior tests

---

## Frontend Implementation Summary

**Files added (new):**
- `src/app/services/api/api.models.ts` — All DTOs added/updated (see list above)
- `src/app/services/api/tournaments-api.ts` — `getGroupConfig()`, `getScoreboard()`, `advanceToBracket()`
- `src/app/services/api/events-api.ts` — `recordPenalties()`, `updatePlayers()` (PATCH)

**Files modified (key changes):**
- `src/app/pages/home/home-page.ts` + `home-page.html` — Creation modal with format, generationMode,
  hasThirdPlaceMatch, group config dropdown
- `src/app/pages/tournament/tournament-page.ts` + `tournament-page.html` — Major overhaul:
  - Tab reorder and new "Classificacao" + "Mata-mata" tabs
  - Scoreboard integration (standings tab)
  - Bracket visualization (round tabs + match cards)
  - Advance-to-bracket modal + button
  - Start config modal for LEAGUE_BRACKET
  - Group labels, team badges, winner highlighting, penalty display
  - `matchWinner()`, `groupLabel()`, `eliminationRoundLabel()` helpers
  - `knockoutRounds()`, `showBracketTab()`, `activeBracketRound` signals
- `src/app/pages/event/event-page.ts` + `event-page.html` — Penalty flow:
  - Two-step flow: endMatch → PENALTIES → penalty modal → submit/finalize
  - `recordPenalties()` and `finalizeWithPenalties()` methods
  - Penalty modal UI
  - PENALTIES status display and badge

---

## Future — Backend Enhancement: TBD Slot Labels (Deferred)

**Status:** ⏳ Deferred for future implementation

**Problem:** When bracket matches have TBD slots (playerHomeId=0 or playerAwayId=0),
frontend shows generic "TBD" or "?" — user wants to see "Vencedor da Semi-final" or
actual player names for source matches.

**Proposed Solution:** Add `homeSourceEventId` and `awaySourceEventId` to `Event` entity
and `EventResponseDTO`. Set during bracket generation via `j / 2` structural pairing.

**Frontend Behavior:**
- `playerHomeId` assigned → show player name (existing)
- `playerHomeId` null + `homeSourceEventId` set → resolve source event:
  - Source completed → show winner name
  - Source in progress → show "Vencedor de [round name]"
  - Source has known players → show "[Player1] ou [Player2]"
  - Source also TBD → show "Vencedor de [round name]"

**Benefits:** No fragile numbering, explicit source-of-truth, works for 3rd place match
("Perdedor de Semi-final").

---

## Future Group — MarketScheduler

- Create MarketScheduler: @Scheduled(fixedDelay=60000),
  suspend OPEN markets where event.gameDatetime < now
- Defensive validation in EventService.startEvent:
  if market still OPEN when admin starts event early, suspend as fallback

---

## Future Group — Dynamic Odds and Cashout

Depends on MarketScheduler being complete first.

- Create outcome_odd_history table (add now, empty, to avoid future migration on live data)
- Update OddsCalculator to accept current score as input
- Trigger recalculation in updateScore
- Broadcast updated odds via WebSocket
- POST /api/v1/bets/{id}/cashout (settle early, partial return)
- Frontend: cashout button when event is IN_PROGRESS

---

## Future Group — Multi-Group Platform

Do not start until Phase 2 is deployed and stable.

- Group entity (id, name, invite_code, created_at)
- GroupMember entity (user_id, group_id, role, joined_at)
- Wallet becomes group-scoped: UNIQUE(user_id, group_id)
- Player becomes group-scoped: add group_id FK
- Tournament becomes group-scoped: add group_id FK
- Expand CurrentContext to carry userId and groupId
- All repository queries filtered by groupId
- Group selector in login flow
- Invite and membership flow
- Decide: open group creation or admin-only
