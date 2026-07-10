# ResenhaBET v2 — Master Project Briefing

## What is ResenhaBET?

ResenhaBET is a private, self-hosted, open source FIFA tournament management and
sports betting platform built for a closed group of friends. There is no commercial
intent. Each group of friends runs their own isolated instance against their own
database — no multi-tenancy, no shared infrastructure between groups.

The project is a full rewrite of v1, which was a Node.js/Express/Socket.IO application
that kept all state in memory and persisted via a single JSON file. v2 replaces it with
a proper Spring Boot backend, a PostgreSQL database, and an Angular frontend.

---

## Tech Stack

### Backend
- Java 21
- Spring Boot 3.4
- Spring Data JPA (Hibernate)
- Flyway (versioned migrations)
- MapStruct (entity → DTO mapping)
- Lombok
- PostgreSQL (schema: `resenha`)
- JUnit 5 + Mockito + MockMvc

### Frontend
- Angular (standalone components, signals, Tailwind CSS)
- Angular HttpClient
- `@stomp/stompjs` (Phase 2 WebSocket — not yet implemented)

### Infrastructure
- Backend: Railway (one project per group instance)
- Frontend: Vercel
- Containerization: Docker + docker-compose (planned, end of Phase 2)

---

## Project Structure

### Backend
```
com.franciscomaath.resenhaapi/
├── controller/
│   ├── dto/
│   │   ├── request/        # Request DTOs
│   │   └── response/       # Response DTOs
│   └── exception/
│       └── GlobalExceptionHandler.java
├── domain/
│   ├── entity/             # JPA entities
│   ├── enums/              # All enums
│   ├── exception/          # Custom exception classes
│   └── repository/         # Spring Data JPA repositories
├── mapper/                 # MapStruct mappers
├── service/
│   └── Impl/               # Service implementations
└── config/                 # CORS, WebSocket, Scheduler configs
```

### Frontend
```
frontend/src/app/
├── pages/                  # Routed page components
│   ├── home/
│   ├── players/
│   ├── tournament/
│   └── events/
├── services/
│   └── api/                # HTTP services per domain
│       ├── api.models.ts   # All TypeScript DTOs
│       ├── players-api.ts
│       ├── tournaments-api.ts
│       └── events-api.ts
├── components/             # Reusable UI components
└── guards/                 # Route guards
```

---

## Error Handling Convention

All errors return the same JSON shape:

```json
{
  "timestamp": "2025-08-10T20:00:00",
  "status": 404,
  "error": "Resource Not Found",
  "message": "Player with id 99 not found",
  "fieldErrors": null
}
```

Custom exception → HTTP status:
- `ResourceNotFoundException` → 404
- `BusinessException` → 400
- `InvalidStateException` → 409
- `DuplicateResourceException` → 409
- `InsufficientFundsException` → 402
- `UnauthorizedException` → 401
- `ValidationException` → 422

---

## Domain Model

### Player
A person who plays FIFA matches in tournaments. Created by the admin.
Fields: `id`, `name`, `active`, `currentElo`, `user` (nullable FK to User).

A Player can exist without a User account (tournament-only participant).
A User can exist without a Player (bettor-only, never plays matches).
They are linked optionally via `player.user_id`.

### User
A person with an app account who can log in and place bets.
Fields: `id`, `name`, `pinHash` (nullable), `userType` (ADMIN/USER), `firstLogin`.

### Session
A session token created on login. UUID stored in DB with 24h TTL.
Frontend sends it as `Authorization: Bearer {uuid}` on every request.
`SessionFilter` validates it and populates the current user context.

### Wallet
One wallet per User. Global across all tournaments — same balance used everywhere.
Created automatically when a User is created.

### Transaction
Every wallet movement. Types: `DEPOSIT`, `BET`, `PRIZE`.
Has nullable `bet_id` FK for full traceability:
`transaction → bet → event → tournament`.

### Tournament
A competition container.
Fields: `id`, `name`, `type` (always `FIFA_MATCH`), `format` (LEAGUE — BRACKET is
future), `status` (CREATED → IN_PROGRESS → ENDED), `startDate`, `endDate`.

### TournamentRound
A phase within a tournament with an Elo multiplier.
Fields: `id`, `tournamentId`, `name`, `multiplier`, `roundOrder`.

For LEAGUE: auto-generated when tournament starts using formula `(n-1) * 2`
where n = number of enrolled players (double round-robin).
For BRACKET: provided manually at tournament creation time.

### TournamentPlayer
A full entity (not a simple join table) representing one player's enrollment
in one tournament. Has an optional `team_id` FK (which FIFA team they used).

### Team
A FIFA team. Fields: `id`, `name`, `abbreviation`, `badgeUrl`, `externalApiId`
(nullable, for future API integration), `country`, `league`.
Badge URL is entered manually — no external API integration in the current scope.

### Event
A single match. Status: CREATED → IN_PROGRESS → COMPLETED.
Fields: `id`, `tournamentId`, `roundId`, `playerHomeId`, `playerAwayId`,
`gameDatetime`, `status`, `homeScore`, `awayScore`, `isKnockout`, `isBye`.

### Market
One betting market per Event. Always named "Resultado Final".
Created automatically when an Event is created.
Status: OPEN → SUSPENDED → CLOSED.

OPEN: bets accepted (from event creation until gameDatetime arrives).
SUSPENDED: no new bets (triggered automatically by a @Scheduled job when
gameDatetime passes — does NOT depend on admin manually starting the event).
CLOSED: all bets resolved (set when finishEvent is called).

This design decouples market lifecycle from admin action, which is critical for
long-duration tournaments where results are entered after the fact.

### Outcome
One of three betting options within a Market:
"Vitória Casa" (home win), "Empate" (draw), "Vitória Fora" (away win).
Each has a `odd` (decimal) calculated by OddsCalculator at event creation.

### Bet
A single bet by a User on one Outcome.
Fields: `id`, `userId`, `eventId`, `outcomeId`, `amount`, `oddSnapshot`
(locked at bet time, never changes), `potentialReturn` (amount × oddSnapshot),
`status` (PENDING → WON or LOST), `createdAt`.
Constraint: `UNIQUE(user_id, event_id)` — one bet per user per event.

---

## Complete Database Schema

```sql
player (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    current_elo  DECIMAL(10,2) NOT NULL DEFAULT 1000.00,
    user_id      BIGINT UNIQUE REFERENCES "user"(id)
)

"user" (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    pin_hash     VARCHAR(255),
    user_type    VARCHAR(10) NOT NULL DEFAULT 'USER',
    first_login  BOOLEAN NOT NULL DEFAULT TRUE
)

session (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES "user"(id),
    token        UUID NOT NULL UNIQUE,
    created_at   TIMESTAMP NOT NULL,
    expires_at   TIMESTAMP NOT NULL
)

wallet (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL UNIQUE REFERENCES "user"(id),
    balance      DECIMAL(10,2) NOT NULL DEFAULT 0.00
)

transaction (
    id           BIGSERIAL PRIMARY KEY,
    wallet_id    BIGINT NOT NULL REFERENCES wallet(id),
    bet_id       BIGINT REFERENCES bet(id),
    type         VARCHAR(20) NOT NULL,
    value        DECIMAL(10,2) NOT NULL,
    created_at   TIMESTAMP NOT NULL
)

team (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    abbreviation    VARCHAR(10),
    badge_url       VARCHAR(500),
    external_api_id BIGINT UNIQUE,
    country         VARCHAR(100),
    league          VARCHAR(255)
)

tournament (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    type         VARCHAR(50) NOT NULL DEFAULT 'FIFA_MATCH',
    format       VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    start_date   TIMESTAMP,
    end_date     TIMESTAMP
)

tournament_round (
    id            BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournament(id),
    name          VARCHAR(255) NOT NULL,
    multiplier    DECIMAL(5,2) NOT NULL DEFAULT 1.00,
    round_order   INTEGER NOT NULL
)

tournament_player (
    id            BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL REFERENCES tournament(id),
    player_id     BIGINT NOT NULL REFERENCES player(id),
    team_id       BIGINT REFERENCES team(id)
)

event (
    id              BIGSERIAL PRIMARY KEY,
    tournament_id   BIGINT NOT NULL REFERENCES tournament(id),
    round_id        BIGINT REFERENCES tournament_round(id),
    player_home_id  BIGINT NOT NULL REFERENCES player(id),
    player_away_id  BIGINT NOT NULL REFERENCES player(id),
    game_datetime   TIMESTAMP,
    status          VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    home_score      INTEGER,
    away_score      INTEGER,
    is_knockout     BOOLEAN NOT NULL DEFAULT FALSE,
    is_bye          BOOLEAN NOT NULL DEFAULT FALSE
)

market (
    id        BIGSERIAL PRIMARY KEY,
    event_id  BIGINT NOT NULL UNIQUE REFERENCES event(id),
    name      VARCHAR(255) NOT NULL DEFAULT 'Resultado Final',
    status    VARCHAR(20) NOT NULL DEFAULT 'OPEN'
)

outcome (
    id         BIGSERIAL PRIMARY KEY,
    market_id  BIGINT NOT NULL REFERENCES market(id),
    name       VARCHAR(255) NOT NULL,
    odd        DECIMAL(10,2) NOT NULL
)

bet (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES "user"(id),
    event_id         BIGINT NOT NULL REFERENCES event(id),
    outcome_id       BIGINT NOT NULL REFERENCES outcome(id),
    amount           DECIMAL(10,2) NOT NULL,
    odd_snapshot     DECIMAL(10,2) NOT NULL,
    potential_return DECIMAL(10,2) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at       TIMESTAMP NOT NULL,
    UNIQUE(user_id, event_id)
)
```

---

## Authentication Model

No JWT. No Spring Security filter chain. Simple session-based auth designed
for a closed group of friends.

### Login flow
```
Login screen → shows all Users as selectable cards
User taps card
  ├── pin_hash NOT NULL          → ask for 4-digit PIN → validate → create session
  ├── pin_hash NULL, firstLogin  → ask "Set a PIN?" → optional → create session
  └── pin_hash NULL, !firstLogin → create session directly
On success → UUID token stored in session table (24h TTL)
Frontend stores token in localStorage
All requests send: Authorization: Bearer {uuid}
SessionFilter validates token, loads user into context
```

### PIN storage
SHA-256 with per-user salt. Not plain text, not BCrypt (overkill for 4 digits).

### Admin
One admin per instance. Seeded by a DataInitializer on first startup.
Admin name configurable via `resenhabet.admin.name` in application.properties.

### PIN reset
`PATCH /api/v1/users/{id}/reset-pin` (admin only) sets pin_hash = null
and firstLogin = true.

---

## Odds Calculation System

See `ODDS_CALCULATION.md` for the full specification with step-by-step formulas,
worked examples, and unit test scenarios.

### Summary
Odds are static — calculated once at event creation, never updated.
Uses two inputs: current Elo of each player + head-to-head history between them.
No house margin.

### Five steps
1. Base probabilities from Elo (configurable scale, default 600)
2. Draw probability introduced via draw_factor (default 0.12)
3. H2H modifier blended in, weighted by sample size (max 20% influence)
4. Renormalize to ensure probabilities sum to 1.0
5. Convert to decimal odds with minimum odd guard (default 1.05)

### Key configurable parameters
```properties
resenhabet.odds.elo-scale=600
resenhabet.odds.draw-factor=0.12
resenhabet.odds.max-h2h-weight=0.20
resenhabet.odds.min-odd=1.05
resenhabet.odds.h2h-match-limit=10
```

### Elo update (after each COMPLETED event)
```
K        = 32 × round.multiplier
expected = 1 / (1 + 10 ^ ((elo_opponent - elo_player) / elo-scale))
actual   = 1.0 (win), 0.5 (draw), 0.0 (loss)
new_elo  = current_elo + K × (actual - expected)
```

---

## Market Lifecycle

```
POST /api/v1/events (event created with gameDatetime)
  → Market created with status = OPEN
  → Outcomes created with odds from OddsCalculator

@Scheduled job runs every 60 seconds
  → finds all OPEN markets where event.gameDatetime < now
  → sets market.status = SUSPENDED

PATCH /api/v1/events/{id}/start (admin)
  → event.status = IN_PROGRESS
  → if market still OPEN (started early), suspend as fallback

PATCH /api/v1/events/{id}/end (admin, requires score set)
  → resolve all PENDING bets
  → update Elo of both players
  → market.status = CLOSED
  → event.status = COMPLETED
```

---

## Phase 1 — COMPLETE

All matches API endpoints are implemented, controller tests pass,
and all Phase 1 frontend pages are complete.

### Implemented REST endpoints

```
# Players
POST   /api/v1/players
GET    /api/v1/players
GET    /api/v1/players/{id}
PUT    /api/v1/players/{id}

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

# Events
POST   /api/v1/events
GET    /api/v1/events
GET    /api/v1/events?tournamentId={id}
PATCH  /api/v1/events/{id}/start
PATCH  /api/v1/events/{id}/score
PATCH  /api/v1/events/{id}/end
```

### Implemented frontend pages (Phase 1)
- Player selection screen (cards, initials avatar, Francisco as hardcoded admin)
- Dashboard (tournament cards, status badges, create modal, empty/loading/error states)
- Tournament page (matches tab grouped by round, players tab with team assignment,
  leaderboard placeholder, start tournament, add player, create match modal)
- Event page (match header, live score via polling, start/score/end admin controls,
  completed state with final scoreboard)
- Global players page (list, create, edit, activate/deactivate)

---

## Phase 2 — IN PROGRESS

### Group 1 — Identity Model ✅ COMPLETE
- Migration V16: `user_id` FK on `player`
- Migration V17: altered `user` table (pin_hash, user_type, first_login)
- Migration V18: `session` table
- Updated Player and User entities
- PinService (SHA-256 + salt)
- SessionFilter
- Auth endpoints: login, logout, me, pin
- User endpoints: create, list, reset-pin
- Player endpoint: link-user
- DataInitializer for admin user

### Group 2 — Wallet and Transactions ✅ COMPLETE
- Wallet auto-created on user registration
- `GET /api/v1/wallet/me`
- `POST /api/v1/wallet/deposit` (admin only)
- `bet_id` FK added to transaction table

### Group 3 — Markets and Odds 🔄 IN PROGRESS

**Completed so far:**
- Migration V19: `current_elo` on `player`
- `currentElo` field on Player entity and PlayerResponseDTO
- `H2HRecord` value object
- `OddsResult` value object
- `findDirectConfrontations` query on EventRepository
- `EloService`

**Currently being implemented:**
- `OddsCalculator` — the 5-step calculation service (see ODDS_CALCULATION.md)

**Remaining in Group 3:**
- Hook in `EventService.create` to create Market and Outcomes automatically
- Hook in `EventService.finishEvent` to update Elo after match ends
- `MarketScheduler` with @Scheduled job
- Defensive validation in `EventService.startEvent`
- `GET /api/v1/markets/{eventId}`
- Unit tests for OddsCalculator (8 scenarios)
- Unit tests for EloService

### Group 4 — Betting System ⏳ NOT STARTED
### Group 5 — Player Statistics ⏳ NOT STARTED
### Group 6 — WebSocket STOMP ⏳ NOT STARTED
### Group 7 — Phase 2 Backend Tests ⏳ NOT STARTED
### Group 8 — Auth Frontend ⏳ NOT STARTED
### Group 9 — Betting Frontend ⏳ NOT STARTED
### Group 10 — WebSocket Frontend ⏳ NOT STARTED
### Group 11 — Stats and Standings Frontend ⏳ NOT STARTED
### Group 12 — Deploy and Infrastructure ⏳ NOT STARTED

---

## Phase 2 — Full Remaining Task List

### Group 3 — Markets and Odds (remaining tasks)

**26. Hook in EventService.create — create Market and Outcomes automatically**
After persisting the event: query H2H history via findDirectConfrontations,
build H2HRecord, call OddsCalculator.calculate(eloHome, eloAway, h2h),
create Market with status=OPEN and three Outcome entities using OddsResult.
All within the same @Transactional as event creation.

**27. Hook in EventService.finishEvent — update Elo**
After resolving bets, call EloService to update currentElo of both players
based on match result and round multiplier. Last operation in the @Transactional.

**28. Create MarketScheduler**
@Scheduled(fixedDelay=60000). Queries all Markets with status=OPEN where
event.gameDatetime < now. Sets status=SUSPENDED. Add @EnableScheduling to
main application class.

**29. Defensive validation in EventService.startEvent**
If market is still OPEN when admin manually starts event before scheduled time,
suspend it immediately as fallback. Scheduler is primary — this is safety net only.

**30. Create GET /api/v1/markets/{eventId}**
Public endpoint. Returns market with three outcomes, odds, and current status.
Used by frontend to render the bet panel and show open/suspended state.

**31. OddsCalculator unit tests — 8 scenarios from ODDS_CALCULATION.md**
Equal Elos no H2H, equal Elos H2H favoring home, large Elo gap no H2H,
large Elo gap H2H favoring underdog, H2H with 0 matches equals no-H2H result,
H2H with 10+ matches respects MAX_H2H_WEIGHT cap, probabilities sum to 1.0
in all scenarios, all odds >= MIN_ODD including extreme Elo gaps.

**32. EloService unit tests**
Initial Elo 1000, win increases winner/decreases loser, draw moves both less
than win/loss, round multiplier scales K-factor correctly.

---

### Group 4 — Betting System

**33. Create POST /api/v1/bets**
Authenticated user. Body: { eventId, outcomeId, amount }.
Validations in order: market must be OPEN, no existing PENDING bet for this
user on this event, wallet balance >= amount.
On success: wallet.balance -= amount, Transaction(BET, bet_id) created,
Bet created with status=PENDING and oddSnapshot locked from current outcome.odd.
Single @Transactional covering all operations.

**34. Create GET /api/v1/bets/me**
Authenticated. Returns own bet history with event and outcome details.
Ordered by createdAt descending.

**35. Hook in EventService.finishEvent — resolve bets**
Determine winning outcome from final score:
homeScore > awayScore → "Vitória Casa"
homeScore == awayScore → "Empate"
awayScore > homeScore → "Vitória Fora"
For each PENDING bet on this event:
  WON: bet.status=WON, wallet.balance += potentialReturn, Transaction(PRIZE, bet_id)
  LOST: bet.status=LOST, no wallet movement
market.status = CLOSED
All within the same @Transactional as finishEvent.

**36. Create GET /api/v1/bets?eventId={id}**
Admin only. All bets for a specific event.
Returns: user name, outcome chosen, amount, oddSnapshot, potentialReturn, status.

---

### Group 5 — Player Statistics

**37. Create GET /api/v1/players/{id}/stats**
All-time stats: matches played, wins, losses, draws, goals scored,
goals conceded, goal difference, current Elo.
Aggregates all COMPLETED events where player participated as home or away.

**38. Create GET /api/v1/players/{id}/stats?tournamentId={id}**
Same logic filtered to a specific tournament.
Include average round multiplier for the external ranking system.

---

### Group 6 — WebSocket STOMP

**39. Configure Spring WebSocket + STOMP**
Add spring-boot-starter-websocket. Create WebSocketConfig with endpoint /ws,
message broker at /topic, CORS for Angular dev server origins.

**40. Broadcast in updateScore**
After saving new score, publish EventResponseDTO to /topic/events/{id}.

**41. Broadcast in finishEvent**
After resolving bets and closing market, publish final event state
(with winning outcome) to /topic/events/{id}.

---

### Group 7 — Phase 2 Backend Tests

**42. PinService tests**
Hash not plain text, correct PIN validates true, wrong PIN validates false,
same PIN for different users produces different hashes.

**43. BetService tests**
Successful bet deducts balance and creates Transaction(BET),
insufficient funds throws InsufficientFundsException,
non-OPEN market throws BusinessException,
duplicate bet on same event throws BusinessException,
resolution credits winners with Transaction(PRIZE),
resolution marks losers LOST with no wallet movement.

---

### Group 8 — Auth and Identity (Frontend)

**44. Create AuthService**
Methods: login(userId, pin?), logout(), getMe(), isLoggedIn(), isAdmin(),
currentUser() as signal. Stores session token in localStorage.

**45. Create UsersApi and update PlayersApi**
UsersApi: getAll(), create({name}), resetPin(userId).
PlayersApi: add linkUser(playerId, userId).

**46. Refactor login screen**
Load Users via GET /api/v1/users. Display as cards with initials avatar.
Selection logic:
  firstLogin AND no pin → PIN definition modal
  pin exists → PIN input
  no pin AND not firstLogin → direct login

**47. Create PIN definition modal**
4-digit numeric input with mask, confirm field, "Set PIN" and "Not now" buttons.
Calls PATCH /api/v1/auth/pin if confirmed. Proceeds with login either way.

**48. Create self-registration flow**
"Create account" link on login screen. Name field only. Calls POST /api/v1/users.
New user appears in login list immediately.

**49. Create AuthGuard**
Redirects to /login if AuthService.isLoggedIn() is false.

**50. Replace hardcoded admin logic with AuthService.isAdmin()**
Remove Francisco hardcoded check from ResenhaBetState.

**51. Add wallet chip to header**
Calls GET /api/v1/wallet/me after login. Displays balance.
Updates via signal after bets and deposits.

---

### Group 9 — Betting (Frontend)

**52. Create BetsApi**
placeBet({eventId, outcomeId, amount}), getMyBets(), getEventBets(eventId).

**53. Create MarketsApi**
getMarket(eventId) → GET /api/v1/markets/{eventId}.

**54. Create WalletApi**
getMyWallet(), deposit({userId, amount}).

**55. Implement bet panel on Event Page**
Visible when market.status=OPEN and user authenticated.
Three outcome cards with odds. Select outcome, enter amount,
live potentialReturn = amount × odd displayed.
Submit calls POST /api/v1/bets.
If user already has PENDING bet on this event: show it highlighted,
hide the selection panel.

**56. Implement deposit UI (admin)**
Select user, enter amount, calls POST /api/v1/wallet/deposit.
Updates wallet chip on success.

**57. Implement bet history on completed Event Page (admin only)**
Visible when event.status=COMPLETED and isAdmin=true.
Table from GET /api/v1/bets?eventId={id}.
Columns: user, outcome, amount, odds, potential return,
result (WON in green / LOST in red).
Completely hidden from non-admins.

---

### Group 10 — WebSocket (Frontend)

**58. Install and configure @stomp/stompjs**
Create WebSocketService: connect(), subscribe(topic) returning Observable,
disconnect().

**59. Replace polling with WebSocket on Event Page**
Remove setInterval. Subscribe to /topic/events/{id}.
Update signals on message received. Disconnect on ngOnDestroy.

---

### Group 11 — Stats and Standings (Frontend)

**60. Implement real standings on Tournament Page**
Replace placeholder. Call GET /api/v1/players/{id}/stats?tournamentId={id}
in parallel for all enrolled players.
Columns: position, name, team, played, W, D, L, GF, GA, GD, points.
Sort: points → GD → GF.

**61. Implement stats on Global Players Page**
Call GET /api/v1/players/{id}/stats for each player.
Replace — placeholders with real data.

**62. Implement stats on Tournament Players Tab**
Same as above using GET /api/v1/players/{id}/stats?tournamentId={id}.

---

### Group 12 — Deploy and Infrastructure

**63. Create Dockerfile for backend**
Base: eclipse-temurin:21-jre. Copies built jar. Defines entrypoint.
Compatible with Railway automatic deploy.

**64. All config via environment variables**
DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD, ADMIN_NAME.
No hardcoded values in application.properties committed to repo.

**65. Create .env.example**
Documents every required variable with example values.
Lives at repo root.

**66. Create docker-compose.yml**
Spring Boot + PostgreSQL together. Variables read from .env.
New instance for a new group = copy .env.example, fill in, docker-compose up.
Flyway creates entire schema automatically on first startup.

**67. Create setup README.md**
Clone repo → copy .env.example to .env → fill variables → docker-compose up.
Target: working instance in under 10 minutes.

---

## Immediate Next Steps

Currently implementing: `OddsCalculator` (Group 3, task 24).

After OddsCalculator is complete:
1. Wire hooks in EventService.create (task 26)
2. Wire hooks in EventService.finishEvent (task 27)
3. Create MarketScheduler (task 28)
4. Defensive validation in startEvent (task 29)
5. Create GET /api/v1/markets/{eventId} (task 30)
6. Write OddsCalculator tests — 8 scenarios (task 31)
7. Write EloService tests (task 32)
8. Begin Group 4: betting endpoints

---

## Future Plans (Post Phase 2)

### Dynamic odds
Currently odds are static. Foundation for dynamic odds:
- New `outcome_odd_history` table (timestamp + trigger per change)
- `Outcome.odd` becomes current value, history tracks changes
- OddsCalculator accepts match state (score, optional minute) as input
- `updateScore` triggers recalculation
- `Bet.oddSnapshot` already handles this correctly — no changes needed there
- Recommended: add `outcome_odd_history` table now (empty/unused) to avoid
  a future migration touching live bet data

### Bracket format
`TournamentFormat.BRACKET` enum already exists.
`is_knockout` and `is_bye` fields already on Event entity.
Needs: manual round definition at creation + bracket visualization component.

### Multi-group platform (future phase)
Current architecture is one-instance-per-group (correct for self-hosted tool).
If a single hosted instance serving multiple groups is ever desired, it requires:
- `Group` entity as a first-class domain object
- `GroupMember` join entity (user + group + role)
- Group-scoped wallets (user_id + group_id instead of just user_id)
- Group-scoped players (Elo contamination across groups must be prevented)
- `CurrentContext` service expanded to carry both userId and groupId
- Every repository query filtered by groupId

Key preparation to do now: centralize context resolution in `CurrentContext`
so that adding groupId later requires changing only one place.

### Real football integration
`Tournament.type` already supports non-FIFA_MATCH values.
`Event` already has nullable `home_team_id` / `away_team_id`.
`Team.external_api_id` exists for API integration.
The entire data model is already prepared — only the service and API
integration layer needs to be built.

---

## Key Architectural Decisions and Rationale

**No JWT / no Spring Security filter chain**
The platform is for a closed group of friends. JWT adds complexity
(key management, refresh tokens, claims encoding) that is not justified
for this use case. Session tokens (UUID in DB with TTL) provide adequate
security with far less complexity.

**Static odds (not dynamic)**
In long-duration tournaments, admins register results after the fact.
Real-time odds updates would require continuous admin presence which
does not match actual usage patterns. Static odds calculated at event
creation are more appropriate and simpler.

**Market suspension via scheduler, not admin action**
Decouples the closing of bets from the admin starting the match.
In casual long tournaments, the admin often forgets or simply does not
press "start match" before playing. The scheduler ensures bets close
at the announced game time regardless.

**Wallet global per user, not per tournament**
A user who wins credits in one tournament naturally expects to use them
in the next. Per-tournament wallets would require the admin to manually
fund every participant for every new tournament.

**Player and User as separate entities**
A spectator who only bets (User without Player) and a player who does
not use the app (Player without User) are both valid use cases.
Forcing a 1:1 relationship would exclude both.

**No house margin on odds**
This is a friends platform. The admin deposits credits manually.
There is no profit motive. Clean probabilistic odds are more fun
and more transparent for the players.

**Elo scale of 600 instead of chess standard 400**
FIFA among friends has more variance than chess. A higher scale
compresses the probability range, preventing extreme odds from
dominating and keeping all three outcomes interesting to bet on.

**draw_factor of 0.12 instead of standard football 0.28**
FIFA matches among friends end in draw significantly less often
than professional football matches. Using 0.28 overestimates draw
probability and pushes home/away odds unrealistically high.
Calibrate against actual historical draw rate from v1 data.
