# Multi-Group Frontend Implementation Plan

Source of truth: `docs/MULTI_GROUP_BRIEFING.md`.

Audited branch: `feature/multi-groups`.

Scope: backend implementation only. This document references existing code and existing endpoints; it does not assume missing endpoints.

## 1. Backend Status Audit

| Briefing requirement | Status | Relevant files/endpoints | Notes |
|---|---|---|---|
| Group is the tenant and every business operation executes in a group context | PARTIAL | `CurrentUserContext`, `SessionFilter`, `Session`, `GroupAuthorizationServiceImpl` | Active group exists on `session.current_group_id` and is loaded into `CurrentUserContext`. Many services require it. Some scheduled/system jobs remain exempt as expected. |
| Users may participate in multiple groups | COMPLETE | `GroupMember`, `GroupMemberRepository`, `GET /api/v1/groups` | Membership is modeled with `group_member` and users can have multiple rows. |
| Group creation, listing, membership, switching | COMPLETE | `GroupController`, `GroupServiceImpl` | Existing endpoints: create, list mine, add member, switch active group. |
| `GroupMember.role` governs group-scoped permissions | COMPLETE | `GroupRole`, `GroupAuthorizationServiceImpl`, `docs/MULTI_GROUP_BRIEFING.md` | Confirmed values are `OWNER`, `ADMIN`, `MEMBER`. `OWNER` has extra privilege for granting another `OWNER`. |
| `User.userType` governs system-wide actions | COMPLETE | `CurrentUserContext.requireAdmin`, `CompetitionServiceImpl`, `FixtureSyncServiceImpl`, `OddsImportServiceImpl`, `EventServiceImpl`, `MarketController`, `TournamentServiceImpl` | Shared infrastructure mutations require `userType == ADMIN`. Existing REAL_FOOTBALL tournament attachment is intentionally group-scoped per briefing Option A. |
| Competition is global | COMPLETE | `Competition`, `CompetitionServiceImpl`, `Tournament.competition` | No group FK on `competition`. Create/update require system admin. |
| Team is global | COMPLETE | `Team`, `TeamServiceImpl` | No group FK on `team`. Create/update external id require system admin. |
| Player is group-scoped | COMPLETE | `Player.group`, `PlayerRepository`, `PlayerServiceImpl`, `V39__ADD_MULTI_GROUP_CORE.sql` | `player.group_id` is non-null after `V1000`. Player reads/writes use active group. |
| Same user may have one Player per group | COMPLETE | `uk_player_group_user`, `PlayerRepository.existsByGroupIdAndUserId` | Migration drops global user-player uniqueness and adds `(group_id, user_id)`. |
| Tournament has no direct group reference | COMPLETE | `Tournament`, `V39__ADD_MULTI_GROUP_CORE.sql` | `Tournament` has no `group_id`; group visibility is through `GroupTournament`. |
| `GroupTournament` is universal tenancy boundary | COMPLETE | `GroupTournament`, `GroupTournamentRepository`, `TournamentServiceImpl.attachCurrentGroup`, `GroupAuthorizationServiceImpl.requireTournamentAccess` | Tournament list and access checks use `group_tournament`. |
| `FIFA_MATCH` tournament has exactly one `GroupTournament`, enforced in service layer | COMPLETE | `TournamentServiceImpl.attachCurrentGroup` | Existing `GroupTournament` for a FIFA tournament causes `BusinessException`. No DB-level partial index, matching briefing's open question. |
| `REAL_FOOTBALL` tournament can be shared by multiple groups | COMPLETE | `TournamentRepository.findByCompetitionIdAndType`, `TournamentServiceImpl.create`, `GroupTournament` | Creating a REAL_FOOTBALL tournament for an existing competition attaches active group to the existing tournament. |
| Shared sporting truth: Tournament/Round/Event/Market/Outcome remain group-agnostic | COMPLETE | `Tournament`, `TournamentRound`, `Event`, `Market`, `Outcome` | No group FK added to these entities. Event/market data is shared across groups by tournament. |
| Tournament queries become group-aware via `GroupTournament` | COMPLETE | `TournamentRepository.findAllByGroupId`, `TournamentServiceImpl.findAll` | `GET /api/v1/tournaments` returns only active-group tournaments. |
| Event and market visibility is tenancy-checked | COMPLETE | `EventServiceImpl`, `MarketServiceImpl`, `MarketController` | Event/market reads and mutations call `requireTournamentAccess` or mutation helper. |
| `TournamentWallet` replaces global wallet for tournament economy | PARTIAL | `TournamentWallet`, `WalletServiceImpl`, `BetServiceImpl`, `TournamentWalletProvisioningServiceImpl` | Betting and wallet APIs use `TournamentWallet`. Legacy `Wallet` entity/table still exists and is not removed. |
| `TournamentWallet` unique by `(group_tournament_id, user_id)` | COMPLETE | `TournamentWallet`, `V39__ADD_MULTI_GROUP_CORE.sql` | Unique constraint exists. |
| `Transaction` references `TournamentWallet` directly | PARTIAL | `Transaction.tournamentWallet`, `V39__ADD_MULTI_GROUP_CORE.sql`, `BetServiceImpl`, `WalletServiceImpl` | New field exists and new transactions use it. Legacy `wallet_id` remains nullable, so migration is additive rather than replacement. |
| `BetSlip` uses `group_tournament_id` instead of `tournament_id` | PARTIAL | `BetSlip.groupTournament`, `V39__ADD_MULTI_GROUP_CORE.sql`, `BetServiceImpl` | Entity uses `groupTournament`, but DB migration only adds `group_tournament_id` and drops `tournament_id NOT NULL`; it does not drop old `tournament_id`. |
| `BetSlipItem` unchanged and inherits group through `BetSlip` | COMPLETE | `BetSlipItem`, `BetServiceImpl` | No group column added. Resolution uses pending items by event and resolves wallets through each slip's `groupTournament`. |
| Bet settlement fans out across groups and resolves wallet by `groupTournamentId + userId` | COMPLETE | `BetServiceImpl.resolveBetsForEvent`, `BetServiceImpl.cancelBetsForEvent` | Pending items are fetched by event, and each affected slip resolves its own tournament wallet. |
| Active group switching endpoint | COMPLETE | `POST /api/v1/groups/{id}/switch`, `GroupServiceImpl.switchGroup` | Switch validates membership and persists `session.current_group_id`. |
| Authentication alone is insufficient; session carries current group | COMPLETE | `Session.currentGroup`, `AuthServiceImpl.login`, `SessionFilter` | Login selects first group alphabetically when available and returns current group fields. |
| Tournament statistics scoped through `GroupTournament` | MISSING | No betting ranking/ROI/highest profit/aggressive bettor/hit-rate endpoints found | Sporting scoreboard exists, but betting statistics from the briefing are not implemented as API endpoints. |
| Create/join UX for shared REAL_FOOTBALL tournament | PARTIAL | `POST /api/v1/tournaments` | Authorization is decided: active group `OWNER` or `ADMIN` can attach to an existing REAL_FOOTBALL tournament. Discovery UX remains open. |
| GroupTournament settings schema | MISSING | None found | Briefing marks this as future extension, not current requirement. |

### Deviations, Bugs, and Technical Debt

| Item | Type | Reference | Frontend impact |
|---|---|---|---|
| `GroupResponseDTO` omits `active` and explicit `isCurrent` | API limitation | `GroupResponseDTO` | Frontend must infer current group from login response or last switch result. Member list is now available through `GET /api/v1/groups/{id}/members`. |
| `PlayerResponseDTO` omits `groupId` | API limitation | `PlayerResponseDTO` | Frontend should treat all returned players as belonging to active group. |
| Legacy `Wallet`, `wallet_id`, and `bet_slip.tournament_id` remain in schema | Technical debt | `V39__ADD_MULTI_GROUP_CORE.sql`, entities | Frontend should use new wallet response fields and ignore legacy concepts. |
| Tournament list response performs one `GroupTournament` lookup per tournament | Technical debt | `TournamentServiceImpl.toResponse` | Acceptable at current scale; could become an N+1 concern for large pages. |

### Deferred Backend Cleanup

These items are intentionally deferred because they require schema migrations or broader compatibility checks:

| Follow-up | Reason to defer | Suggested validation before implementation |
|---|---|---|
| Drop `bet_slip.tournament_id` | `BetSlip` now uses `group_tournament_id`, but old column still exists from legacy schema. | Confirm no repository, mapper, migration, report, or external consumer reads `bet_slip.tournament_id`. |
| Drop `transaction.wallet_id` | New transactions use `tournament_wallet_id`, but legacy rows/column may still exist. | Backfill/verify every transaction has `tournament_wallet_id`; confirm no global wallet transaction API remains. |
| Remove legacy `Wallet` entity/table | Tournament economy uses `TournamentWallet`, but legacy wallet code may still be referenced by historical docs or old data. | Confirm all wallet APIs, websocket payloads, and transaction flows use `TournamentWallet`. |
| Optimize tournament response `groupTournamentId` lookup | Current service mapping may do one lookup per tournament. | Add repository query/projection or join-fetch only if page size/group count makes this measurable. |

## 2. Implemented API Reference

Common behavior for all endpoints below:

| Concern | Behavior |
|---|---|
| Auth | All listed endpoints require `Authorization: Bearer <session UUID>` except `POST /api/v1/auth/login`. |
| Active group | Group-scoped endpoints require a session `currentGroup`; otherwise `401` with message `Grupo ativo nao selecionado.` |
| Errors | Errors use `ErrorResponseDTO` from `GlobalExceptionHandler` when thrown from services. `SessionFilter` auth failures return a smaller JSON `{status,error,message}`. |
| Common HTTP errors | `400` business/validation, `401` unauthorized, `402` insufficient funds, `404` not found, `409` duplicate/invalid state. |

### Auth and Active Group

#### `POST /api/v1/auth/login`

Purpose: create a session and initialize `currentGroup` to the first group found for the user.

Request DTO: `UserLoginRequestDTO` with `userId`, `pin`.

Response DTO: `UserLoginResponseDTO` with `token`, `id`, `name`, `userType`, `currentGroupId`, `currentGroupName`, `firstLogin`, `hasPin`.

Permissions: public.

Validation/errors: missing `userId` returns `400`; invalid credentials return `401`; unknown user returns `404`.

Example:

```json
{"userId":1,"pin":"1234"}
```

```json
{"token":"uuid","id":1,"name":"Francisco","userType":"ADMIN","currentGroupId":1,"currentGroupName":"ResenhaBET","firstLogin":false,"hasPin":true}
```

#### `GET /api/v1/auth/me`

Purpose: return authenticated user.

Request DTO: none.

Response DTO: `UserResponseDTO`.

Permissions: authenticated session.

Validation/errors: invalid/missing session returns `401`.

Example response: user DTO with id/name/user type fields from `UserMapper`.

#### `POST /api/v1/groups/{id}/switch`

Purpose: set active group for the current session.

Request DTO: none.

Response DTO: `GroupResponseDTO`.

Permissions: current user must be a member of `{id}`.

Validation/errors: non-member returns `401`.

Example response:

```json
{"id":5,"name":"Copa Gege","role":"ADMIN"}
```

### Groups

#### `POST /api/v1/groups`

Purpose: create a group, add current user as `OWNER`, and switch session to it.

Request DTO: `GroupRequestDTO`.

Response DTO: `GroupResponseDTO`.

Permissions: authenticated user.

Validation/errors: `name` required; duplicate name returns `409`.

Example:

```json
{"name":"FIFA da Empresa"}
```

```json
{"id":10,"name":"FIFA da Empresa","role":"OWNER"}
```

#### `GET /api/v1/groups`

Purpose: list groups where current user is a member.

Request DTO: none.

Response DTO: `List<GroupResponseDTO>`.

Permissions: authenticated user.

Validation/errors: invalid session returns `401`.

Example response:

```json
[{"id":1,"name":"ResenhaBET","role":"OWNER"},{"id":5,"name":"Copa Gege","role":"MEMBER"}]
```

#### `POST /api/v1/groups/{id}/members`

Purpose: add a user to a group and provision tournament wallets for that group's tournaments.

Request DTO: `GroupMemberRequestDTO` with `userId`, `role`.

Response DTO: `GroupResponseDTO` for added member.

Permissions: active group must equal `{id}` and current member role must be `OWNER` or `ADMIN`. Adding role `OWNER` requires current role `OWNER`.

Validation/errors: duplicate member returns `409`; missing user returns `404`; inactive group context mismatch returns `401`.

Example:

```json
{"userId":7,"role":"MEMBER"}
```

```json
{"id":1,"name":"ResenhaBET","role":"MEMBER"}
```

#### `GET /api/v1/groups/{id}/members`

Purpose: list members of the active group.

Request DTO: none.

Response DTO: `List<GroupMemberResponseDTO>` with `userId`, `userName`, `role`, `createdAt`.

Permissions: active group must equal `{id}` and current user must be a member.

Validation/errors: inactive group context mismatch returns `401`.

Example response:

```json
[{"userId":1,"userName":"Francisco","role":"OWNER","createdAt":"2026-01-01T10:00:00"}]
```

### Group-Scoped Players

#### `POST /api/v1/players`

Purpose: create player in active group.

Request DTO: `PlayerRequestDTO` with `name`.

Response DTO: `PlayerResponseDTO`.

Permissions: active group `OWNER` or `ADMIN`.

Validation/errors: name required; missing active group `401`.

Example: `{"name":"Jonas"}` -> `{"id":2,"name":"Jonas","active":true,"userId":null}`.

#### `GET /api/v1/players`

Purpose: list players in active group.

Request DTO: none.

Response DTO: `List<PlayerResponseDTO>`.

Permissions: authenticated user with active group.

Validation/errors: missing active group `401`.

Example response: `[{"id":1,"name":"Francisco","active":true,"userId":1}]`.

#### `GET /api/v1/players/{id}`

Purpose: get active-group player by id.

Request DTO: none.

Response DTO: `PlayerResponseDTO`.

Permissions: authenticated user with active group.

Validation/errors: player outside active group returns `404`.

Example response: `{"id":1,"name":"Francisco","active":true,"userId":1}`.

#### `PUT /api/v1/players/{id}`

Purpose: update active-group player name/status.

Request DTO: `PlayerUpdateRequestDTO`.

Response DTO: `PlayerResponseDTO`.

Permissions: active group `OWNER` or `ADMIN`.

Validation/errors: player outside active group returns `404`.

Example request: `{"name":"Francisco","active":true}`.

#### `PATCH /api/v1/players/{id}/link-user`

Purpose: link an existing user to an active-group player.

Request DTO: `LinkUserRequestDTO` with `userId`.

Response DTO: `PlayerResponseDTO`.

Permissions: active group `OWNER` or `ADMIN`.

Validation/errors: player already linked or user already linked to another player in active group returns `400`; user not found `404`.

Example request: `{"userId":4}`.

#### `GET /api/v1/players/{id}/stats?tournamentId={id}`

Purpose: get active-group sporting stats, optionally tournament-filtered.

Request DTO: query param optional `tournamentId`.

Response DTO: `PlayerStatsResponseDTO`.

Permissions: active group member; if `tournamentId` supplied, active group must have tournament access.

Validation/errors: player outside active group `404`; tournament outside active group `401`.

Example response: `{"playerId":1,"playerName":"Francisco","matchesPlayed":10,"wins":6,"losses":3,"draws":1,"goalsScored":20,"goalsConceded":12,"goalDifference":8,"points":19,"currentElo":1040}`.

### Group-Visible Tournaments

#### `POST /api/v1/tournaments`

Purpose: create tournament for active group, or attach active group to existing REAL_FOOTBALL tournament by `competitionId`.

Request DTO: `TournamentRequestDTO` with `name`, `format`, optional `type`, `competitionId`, `marketTypes`, `generationMode`, `hasThirdPlaceMatch`, `startDate`, `endDate`.

Response DTO: `TournamentResponseDTO`.

Permissions: active group `OWNER` or `ADMIN`. For new REAL_FOOTBALL shared infrastructure creation, also `userType == ADMIN`. Attaching the active group to an existing REAL_FOOTBALL tournament is group-scoped and allowed for active group `OWNER` or `ADMIN`.

Validation/errors: `competitionId` required for REAL_FOOTBALL; `competitionId` must be null for FIFA_MATCH; FIFA_MATCH already attached to another group returns `400`.

Example:

```json
{"name":"Copa 2026","format":"LEAGUE","type":"FIFA_MATCH","generationMode":"MANUAL"}
```

#### `GET /api/v1/tournaments`

Purpose: page tournaments visible to active group.

Request DTO: Spring pageable query params.

Response DTO: `Page<TournamentResponseDTO>`.

Permissions: authenticated user with active group.

Validation/errors: missing active group `401`.

Example response: page object whose `content` contains `TournamentResponseDTO`, including active-group `groupTournamentId` when available.

#### Tournament child endpoints

The following existing endpoints all require the active group to have `GroupTournament` access. Mutations require active group `OWNER` or `ADMIN` unless noted.

| Endpoint | Purpose | Request | Response | Notes |
|---|---|---|---|---|
| `GET /api/v1/tournaments/{id}/players` | List tournament players | none | `TournamentPlayersResponseDTO` | Read access. |
| `POST /api/v1/tournaments/{id}/players` | Add active-group player | `TournamentPlayerRequestDTO` | `TournamentPlayerResponseDTO` | Admin role required. |
| `POST /api/v1/tournaments/{id}/players/{playerId}/team` | Set player team | `PatchTournamentPlayerTeamRequestDTO` | `TournamentPlayerResponseDTO` | Admin role required. |
| `GET /api/v1/tournaments/{id}/rounds` | List rounds | none | `List<TournamentRoundResponseDTO>` | Read access. |
| `GET /api/v1/tournaments/{id}/scoreboard` | Sporting scoreboard | none | `TournamentScoreboardResponseDTO` | Read access. |
| `POST /api/v1/tournaments/{id}/start` | Start tournament | `StartTournamentRequestDTO` optional | `TournamentResponseDTO` | Admin role required. |
| `POST /api/v1/tournaments/{id}/advance-to-bracket` | Advance league-bracket | none | `TournamentResponseDTO` | Admin role required. |
| `POST /api/v1/tournaments/{id}/force-advance-to-bracket` | Cancel pending group-stage events and advance | none | `TournamentResponseDTO` | Admin role required. |
| `POST /api/v1/tournaments/{id}/sync-fixtures` | Sync REAL_FOOTBALL fixtures | none | `SyncResult` | `userType == ADMIN` plus tournament access. |
| `POST /api/v1/tournaments/{id}/sync-odds` | Sync REAL_FOOTBALL odds | none | `OddsImportResult` | `userType == ADMIN` plus tournament access. |
| `GET /api/v1/tournaments/tournament-group-config?playerCount=N` | Valid FIFA group-stage configs | query | `TournamentGroupConfigResponseDTO` | Existing helper endpoint; not tenant-specific. |

### Events and Markets

#### Event endpoints

All event reads require active-group tournament visibility. FIFA_MATCH event mutations require active group `OWNER` or `ADMIN`; REAL_FOOTBALL event mutations require `userType == ADMIN` plus active-group visibility.

| Endpoint | Purpose | Request | Response | Validation/errors |
|---|---|---|---|---|
| `POST /api/v1/events` | Create FIFA_MATCH event | `EventRequestDTO` | `EventResponseDTO` | REAL_FOOTBALL creation rejected; players must be active-group players in tournament. |
| `GET /api/v1/events?tournamentId=&status=` | List events | query | `List<EventResponseDTO>` | If no tournamentId, service filters to visible tournaments. |
| `GET /api/v1/events/{id}` | Get event | none | `EventResponseDTO` | Event outside active group returns `401`. |
| `POST /api/v1/events/{id}/score` | Update score | `EventUpdateRequestDTO` | `EventResponseDTO` | Event must be `IN_PROGRESS`. |
| `POST /api/v1/events/{id}/start` | Start event | none | `EventResponseDTO` | Closes market. |
| `POST /api/v1/events/{id}/end` | Finish event | none | `EventResponseDTO` | Resolves bets via event-completed listener. |
| `PATCH /api/v1/events/{id}/players` | Assign players to created event | `PatchEventPlayersRequestDTO` | `EventResponseDTO` | Players must belong to active group and tournament. |
| `PATCH /api/v1/events/{id}/penalties` | Record/finalize penalties | `FinishEventRequestDTO` | `EventResponseDTO` | Event must be `PENALTIES`. |

Example `EventRequestDTO`:

```json
{"tournamentId":1,"roundId":1,"playerHomeId":1,"playerAwayId":2,"gameDatetime":"2026-06-24T20:00:00"}
```

#### Market endpoints

| Endpoint | Purpose | Request | Response | Permissions |
|---|---|---|---|---|
| `GET /api/v1/markets/{eventId}` | Get event markets/outcomes | none | `List<MarketResponseDTO>` | Active group tournament visibility. |
| `POST /api/v1/markets/{eventId}/status` | Open/close event markets | `MarketStatusRequestDTO` | `List<MarketResponseDTO>` | REAL_FOOTBALL requires `userType == ADMIN`; FIFA_MATCH requires group admin. |

Validation: `status` must be `OPEN` or `CLOSED`.

Example request: `{"status":"OPEN"}`.

### Betting and Tournament Wallets

#### `POST /api/v1/bets`

Purpose: place bet in active group's `GroupTournament` economy.

Request DTO: `BetRequestDTO`.

Response DTO: `BetSlipResponseDTO`.

Permissions: authenticated active-group user with tournament access by active group.

Validation/errors: stake minimum `0.01`, all events must belong to `tournamentId`, market must be `OPEN`, duplicate pending bet on same event/groupTournament rejected, insufficient tournament wallet returns `402`.

Example:

```json
{"tournamentId":1,"stake":10.00,"items":[{"eventId":1,"marketId":1,"outcomeId":2}]}
```

#### `GET /api/v1/bets/me`

Purpose: list current user's bet slips in active group.

Request DTO: none.

Response DTO: `List<BetSlipResponseDTO>`.

Permissions: authenticated active-group user.

Example response includes `groupTournamentId` and `tournamentId`.

#### `GET /api/v1/bets?eventId={id}`

Purpose: list active-group bet slips for an event.

Request DTO: query param `eventId`.

Response DTO: `List<BetSlipResponseDTO>`.

Permissions: active group `OWNER` or `ADMIN` for event tournament.

#### `GET /api/v1/wallet?userId={id}&tournamentId={id}`

Purpose: get a user's tournament wallet in active group.

Request DTO: query params `userId`, `tournamentId`.

Response DTO: `WalletResponseDTO`.

Permissions: authenticated active-group session. Users may read their own wallet; reading another user's wallet requires active group `OWNER` or `ADMIN` for the tournament.

Validation/errors: no group tournament or wallet returns `404`.

Example response:

```json
{"userId":1,"tournamentId":1,"groupTournamentId":3,"balance":1000.00,"initialBalance":1000.00}
```

#### `POST /api/v1/wallet/deposit`

Purpose: deposit into one member's tournament wallet in active group.

Request DTO: `WalletDepositRequestDTO` with `userId`, `tournamentId`, `amount`.

Response DTO: `BigDecimal` new balance.

Permissions: active group `OWNER` or `ADMIN` for tournament; target user must be active-group member.

Validation/errors: amount required, max 2 decimals, greater than zero.

Example request: `{"userId":1,"tournamentId":1,"amount":100.00}`.

#### `POST /api/v1/wallet/deposit-all?tournamentId={id}&amount={amount}`

Purpose: deposit into all existing tournament wallets for active group's tournament.

Request DTO: query params.

Response DTO: empty body.

Permissions: active group `OWNER` or `ADMIN` for tournament.

Validation/errors: amount required, max 2 decimals, greater than zero.

### Competitions Relevant to REAL_FOOTBALL Visibility

| Endpoint | Purpose | Permissions | DTOs |
|---|---|---|---|
| `POST /api/v1/competitions` | Create global competition | `userType == ADMIN` | `CompetitionRequestDTO` -> `CompetitionResponseDTO` |
| `GET /api/v1/competitions` | List competitions | Authenticated session | `Page<CompetitionResponseDTO>` |
| `GET /api/v1/competitions/{id}` | Get competition | Authenticated session | `CompetitionResponseDTO` |
| `PATCH /api/v1/competitions/{id}` | Update competition | `userType == ADMIN` | `CompetitionRequestDTO` -> `CompetitionResponseDTO` |

Frontend use: REAL_FOOTBALL tournament creation/attachment needs an existing `competitionId`.

## 3. Frontend Implementation Plan

### Pages and Routes

| Task | Priority | Dependencies | Complexity |
|---|---|---|---|
| Add authenticated shell that blocks app content until login and active group are known | P1 | Auth service, group service | M |
| Add `/groups` page to list current user's groups and switch active group | P1 | `GET /groups`, `POST /groups/{id}/switch` | S |
| Add create group flow | P1 | `POST /groups` | S |
| Add group member list and add form | P2 | `GET /groups/{id}/members`, `POST /groups/{id}/members`, user lookup/listing strategy from existing user APIs | M |
| Update tournament list route to reflect active group only | P1 | `GET /tournaments` | S |
| Add REAL_FOOTBALL create/attach flow using competition selection | P2 | competitions API, `POST /tournaments` | M |
| Update player pages to treat players as active-group scoped | P1 | player APIs | S |
| Update betting pages to use tournament wallet and active-group bet history | P1 | bets/wallet APIs | M |
| Add permission-aware admin controls on tournament/event/market screens | P1 | auth userType, group role from group response | M |
| Add active group switch confirmation/reload flow | P1 | all state stores | M |

### Components

| Component | Purpose | Priority | Dependencies | Complexity |
|---|---|---|---|---|
| `ActiveGroupSwitcherComponent` | Display current group, list groups, switch group | P1 | `GroupService`, auth state | M |
| `GroupListPageComponent` | Full group list and current role | P1 | `GroupService` | S |
| `CreateGroupDialogComponent` | Create group and switch automatically from response | P1 | `GroupService.create` | S |
| `GroupMembersComponent` | Display group members and roles | P2 | `GroupService.listMembers` | S |
| `AddGroupMemberDialogComponent` | Add member by `userId` and role | P2 | Existing user APIs, `GroupService.addMember` | M |
| `PermissionDirective` or `HasGroupRoleDirective` | Hide group-admin controls | P1 | auth/group state | S |
| `SystemAdminDirective` | Hide system-admin controls | P1 | auth state `userType` | S |
| `TournamentVisibilityBadgeComponent` | Show active-group tournament context and REAL_FOOTBALL shared label from `type` | P2 | tournament DTO | S |
| `TournamentWalletSummaryComponent` | Show current user's wallet for selected tournament | P1 | wallet API | M |

### Services

| Service | Required methods | Priority | Dependencies | Complexity |
|---|---|---|---|---|
| `AuthService` | `login`, `me`, `logout`, expose `userType`, `currentGroupId`, `currentGroupName` | P1 | existing auth endpoints | M |
| `GroupService` | `listMine`, `listMembers`, `create`, `switchGroup`, `addMember` | P1 | groups API | S |
| `TournamentService` | Ensure all list/detail calls are invalidated on group switch | P1 | existing tournament API | M |
| `PlayerService` | No `groupId` parameter; all calls active-group scoped | P1 | player API | S |
| `WalletService` | `getTournamentWallet(userId,tournamentId)`, `deposit`, `depositAll` | P1 | wallet API | M |
| `BetService` | `placeBet`, `getMyBets`, `getBetsByEvent` active-group scoped | P1 | bets API | M |
| `CompetitionService` | list/get/create/update for REAL_FOOTBALL flow | P2 | competition API | S |

### State Management Changes

| Task | Priority | Dependencies | Complexity |
|---|---|---|---|
| Add `activeGroup` state with `{id,name,role}` | P1 | login response, switch endpoint | S |
| Add `groups` collection state | P1 | `GET /groups` | S |
| Clear or refetch group-scoped caches on group switch: players, tournaments, events, markets, bets, wallets, scoreboards | P1 | active group state | M |
| Keep global caches across group switch: competitions, teams, current user | P2 | service boundaries | S |
| Store permissions as derived selectors: `isSystemAdmin`, `isGroupOwner`, `isGroupAdmin`, `canManageGroup`, `canMutateFifaTournament`, `canMutateRealFootballShared` | P1 | auth/group state | M |

### Guards and Interceptors

| Task | Priority | Dependencies | Complexity |
|---|---|---|---|
| Ensure bearer token interceptor is applied to all non-login API requests | P1 | auth state | S |
| Add `AuthGuard` for all app routes except login | P1 | auth service | S |
| Add `ActiveGroupGuard` for group-scoped routes | P1 | auth/group state, `GET /groups` fallback | M |
| Add `SystemAdminGuard` for competition create/update and REAL_FOOTBALL shared controls | P2 | `userType` | S |
| Add `GroupAdminGuard` for group management/FIFA admin routes | P2 | active group role | S |
| Add HTTP `401` handling that redirects to login only for invalid session, and prompts group selection for `Grupo ativo nao selecionado.` | P1 | error interceptor | M |

### Required DTOs and Models

Implement TypeScript models matching backend names and enum values:

| Model | Fields |
|---|---|
| `GroupResponse` | `id`, `name`, `role: 'OWNER' \| 'ADMIN' \| 'MEMBER'` |
| `GroupMemberResponse` | `userId`, `userName`, `role`, `createdAt` |
| `GroupRequest` | `name` |
| `GroupMemberRequest` | `userId`, `role` |
| `LoginResponse` | `token`, `id`, `name`, `userType`, `currentGroupId`, `currentGroupName`, `firstLogin`, `hasPin` |
| `TournamentResponse` | fields from `TournamentResponseDTO`, including active-group `groupTournamentId` |
| `PlayerResponse` | `id`, `name`, `active`, `userId`; no `groupId` available |
| `WalletResponse` | `userId`, `tournamentId`, `groupTournamentId`, `balance`, `initialBalance` |
| `BetSlipResponse` | `id`, `userId`, `tournamentId`, `groupTournamentId`, `stake`, `combinedOdd`, `potentialReturn`, `status`, `createdAt`, `items` |

### User Flows

Active group switching flow:

1. Login receives `currentGroupId/currentGroupName` if user belongs to at least one group.
2. Load `GET /api/v1/groups` and match the active group to obtain `role`.
3. If no active group exists, route to `/groups` and allow creating a group. Do not call group-scoped endpoints.
4. On switch, call `POST /api/v1/groups/{id}/switch`.
5. Replace active group state with response.
6. Clear group-scoped caches and navigate to a safe group-scoped landing page, such as tournaments.
7. Refetch players, tournaments, bets, and wallet summaries for the new active group.

Tournament visibility flow:

1. `GET /api/v1/tournaments` returns only tournaments visible to active group.
2. Use `type === 'REAL_FOOTBALL'` to label shared sporting truth; do not imply independent fixtures/markets per group.
3. For REAL_FOOTBALL creation, load competitions and submit `type: 'REAL_FOOTBALL'`, `competitionId`, `format`, and market types.
4. If backend returns an existing tournament, treat it as successfully attached/visible to the active group and store the returned `groupTournamentId`.
5. Hide sync fixtures/odds unless `userType === 'ADMIN'`.
6. Hide FIFA_MATCH mutation controls unless active role is `OWNER` or `ADMIN`.

Permission handling:

| UI action | Frontend check from implemented backend |
|---|---|
| Create group | Authenticated |
| Switch group | Membership in `GET /groups` list |
| Add member | Active role `OWNER` or `ADMIN`; adding `OWNER` only for `OWNER` |
| Create FIFA_MATCH tournament | Active role `OWNER` or `ADMIN` |
| Create new REAL_FOOTBALL tournament | Active role `OWNER` or `ADMIN` and `userType === 'ADMIN'` |
| Attach to existing REAL_FOOTBALL via create endpoint | Active role `OWNER` or `ADMIN`; this is intentionally group-scoped |
| Mutate FIFA_MATCH events/markets | Active role `OWNER` or `ADMIN` |
| Mutate REAL_FOOTBALL events/markets/sync | `userType === 'ADMIN'` |
| Place bets | Authenticated active-group member with wallet balance |
| Read own bets | Authenticated active-group member |
| Read own tournament wallet | Authenticated active-group member |
| Read another member's tournament wallet | Active role `OWNER` or `ADMIN` for that tournament |

## 4. Frontend Checklist

1. Add TypeScript enums for `GroupRole` (`OWNER`, `ADMIN`, `MEMBER`) and `UserType` (`ADMIN`, `USER`).
2. Extend login model to include `currentGroupId` and `currentGroupName`.
3. Add `GroupService` with `listMine`, `listMembers`, `create`, `switchGroup`, `addMember`.
4. Add active group state derived from login plus `GET /groups` role lookup.
5. Add token interceptor for all API requests except login.
6. Add `AuthGuard` and protect all app routes except login.
7. Add `ActiveGroupGuard` for players, tournaments, events, markets, bets, and wallet routes.
8. Build `ActiveGroupSwitcherComponent` in the app shell.
9. Implement `/groups` page with list, switch, create, and member list actions.
10. On group switch, clear group-scoped caches and refetch current route data.
11. Update player screens to remove any global-player assumptions and rely on active group.
12. Update tournament list to refetch on active group changes.
13. Implement tournament creation for FIFA_MATCH with group-admin gating.
14. Implement REAL_FOOTBALL tournament create/attach flow using competitions; require system admin only when creating new shared infrastructure.
15. Add labels/copy explaining REAL_FOOTBALL events/markets are shared sporting truth while bets/wallets are group-specific.
16. Update event and market controls with role checks based on tournament type.
17. Update betting flow to call `POST /bets` with `tournamentId` and rely on active group context.
18. Add tournament wallet summary using `GET /wallet?userId=currentUserId&tournamentId=...`.
19. Update bet history to use `GET /bets/me` and clear/refetch on group switch.
20. Add admin-only event bet list using `GET /bets?eventId=...` only for group admins.
21. Add deposit UI only for group admins and active-group members.
22. Handle `401 Grupo ativo nao selecionado.` by routing to `/groups` instead of logging out.
23. Handle `401 Token de sessao invalido ou expirado.` by clearing auth and routing to login.
24. Use `GET /groups/{id}/members` to display active-group membership.
25. Use `groupTournamentId` from tournament, wallet, and bet responses for active-group tournament context.
26. Test switching between two groups verifies different players, tournaments, bets, and wallet balances.
27. Test a user with `OWNER`, `ADMIN`, and `MEMBER` roles sees the expected controls.
28. Test system admin vs group admin behavior for REAL_FOOTBALL sync/event/market controls.
29. Test joining/creating REAL_FOOTBALL competition flow against current backend behavior.
30. Document remaining UX that requires missing backend support, especially explicit shared tournament discovery.
