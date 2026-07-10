# ResenhaBET v2 — Phase 2 Briefing

## Project Context

ResenhaBET is a private, self-hosted, open source FIFA tournament management and sports betting platform built for a closed group of friends. There is no commercial intent. The goal is simplicity: anyone should be able to clone the repo, run it, and use it with their own group.

Phase 1 is complete. It covers tournament management, match tracking, and the full matches API. Phase 2 adds user identity, virtual currency betting, live WebSocket updates, and player statistics.

---

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.4, Spring Data JPA, Flyway, Spring WebSocket (STOMP)
- **Database:** PostgreSQL (schema: `resenha`)
- **Mapping:** MapStruct
- **Utilities:** Lombok
- **Testing:** JUnit 5, Mockito, MockMvc
- **Frontend:** Angular (standalone components, signals, Tailwind CSS)
- **Frontend HTTP:** Angular HttpClient
- **Frontend WebSocket:** `@stomp/stompjs`

---

## Architecture Overview

```
com.franciscomaath.resenhaapi/
├── controller/          # REST controllers + request/response DTOs
├── domain/
│   ├── entity/          # JPA entities
│   ├── enums/           # All enums
│   ├── exception/       # Custom exceptions
│   └── repository/      # Spring Data JPA repositories
├── mapper/              # MapStruct mappers
├── service/
│   └── Impl/            # Service implementations
└── config/              # WebSocket, CORS, Security configs
```

```
frontend/src/app/
├── pages/               # Page components (routed)
├── services/
│   └── api/             # HTTP services per domain
├── components/          # Reusable UI components
└── guards/              # Route guards
```

---

## Identity Model — Critical to Understand

There are **two distinct entities** that can be related but are independent:

### `Player`
A person who plays FIFA matches in tournaments. Created by the admin. Has a name and an `is_active` flag. **Does not require a user account.** Phase 1 was built entirely around players.

```sql
player (id, name, is_active, user_id FK NULLABLE UNIQUE)
```

The `user_id` on `player` is the Phase 2 addition. It links a player to their user account if they have one. A player without `user_id` can still participate in tournaments but cannot log in or place bets.

### `User`
A person with an app account. Can place bets. **Does not require a player profile.** A spectator who only bets but never plays is a valid user with no linked player.

```sql
user (id, name, pin_hash NULLABLE, user_type, first_login)
```

### Relationship
- One `User` can be linked to at most one `Player` (via `player.user_id`)
- A `User` without a linked `Player` is a bettor-only account
- A `Player` without a linked `User` is a tournament-only participant (no app access)
- The admin links them via `PATCH /api/v1/players/{id}/link-user`
- When the admin creates a `Player`, a corresponding `User` is **not** automatically created — the admin does it explicitly or the person self-registers and the admin links later

---

## Authentication Model

There is **no JWT, no email, no Spring Security filter chain complexity**. The auth model is intentionally simple for a self-hosted closed group.

### How it works
- Login screen shows all `User`s as selectable cards
- User taps their name
- If `pin_hash IS NOT NULL` → app asks for 4-digit PIN, validates against hash
- If `pin_hash IS NULL AND first_login = true` → app asks if they want to set a PIN (can skip)
- If `pin_hash IS NULL AND first_login = false` → logs in directly (user previously chose no PIN)
- On successful login, backend creates a **session token (UUID)** stored in a `session` table with a 24h TTL
- Frontend stores this token in `localStorage` and sends it as `Authorization: Bearer {uuid}` on every request
- Backend has a `SessionFilter` that validates the token and populates the current user context

### PIN storage
PINs are hashed with SHA-256 + a per-user salt. Not stored in plain text. BCrypt is overkill for a 4-digit PIN.

### Admin reset
If a user forgets their PIN, the admin calls `PATCH /api/v1/users/{id}/reset-pin` which sets `pin_hash = null` and `first_login = true`, allowing them to redefine on next login.

### User types
```java
public enum UserType {
    ADMIN,
    USER
}
```

There is only one admin. Admin is seeded by a data initializer on first startup (configurable via `application.properties`). Admin has access to all write operations and sensitive views (bet history, deposit credits, resolve matches).

---

## New Database Migrations (Phase 2)

### V16 — Add `user_id` to `player`
```sql
ALTER TABLE resenha.player
    ADD COLUMN user_id BIGINT UNIQUE,
    ADD CONSTRAINT fk_player_user FOREIGN KEY (user_id) REFERENCES resenha.user(id);
```

### V17 — Alter `user` table
```sql
ALTER TABLE resenha.user
    ADD COLUMN pin_hash    VARCHAR(255),
    ADD COLUMN user_type   VARCHAR(10) NOT NULL DEFAULT 'USER',
    ADD COLUMN first_login BOOLEAN     NOT NULL DEFAULT TRUE;

-- remove email if it exists from Phase 1 placeholder
ALTER TABLE resenha.user DROP COLUMN IF EXISTS email;
ALTER TABLE resenha.user DROP COLUMN IF EXISTS hashcode;
```

### V18 — Create `session` table
```sql
CREATE TABLE resenha.session (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES resenha.user(id),
    token      UUID        NOT NULL UNIQUE,
    created_at TIMESTAMP   NOT NULL,
    expires_at TIMESTAMP   NOT NULL
);
```

---

## Complete Entity Reference (Phase 2 state)

### `User`
```java
@Entity @Table(name = "user", schema = "resenha")
public class User {
    Long id;
    String name;
    String pinHash;          // nullable — user opted out of PIN
    UserType userType;       // ADMIN or USER
    boolean firstLogin;      // true until first successful login
}
```

### `Player` (updated)
```java
@Entity @Table(name = "player", schema = "resenha")
public class Player {
    Long id;
    String name;
    boolean active;
    @ManyToOne @JoinColumn(name = "user_id") User user; // nullable
}
```

### `Wallet`
```java
@Entity @Table(name = "wallet", schema = "resenha")
public class Wallet {
    Long id;
    @OneToOne User user;
    BigDecimal balance;
}
```

### `Transaction`
```java
@Entity @Table(name = "transaction", schema = "resenha")
public class Transaction {
    Long id;
    @ManyToOne Wallet wallet;
    @Enumerated(EnumType.STRING) TransactionType type; // DEPOSIT, BET, PRIZE
    BigDecimal value;
    LocalDateTime createdAt;
}
```

### `Market`
```java
@Entity @Table(name = "market", schema = "resenha")
public class Market {
    Long id;
    @OneToOne Event event;
    String name;             // always "Resultado Final" for now
    @Enumerated(EnumType.STRING) MarketStatus status; // OPEN, SUSPENDED, CLOSED
}
```

### `Outcome`
```java
@Entity @Table(name = "outcome", schema = "resenha")
public class Outcome {
    Long id;
    @ManyToOne Market market;
    String name;             // "Vitória Casa", "Empate", "Vitória Fora"
    BigDecimal odd;
}
```

### `Bet`
```java
@Entity @Table(name = "bet", schema = "resenha")
public class Bet {
    Long id;
    @ManyToOne User user;
    @ManyToOne Event event;
    @ManyToOne Outcome outcome;
    BigDecimal amount;
    BigDecimal oddSnapshot;      // odds locked at bet time — never changes
    BigDecimal potentialReturn;  // amount × oddSnapshot
    @Enumerated(EnumType.STRING) BetStatus status; // PENDING, WON, LOST
    LocalDateTime createdAt;
}
```

### `Session`
```java
@Entity @Table(name = "session", schema = "resenha")
public class Session {
    Long id;
    @ManyToOne User user;
    UUID token;
    LocalDateTime createdAt;
    LocalDateTime expiresAt;
}
```

---

## API Endpoints — Phase 2

### Auth
```
POST   /api/v1/auth/login          — { userId, pin? } → { token, userType, name, firstLogin }
POST   /api/v1/auth/logout         — invalidates session token
GET    /api/v1/auth/me             — returns current user from session token
PATCH  /api/v1/auth/pin            — { pin } — set or update own PIN (authenticated)
```

### Users
```
POST   /api/v1/users               — { name } — self-registration, creates wallet automatically
GET    /api/v1/users               — list all users (for login screen)
PATCH  /api/v1/users/{id}/reset-pin — admin only — resets pin_hash to null, firstLogin to true
```

### Players (updated)
```
PATCH  /api/v1/players/{id}/link-user — admin only — { userId } — links user to player
```

### Wallet
```
GET    /api/v1/wallet/me           — authenticated — current balance + recent transactions
POST   /api/v1/wallet/deposit      — admin only — { userId, amount } — add credits
```

### Markets
```
GET    /api/v1/markets/{eventId}   — public — market with three outcomes and current odds
```

### Bets
```
POST   /api/v1/bets                — authenticated — { eventId, outcomeId, amount }
GET    /api/v1/bets/me             — authenticated — own bet history
GET    /api/v1/bets?eventId={id}   — admin only — all bets for a specific event
```

### Players Stats (new)
```
GET    /api/v1/players/{id}/stats                        — all-time stats
GET    /api/v1/players/{id}/stats?tournamentId={id}      — stats scoped to tournament
```

---

## Key Business Rules

### Odds calculation
When an event is created (`POST /api/v1/events`), the system automatically:
1. Reads the current Elo of both players using `EloService`
2. Calculates implied probabilities and decimal odds using `OddsCalculator` (port of v1 JavaScript logic)
3. Creates one `Market` with `status = OPEN` and three `Outcome`s: home win, draw, away win

### Market lifecycle tied to event status
- Event `CREATED` → Market `OPEN` (bets accepted)
- Event `IN_PROGRESS` → Market `SUSPENDED` (no new bets, existing bets unchanged)
- Event `COMPLETED` → Market `CLOSED` (all bets resolved)

### Bet placement rules (all enforced in a single `@Transactional`)
- Market must be `OPEN`
- User cannot have another `PENDING` bet on the same event
- User's wallet balance must cover the amount
- `oddSnapshot` is written at bet time and never changes afterward
- `potentialReturn = amount × oddSnapshot`
- `wallet.balance -= amount`
- `Transaction(BET)` created

### Bet resolution (triggered by `finishEvent`, single `@Transactional`)
- Determine winner: `homeScore > awayScore` → home win, equal → draw, `awayScore > homeScore` → away win
- For each `PENDING` bet on the event:
  - If outcome matches winner: `bet.status = WON`, `wallet.balance += potentialReturn`, `Transaction(PRIZE)` created
  - Otherwise: `bet.status = LOST`, no wallet movement
- Market closed: `market.status = CLOSED`

### Elo calculation
- Each player starts with a base Elo (e.g. 1000)
- Elo changes after each `COMPLETED` event based on expected vs actual result
- `tournament_round.multiplier` scales the Elo delta for higher-stakes rounds (e.g. 1.4 for finals)
- `EloService` recalculates current Elo by replaying all completed events in chronological order

---

## Error Handling

All errors follow the same response shape established in Phase 1:

```json
{
  "timestamp": "2025-08-10T20:00:00",
  "status": 402,
  "error": "Insufficient Funds",
  "message": "Wallet balance (10.00) is insufficient for bet amount (25.00)",
  "fieldErrors": null
}
```

HTTP status mapping:
- `ResourceNotFoundException` → 404
- `BusinessException` → 400
- `InvalidStateException` → 409
- `DuplicateResourceException` → 409
- `InsufficientFundsException` → 402
- `UnauthorizedException` → 401
- `ValidationException` → 422

---

## WebSocket (STOMP)

### Configuration
- Endpoint: `/ws`
- Broker prefix: `/topic`
- CORS: allow Angular dev server origins

### Topics
- `/topic/events/{id}` — broadcast on every `updateScore` and `finishEvent`

### Payload
The broadcast payload is the same `EventResponseDTO` used by the REST endpoints. Frontend updates its local state from it without an additional HTTP call.

---

## Frontend — New Pages and Components

### Login Screen (refactored from Phase 1 player selection)
- Calls `GET /api/v1/users` to load all users
- Displays user cards with initials avatar
- Tap a card → check `pinHash` and `firstLogin` state → show PIN input or proceed
- "Create account" link opens self-registration flow
- After successful login, stores token in `localStorage` via `AuthService`

### Self-registration flow
- Simple modal or page: name field, submit
- Calls `POST /api/v1/users`
- On success, new user appears in the login list

### PIN definition modal
- 4-digit numeric input with mask
- "Set PIN" and "Not now" buttons
- "Not now" proceeds with login and sets `firstLogin = false` via the login response

### Header (updated)
- Shows logged-in user name
- Shows wallet balance chip (calls `GET /api/v1/wallet/me` after login)
- Logout button calls `POST /api/v1/auth/logout` and clears localStorage

### Event Page — Bet Panel (new section)
- Visible when `market.status = OPEN` and user is authenticated
- Three outcome cards with current odds
- User selects outcome, enters amount, sees live `potentialReturn = amount × odd`
- Submit calls `POST /api/v1/bets`
- If user already has `PENDING` bet on this event: show their current bet highlighted, hide selection panel

### Event Page — Completed State (updated)
- Final scoreboard with winner highlighted
- Admin only: bet history table from `GET /api/v1/bets?eventId={id}`
  - Columns: user, outcome chosen, amount, odds, potential return, result (WON green / LOST red)

### Tournament Page — Standings (updated)
- Replaces Phase 1 placeholder with real standings table
- Calls `GET /api/v1/players/{id}/stats?tournamentId={id}` in parallel for all enrolled players
- Columns: position, name, team, played, W, D, L, GF, GA, GD, points
- Sorted by points → goal difference → goals scored

### Players Pages (updated)
- Global: `GET /api/v1/players/{id}/stats` — replaces `—` placeholders with real data
- Tournament-scoped tab: `GET /api/v1/players/{id}/stats?tournamentId={id}`

---

## Frontend Services — New

### `AuthService`
```ts
login(userId: number, pin?: string): Observable<LoginResponse>
logout(): Observable<void>
getMe(): Observable<User>
isLoggedIn(): boolean
isAdmin(): boolean          // reads userType from stored session
currentUser(): Signal<User | null>
```

### `UsersApi`
```ts
getAll(): Observable<User[]>
create(dto: { name: string }): Observable<User>
resetPin(userId: number): Observable<void>   // admin only
```

### `BetsApi`
```ts
placeBet(dto: PlaceBetRequest): Observable<BetResponse>
getMyBets(): Observable<BetResponse[]>
getEventBets(eventId: number): Observable<BetResponse[]>   // admin only
```

### `MarketsApi`
```ts
getMarket(eventId: number): Observable<MarketResponse>
```

### `WalletApi`
```ts
getMyWallet(): Observable<WalletResponse>
deposit(dto: { userId: number, amount: number }): Observable<WalletResponse>   // admin only
```

### `WebSocketService`
```ts
connect(): void
subscribe(topic: string): Observable<any>
disconnect(): void
```

---

## Frontend Auth Flow

```
App loads
└── Check localStorage for session token
    ├── Token exists → GET /auth/me → restore AuthService state → proceed
    └── No token → redirect to /login

/login
└── GET /api/v1/users → show cards
    └── User taps card
        ├── firstLogin=true AND pin=null → show PIN modal → set or skip → POST /auth/login
        ├── pin exists → show PIN input → POST /auth/login
        └── pin=null AND firstLogin=false → POST /auth/login (no PIN needed)
            └── On success → save token → redirect to /
```

---

## Frontend Admin Visibility

All admin-only UI elements use a single Angular directive `*appAdminOnly` (or equivalent pipe/computed signal). The directive reads `AuthService.isAdmin()`. This replaces the Phase 1 hardcoded Francisco check.

```html
<!-- example usage -->
<button *appAdminOnly>Create Tournament</button>
<section *appAdminOnly>Bet History</section>
```

---

## Development Order

1. Backend: identity model migration (V16, V17, V18) + entity updates
2. Backend: `PinService`, `SessionFilter`, auth endpoints
3. Backend: data initializer for admin user
4. Backend: `WalletApi` endpoints
5. Backend: `EloService` + `OddsCalculator`
6. Backend: Market/Outcome auto-creation hook in EventService
7. Backend: Bet endpoints + resolution hook in finishEvent
8. Backend: Stats endpoints
9. Backend: WebSocket configuration + broadcasts
10. Backend: Phase 2 unit tests
11. Frontend: `AuthService` + refactor login screen
12. Frontend: PIN modal + self-registration flow
13. Frontend: header wallet chip + logout
14. Frontend: `AuthGuard` + route protection
15. Frontend: `BetsApi` + `MarketsApi` + `WalletApi` + `WebSocketService`
16. Frontend: Bet panel on Event Page
17. Frontend: Admin bet history on completed Event Page
18. Frontend: WebSocket replacing polling on Event Page
19. Frontend: Real standings table on Tournament Page
20. Frontend: Stats on Players Pages
