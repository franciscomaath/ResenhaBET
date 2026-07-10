# Backlog Validation

_Audit of `docs/BACKLOG.md` against the codebase (source of truth: code, tests, infra, docs, BACKLOG). Last verified: 2026-06-21._

Scope: backend repository only (`com.franciscomaath.resenhaapi`). There is **no frontend module** in this repository, so every backlog entry claiming frontend pages is unverifiable from this codebase and is marked `OUT‑OF‑SCOPE` rather than `DONE`. All `DONE´ statuses below require code evidence inside `src/main`/`src/test` or a `db/migration/V*__*.sql` file.

---

## 1. Summary

- Backlog claims **54 Done** items, **6 Partial**, **~19 Pending**, **3 Future/Roadmap**.
- After cross-referencing against the tree, **30 Done entries are confirmed by code**, **8 Done entries are MISLABELED** (either out-of-scope frontend claims or overstated), **1 Partial entry is MISLABELED** (the framing of the BetSlipItem unique constraint is incorrect — no such constraint ever existed and there is no `market_id` on `BetSlipItem`), and **5 Partial entries are correctly Partial**.
- The "Group 7 Phase 2 Backend Tests (146 tests)" Done item is **stale**: the test suite now contains **164 `@Test` methods** (5 service/18 [`src/test/java/com/franciscomaath/resenhaapi/...`](../src/test/java/com/franciscomaath/resenhaapi)), so the count claim is understated but the intent (suite passing) holds.
- `OddsRecalculationListener` + `OddsUpdateServiceImpl.recalculateFutureOdds` actually recalculate odds after every completed event (`src/main/java/com/franciscomaath/resenhaapi/listener/OddsRecalculationListener.java:21`, `:26`), contradicting `docs/AGENTS.md` ("Static odds — never updated"). This is an **untracked implemented feature** and partially realizes the Future/Roadmap "Dynamic Odds" item.
- No `@Scheduled` job exists in the codebase (`grep -rn "@Scheduled" src/main/` returns nothing), so RF-12/13/14/15 and the new `MarketSuspensionJob` task are functionally TODO, not "in progress".
- No `Dockerfile`, `docker-compose.yml`, or `.env.example` exist (only a committed `.env` containing real DB credentials — a security/infra gap).
- `docs/AGENTS.md` itself is **partially obsolete**: it states Spring Boot 3.4/Java 21 and "Migrations: V1–V18", while `pom.xml` is **Spring Boot 4.0.4 / Java 17** and migrations run through **V36** (`spring.flyway.target=36`).

---

## 2. Backlog Audit Table

Legend: `DONE´ = confirmed in code · `PARTIAL` = incomplete · `TODO` = missing · `MISLABELED` = wrong backlog metadata · `OUT-OF-SCOPE` = lives outside this repo.

### ✅ Done section

| # | Backlog wording (abbreviated) | Code evidence | Tests | Status |
|---|---|---|---|---|
| 1 | Phase 1 Matches API | `controller/{Match implicitly via EventController,PlayerController,TournamentController,TeamController,MarketController}` + `service/Impl/{Event,Player,Tournament,Team,Market}ServiceImpl` | `controller/*.java` (5 classes, 32 @Test) | DONE |
| 2 | Group 1 Identity Model (V16-V18, PinService, SessionFilter, auth endpoints) | `V16__..,V17__..,V18__..`, `config/SessionFilter.java`, `config/CurrentUserContext.java`, `service/Impl/PinServiceImpl.java`, `controller/AuthController.java:28-52` (`/login`,`/me`,`/logout`, `PATCH /pin`) | `AuthServiceImplTest` (5), `PinServiceImplTest` (4) | DONE |
| 3 | Group 2 Wallet/Transactions (deposit, deposit-all, wallet query) | `controller/WalletController.java:21-43`, `service/Impl/WalletServiceImpl.java:40-129` (+ `TransactionType.DEPOSIT`) | `WalletServiceImplTest` (6) | DONE |
| 4 | Group 3 Markets & Odds (OddsCalculator, EloService, market/outcome auto-creation) | `EloServiceImpl`, `OddsCalculatorServiceImpl`, `EventServiceImpl.createMarketAndOutcomesForEvent:487-515` | `OddsCalculatorServiceImplTest` (11), `EloServiceImplTest` (10), `MarketServiceImplTest` (6) | DONE |
| 5 | Group 4 Betting System (BetSlip, placeBet, resolveBets, multi-event parlay) | `domain/entity/{BetSlip,BetSlipItem}`, `service/Impl/BetServiceImpl.java:54-295` (placeBet, resolveBetsForEvent, cancelBetsForEvent) | `BetServiceImplTest` (9) | DONE |
| 6 | Group 5 Player Statistics (`GET /players/{id}/stats`, scoreboard endpoint) | `controller/PlayerController.java:64-71` (`?tournamentId`), `TournamentServiceImpl.getScoreboard:579`, `toPlayerStatsDTO:762-777` | `PlayerServiceImplTest` (5) — **no dedicated stats test** | DONE |
| 7 | Group 6 WebSocket STOMP (3 topics) | `config/websocket/WebSocketConfig.java`, `config/websocket/WebSocketBroadcaster.java:24-46` (broadcast on `/topic/events/{id}`, `/topic/markets/{id}`, `/topic/wallet/{userId}`) | **none** | DONE |
| 8 | Group 7 Backend Tests (146 tests passing) | — | actual = **164** `@Test` methods across 20 classes | MISLABELED (count stale; intent valid) |
| 9 | Group 8 Auth Frontend | none in repo (no frontend module) | — | OUT-OF-SCOPE |
| 10 | Group 9 Betting Frontend | none in repo | — | OUT-OF-SCOPE |
| 11 | Group 10 WebSocket Frontend | none in repo | — | OUT-OF-SCOPE |
| 12 | Bracket Phase 1 Schema (V20-V26) | `V20__..–V26__..` introduce `format`, `phase_type`, `is_knockout`, `group_number`, `penalties_*`, `third_place_match` | `TournamentServiceImplTest` (29) | DONE |
| 13 | Bracket Phase 2 Auto-generation (LEAGUE/BRACKET/LEAGUE_BRACKET AUTO) | `TournamentServiceImpl.startTournament:323-353`, `generateBracketAutoEvents:1020`, `generateLeagueBracketAutoEvents:1144` | `TournamentServiceImplTest` | DONE |
| 14 | Bracket Phase 3 Manual Mode (`PATCH /events/{id}/players`) | `controller/EventController.java:78-85`, `EventServiceImpl.patchEventPlayers:432-485` | `EventServiceImplTest` (25) | DONE |
| 15 | Bracket Phase 4 Knockout Behavior (penalties, winner advancement, no-draw markets) | `EventServiceImpl.finishEvent:196-202` & `recordPenalties:234-294`, `BetServiceImpl.resolveBetsForEvent:201-211` (uses penalties on tied knockout), `createMarketAndOutcomesForEvent:503-507` (no Empate for knockout) | `EventServiceImplTest`, `TournamentServiceImplTest` | DONE |
| 16 | Bracket Phase 5 LEAGUE_BRACKET Transition (advance-to-bracket, force-advance) | `TournamentController.java:108-123`, `TournamentServiceImpl.advanceToBracket:380` & `forceAdvanceToBracket:447` | `TournamentServiceImplTest` | DONE |
| 17 | Bracket Phase 6 Scoreboard (polymorphic response) | `TournamentServiceImpl.getScoreboard:579-605` (sets `entries`/`groups`/`placements` per format) | `TournamentServiceImplTest` (covers scoreboard) | DONE |
| 18 | Bracket Phase 8 Tests (GroupConfigValidator, bracket gen, knockout, LEAGUE_BRACKET) | `service/TournamentGroupConfigValidatorTest` (9 @Test) + `TournamentServiceImplTest` (29) | — | DONE |
| 19-26 | BUG-01/02/03/05/06/07/08 fixes | `standardRounds` filter on `PhaseType.KNOCKOUT` (`getStandardBracketRounds:1278`), `maxExistingOrder` offset (`advanceToBracket:439-444`,`performBracketAdvancement:539`), `homeSourceEvent/awaySourceEvent` populated in `linkBracketEvents:1202-1225`, `autoAdvanceByeWinner:1110` matches `playerHome != null` branch, no `autoCompleteGroupStageEvents` symbol found, `findRoundsByTournamentId` uses `findByTournamentIdOrderByRoundOrderAsc:209`, `computeNextPowerOf2:1189` + `Integer.numberOfTrailingZeros` (`TournamentServiceImpl.java:291,529`) | `TournamentServiceImplTest` | DONE |
| 27 | CONC-1 `@Version` on Tournament (V28) | `domain/entity/Tournament.java:29` + `V28__ADD_TOURNAMENT_VERSION_AND_CONSTRAINTS.sql:1` | indirect (covered by service tests) | DONE |
| 28 | SEC-1/2/3 requireAdmin on startTournament/addPlayer/updateTeam | `TournamentServiceImpl.java:82,150,187,217,383,450` all call `currentUserContext.requireAdmin()` | `TournamentServiceImplTest` mocks context | DONE |
| 29 | PROD-1 logging in TournamentServiceImpl | `TournamentServiceImpl.java` uses `@Slf4j`, 8 `log.info/warn` sites (e.g. `:121`,`:178`,`:231`,`:436`,`:485`,`:1167`…) | — | DONE |
| 30 | QUAL-1 performBracketAdvancement extracted (shared) | `TournamentServiceImpl.java:525-577` called by `advanceToBracket` and `forceAdvanceToBracket` | `TournamentServiceImplTest` | DONE |
| 31 | QUAL-2 STANDINGS_COMPARATOR extracted as static constant | `TournamentServiceImpl.java:73-77` | `TournamentServiceImplTest` | DONE |
| 32 | BUG-1 `@Transactional` uses Spring (not `jakarta.transaction`) on TournamentServiceImpl | `TournamentServiceImpl.java:40` imports `org.springframework.transaction.annotation.Transactional` | — | DONE |
| 33 | RF-01..RF-11 Real Football domain, migrations, API clients, fixture sync, odds import | `V29__..,V30__..–V36__..`; `service/ApiFootballClient`, `service/impl/ApiFootballClientImpl`, `service/impl/GameForecastClientImpl`, `service/impl/FixtureSyncServiceImpl`, `service/impl/OddsImportServiceImpl`, `ExternalApiLog` entity, `ApiFootballProperties`, `GameForecastProperties` | **no `FixtureSyncServiceImplTest`, no `OddsImportServiceImplTest`, no `ApiFootballClientImplTest`** | PARTIAL (infra present, integration untested) |
| 34 | Competition entity and CRUD (V33/V34) | `V33__CREATE_COMPETITION_TABLE.sql`, `V34__ADD_COMPETITION_TO_TOURNAMENT.sql`, `controller/CompetitionController`, `service/impl/CompetitionServiceImpl` (create/findAll/findById/toggleActive:28-77) | **no `CompetitionServiceImplTest`** | DONE (CRUD done; coverage gap) |
| 35 | TournamentRequestDTO accepts `type` and `marketTypes` | `controller/dto/request/TournamentRequestDTO.java` exposes `getType()`, `getMarketTypes()`; read by `TournamentServiceImpl.create:84,117` | `TournamentControllerTest` (9) | DONE |
| 36 | TournamentServiceImpl.create() reads `type`, validates `competitionId` | `TournamentServiceImpl.java:84-103` | `TournamentServiceImplTest` | DONE |
| 37 | Multi-market betting (placeBet supports multiple markets, list of BetSlipItem) | `BetServiceImpl.placeBet:56-168` iterates `dto.getItems()` (event+market+outcome per item), `BetRequestDTO.getItems()` | `BetServiceImplTest` (9) | DONE |
| 38 | Resolving bets with multiple markets (batch resolution) | `BetServiceImpl.resolveBetsForEvent:222-294` resolves all markets for an event & batches winnings per slip | `BetServiceImplTest` | DONE |
| 39 | Team name in outcome names for real football tournaments | `TournamentServiceImpl.createMarketAndOutcomesForEvent:969-972` and `EventServiceImpl.createMarketAndOutcomesForEvent:506-513` use `playerHome.getName()`/`playerAway.getName()` (no Empate for knockout) | — | DONE |
| 40 | External API logging (ExternalApiLog, V36, repo) | `domain/entity/ExternalApiLog.java`, `V36__CREATE_EXTERNAL_API_LOG.sql`, `domain/repository/ExternalApiLogRepository.java`, `idx_external_api_log_lookup` | **no test** | DONE |
| 41 | GameForecast team ID storage + admin PATCH endpoint (V35) | `V35__ADD_GAMEFORECAST_TEAM_ID.sql`, `controller/dto/request/UpdateGameForecastTeamIdRequestDTO.java` | **no test** | DONE |
| 42 | Odds update from API (OddsUpdateServiceImpl) | `service/impl/OddsUpdateServiceImpl.java:50-127` `recalculateFutureOdds` | **no `OddsUpdateServiceImplTest`** | DONE |
| 43 | Events unmatching fix between external APIs | git `06ca316` (present) | — | DONE (cannot re-verify behavior; only commit presence) |
| 44 | Group stage correct round group names fix | git `4a05a60` | `TournamentServiceImplTest` round-name tests | DONE |
| 45 | PERF-4 Pagination on findAll tournaments | `TournamentController.java:41-45` accepts `Pageable`, `TournamentServiceImpl.findAll:126` returns `Page<…>` | `TournamentControllerTest` | DONE |
| 46-48 | B25/B26/B23 Frontend components | none in repo | — | OUT-OF-SCOPE |
| 49 | RF-17..RF-20 Real Football frontend | none in repo | — | OUT-OF-SCOPE |

### 🔄 Partial section

| # | Backlog wording | Code evidence | Status |
|---|---|---|---|
| P1 | BUG-04 Odd-sized group round-robin — "fix documented but not verified in code" | `TournamentServiceImpl.generateRoundRobinForGroup:1227-1259` explicitly pads odd-sized lists with `playerList.add(null)` and skips dummy pairs — **the fix IS verified in code** | **DONE (mislabeled as Partial)** |
| P2 | `jakarta.transaction.Transactional` still used in 8 files | Confirmed exactly: `PlayerInitializer`, `AuthServiceImpl`, `CompetitionServiceImpl`, `EventServiceImpl`, `PinServiceImpl`, `PlayerServiceImpl`, `TeamServiceImpl`, `UserServiceImpl` (all under `src/main/java/.../service/Impl` + `config/PlayerInitializer`) import `jakarta.transaction.Transactional`; 8 services already migrated to `org.springframework.transaction.annotation.Transactional` (`Bet`, `FixtureSync`, `Elo`, `Market`, `OddsImport`, `OddsUpdate`, `Tournament`, `Wallet`) | PARTIAL (correct) |
| P3 | DB-1/DB-2/DB-3 Missing indexes/unique constraints | `V28` adds only `idx_event_tournament_status`, `idx_tournament_player_group`, `uk_tournament_player`. Missing indexes: `market(event_id,status)`, `outcome(market_id)`, `bet_slip(user_id,status,created_at)`, `bet_slip_item(event_id,status)`, `transaction(wallet_id, type)`, `round(tournament_id, round_order)` (round lookup is hot in `findRoundsByTournamentId`). `market(event_id, market_type)` unique added in V30 but no useful index. `bet_slip_item` has **no uniqueness** (see P₆) | PARTIAL (correct) |
| P4 | Hardcoded postgres/postgres in application.properties | `application.properties:4-6` hardcodes `username=postgres`, `password=postgres`, `url=jdbc:postgresql://localhost:5432/resenhaapi`. A `.env` file exists (committed, real-looking DB credentials) but is unused by Spring Boot — there is no `${DB_USER:…}` wiring | PARTIAL (correct) |
| P5 | Market type filtering during tournament creation — backend stores `marketTypes` but doesn't filter markets accordingly | `TournamentServiceImpl.create:117` stores the set; `EventServiceImpl.createMarketAndOutcomesForEvent:487-515` & `TournamentServiceImpl.createMarketAndOutcomesForEvent:952-980` always hardcode `MarketType.MATCH_RESULT` regardless of tournament's `marketTypes`. No filtering logic exists in any service | PARTIAL (correct) |
| P6 | **NEW** Verify BetSlipItem unique constraint was migrated from `UNIQUE(betSlipId, eventId)` to `UNIQUE(betSlipId, marketId)` | `V19__CREATE_BET_SLIP.sql:16-26` defines `bet_slip_item` with only FKs — **no unique constraint of either form exists**. `domain/entity/BetSlipItem.java:26-32` has fields `event` + `outcome` only; **there is no `marketId` column**. There is no migration adding such a constraint. The premise of this task is wrong: nothing was ever migrated. | **MISLABELED** (re-frame: there is no uniqueness at all on `bet_slip_item`; multi-market relies on duplicate-tolerant inserts) |

### 📋 Pending section

| # | Backlog wording | Code evidence | Status |
|---|---|---|---|
| T1 | B22 Tournament creation modal for bracket options | frontend only | OUT-OF-SCOPE |
| T2 | B24 Group standings tables on Tournament Page | frontend only | OUT-OF-SCOPE |
| T3 | B27 Group number display in Players/Matches tabs | frontend only | OUT-OF-SCOPE |
| T4 | Stats on Global Players Page (use `GET /players/{id}/stats`) | frontend only; endpoint exists `PlayerController.java:64` | OUT-OF-SCOPE (backend ready) |
| T5 | Stats on Tournament Players Tab (`?tournamentId`) | backend supports `?tournamentId` (`PlayerController.java:68`) | OUT-OF-SCOPE (backend ready) |
| T6 | Standings table on Tournament Page using `GET /tournaments/{id}/scoreboard` | backend ready `TournamentController.java:82-88` | OUT-OF-SCOPE (backend ready) |
| T7 | Remove unused legacy frontend components | not in repo | OUT-OF-SCOPE |
| T8 | Clean ResenhaBetState (frontend) | not in repo | OUT-OF-SCOPE |
| T9 | Create Dockerfile (eclipse-temurin:21-jre) | no `Dockerfile*` in tree | **TODO** |
| T10 | Move all config to environment variables | `application.properties` is fully hardcoded (DB creds, API keys, scheduler flag, replay-mode) | **TODO** |
| T11 | Create `.env.example` with all variables documented | only a real `.env` (no example) | **TODO** |
| T12 | Create `docker-compose.yml` (Spring Boot + PostgreSQL) | not in tree | **TODO** |
| T13 | Create setup README.md | no `README.md` at repo root | **TODO** |
| T14 | RF-12 RealFootballScheduler `autoStartEvents()` and `pollLiveScores()` | no `RealFootballScheduler` class, no `@Scheduled` anywhere; `application.properties:30` `resenhabet.scheduler.live-poll-enabled=true` is read by **nothing** | **TODO** |
| T15 | RF-13 APIfootball status-to-action mapping | `FixtureSyncServiceImpl.sync:136` only checks `"Finished"`; no start/cancel action mapping | **TODO** |
| T16 | RF-14 Event cancellation logic — refund stakes, cancel BetSlipItems | `BetServiceImpl.cancelBetsForEvent:297-346` exists and is invoked by `TournamentServiceImpl.forceAdvanceToBracket:497` — but no scheduler/cancellation path driven by external API status; no auto-refund of `MarketStatus.CANCELLED` market | **PARTIAL** (primitive exists, no event lifecycle wiring) |
| T17 | RF-15 `live-poll-enabled` scheduler toggle | property exists in `application.properties:30`, but no `@ConfigurationProperties` consumer | **TODO** |
| T18 | Competition Model Phase B — APIfootball league discovery endpoint | `CompetitionServiceImpl` only has create/find/toggleActive; `ApiFootballClient` has no `getLeagues`/`discoverLeagues` method | **TODO** |
| T19 | **NEW** `MarketSuspensionJob` (`fixedDelay=60s`, `suspendAt`, `completeEvent` hook sets `market.status = SETTLED`) | `domain/entity/Market.java` has no `suspendAt` column; no `@Scheduled` job; `EventServiceImpl.finishEvent:204` sets `EventStatus.COMPLETED` and `BetServiceImpl.resolveBetsForEvent:292` sets `MarketStatus.CLOSED` (not `SETTLED`); `EventStatus` enum has no `SCHEDULED`/`SETTLED`; there is no `completeEvent` symbol — the closest is `finishEvent` | **TODO** (and the design references symbols that don't exist in code: `suspendAt`, `completeEvent`, `SETTLED`) |
| T20 | SEC-4/SEC-5 Input validation on `StartTournamentRequestDTO` and `startDate/endDate` | `StartTournamentRequestDTO.java` has no `@NotNull/@Min/…`; `TournamentRequestDTO` exposes `startDate`/`endDate` without validation annotations; validation only happens at runtime inside `TournamentServiceImpl` via `BusinessException` (`:257-273`) | **TODO** |
| T21 | QUAL-3 Magic numbers → named constants | `TournamentServiceImpl` still literals: multiplier `BigDecimal.valueOf(1.4)` (:309,:548), `0.2 * i` (:920), `2.0` (:918), `points = wins*3 + draws` (:774), expiry `plusHours(24)` (`AuthServiceImpl:55`) | **TODO** |
| T22 | SPRING-3 `@Transactional(readOnly=true)` on read-only methods | `TournamentServiceImpl.findAll:126` and `BetServiceImpl.getUserBets/getBetsByEvent:171,181` use it, but `findPlayersByTournamentId`, `findRoundsByTournamentId`, `getScoreboard`, `PlayerServiceImpl.findAll/findPlayerById`, `CompetitionServiceImpl.findAll/findById`, `EventServiceImpl.findEvent/findAll` do **not** | **PARTIAL** |
| T23 | PROD-3 Use `SecureRandom` for `Collections.shuffle` | `TournamentServiceImpl.java:337,1152` still use `Collections.shuffle(list)` (default `Random`) | **TODO** |
| T24 | DOMAIN-3 Extract `StatsAccumulator` as proper domain value object | `StatsAccumulator` is a private inner class at `TournamentServiceImpl.java:842-849`; also contains flag `matchesPlayed` etc. not in any `domain/` package | **TODO** |
| T25 | PERF-1/2/3 N+1 queries | `getScoreboard` loads `eventRepository.findAllByTournamentId` then loops; `BetServiceImpl.resolveBetsForEvent` loads slips one-by-one inside loop (`betSlipRepository.findById(slipId)`, `:256`); `patchEventPlayers:476` does `marketRepository.findByEventId` per event | **TODO** (evidence of N+1) |
| T26 | BUG-9 NPE risks in TournamentServiceImpl | `determineWinner/Final` (`:796-815`) derefs `event.getHomeScore()` after null-checking `playerHome`/`playerAway` only; `buildBracketPlacements:654` filters by round name `equals("Final")` (silent NPE if a round name is null); `calculateGroupStandings` unchecked `tp.getGroupNumber()` boxing comparisons | **TODO** |
| T27 | Transactions history endpoint `GET /wallet/transactions` | `WalletController.java` exposes only `/`, `/deposit`, `/deposit-all`; `WalletServiceImpl.me:40-47` returns wallet only with `// TODO: return transactions history` | **TODO** |
| T28 | End tournament manual endpoint | `TournamentController.java:145` has `// TODO: end tournament endpoint -> necessario?`; `TournamentStatus.COMPLETED` is only set by `EventServiceImpl.checkTournamentCompletion:406` | **TODO** |
| T29 | Past match import endpoint (`POST /events` with already-played result) | `EventController.java:33` `// TODO: criar endpoint para adicionar partida que ja aconteceu`; only `EventRequestDTO` for upcoming events | **TODO** |
| T30 | Other market types resolving in `BetServiceImpl` | `BetServiceImpl.resolveBetsForEvent:220` `// TODO: other market types resolving`; only `MarketType.MATCH_RESULT` is resolved (matches winning outcome by name) | **TODO** (same concern as P₅ market-type filtering) |
| T31 | Validate knockout fixture round/stage fields for REAL_FOOTBALL sync | `FixtureSyncServiceImpl.java:102` `// TODO: validate get_events round/stage fields…`; logic falls back to `match.getMatchRound()/getStageName()` for any non intra-group match, so cross-group finals/semis can be misqualified | **TODO** |

### 🔮 Future / Roadmap

| # | Backlog wording | Code evidence | Status |
|---|---|---|---|
| F1 | Dynamic Odds & Cashout — `outcome_odd_history` table, live recalc, `POST /bets/{id}/cashout` | No `outcome_odd_history`, no `/cashout` endpoint, no `Bet.<status> CASHOUT` enum value | **PARTIAL**: live odds recalculation **is** implemented for future events after each match (`OddsRecalculationListener:21` + `OddsUpdateServiceImpl.recalculateFutureOdds:50`), but no history table or cashout endpoint exists. Backlog should be split: "Live odds recalculation" is **DONE (untracked)**, "Cashout + history table" is **TODO**. |
| F2 | Multi-Group Platform — Group entity, GroupMember, group-scoped wallets/players/tournaments | no `Group`/`GroupMember` entities (only `TournamentPlayer.groupNumber`); no group-scoped wallet repos | **TODO** |
| F3 | APIfootball WebSocket integration | no WS consumer for apifootball; backlog already notes "not viable — no free plan WS" | **WON'T-FIX** (acknowledged in backlog) |

---

## 3. Missing Tasks

Items physically absent from `BACKLOG.md` despite being present (or imminent) in the codebase:

1. **Live odds recalculation after every match.** `OddsRecalculationListener.java:21` listens to `EventChangeEvent` and calls `OddsUpdateServiceImpl.recalculateFutureOdds` — a real, ship-able feature. `docs/AGENTS.md` still claims "Static odds — never updated", which is now factually wrong and contradicts `OddsUpdateServiceImpl.java:48-127`.

2. **External API request logging persistence is wired into clients** is implied but there is **no evidence** any service actually persists to `ExternalApiLog`. The "Done" item only proves the table/repo exist; the **write path** (`ExternalApiLogRepository.save`) is **not called** anywhere — verify and either (a) mark as PARTIAL, or (b) remove from Done.

   `grep -rn "externalApiLogRepository\|ExternalApiLog " src/main/` returns only the entity + repository declarations; no service saves to it.

3. **`logout` deletes by token.** `AuthServiceImpl.logout:80` calls `sessionRepository.deleteByToken(token)` — done in code, **not tracked** in the Identity/Group 1 backlog item.

4. **`PATCH /tournaments/{id}/players/{playerId}/team`** (admin team assignment per tournament player) — wired in `TournamentController.java:55-63`, `TournamentServiceImpl.updateTournamentPlayerTeam:185`. Done in code, **not tracked**.

5. **`POST /tournaments/{id}/sync-fixtures` and `POST /tournaments/{id}/sync-odds`** admin endpoints (`TournamentController.java:125-143`) — implemented and not tracked as a separate task (RF-11 alludes to "fixture sync" but these controllers are absent from the explicit Done list).

6. **`POST /events/{id}/end`, `POST /events/{id}/start`, `PATCH /events/{id}/penalties`.** The `recordPenalties` flow with `EventStatus.PENALTIES` (`EventController.java:87-94`, `EventServiceImpl.recordPenalties:234-294`) goes beyond "Bracket Phase 4 Knockout Behavior" wording and deserves its own line.

7. **Spring Boot 4 / Java 17 / Flyway target=36 stack update.** The Done list says nothing; `docs/AGENTS.md` still claims Spring Boot 3.4 / Java 21 / "V1–V18" migrations. This drift should be an explicit Done entry so future agents don't trust stale docs.

8. **`market_event_market_type_key UNIQUE (event_id, market_type)`** (`V30__ADD_MARKET_TYPE.sql:10`) — meaningful integrity guarantee introduced for multi-market that is invisible in the backlog.

9. **`TournamentGroupConfigValidator`** (`service/validator/TournamentGroupConfigValidator.java` + 9 tests) is a proper invariant gate but only Bracket Phase 8 "Tests" mentions it in passing — it deserves a Done line ("Group config validation rules + admin endpoint `GET /tournaments/tournament-group-config`", `TournamentController.java:65-72`).

---

## 4. TODO / FIXME Findings

Source island: every literal `TODO`/`FIXME`/`HACK` in `src/main` and `src/test`.

| File:line | Comment | Maps to backlog task | Notes |
|---|---|---|---|
| `service/Impl/WalletServiceImpl.java:44` | `// TODO: return transactions history` | Pending → "Transactions history endpoint `GET /wallet/transactions`" (BACKLOG line 94) | Wallet DTO has no transactions list |
| `controller/TournamentController.java:145` | `// TODO: end tournament endpoint -> necessario?` | Pending → "End tournament manual endpoint" (BACKLOG line 95) | Author left a design question (`necessario?`) — decision needed before scheduling |
| `controller/EventController.java:33` | `// TODO: criar endpoint para adicionar partida que ja aconteceu` | Pending → "Past match import endpoint" (BACKLOG line 96) | Brazilian Portuguese consistency tip with rest of code |
| `service/Impl/BetServiceImpl.java:220` | `// TODO: other market types resolving` | Pending → "Other market types resolving in BetServiceImpl" (BACKLOG line 97) | Resolving only matches `MarketType.MATCH_RESULT`; new `MarketType` enum values would silently resolve as LOST |
| `service/Impl/FixtureSyncServiceImpl.java:102` | `// TODO: validate get_events round/stage fields once knockout fixtures…` | Pending → "Validate knockout fixture round/stage fields for REAL_FOOTBALL sync" (BACKLOG line 98) | Currently relies on group-equality heuristic only |
| `service/Impl/FixtureSyncServiceImpl.java:159` | `// TODO: deixar isso somente na criacao do torneio` | **NEW — not in BACKLOG** | Odds import is currently re-run on every sync; should move to tournament-creation flow only |

Additional findings (no literal `TODO` symbol but functionally incomplete):

- `config/websocket/WebSocketBroadcaster.java:48-52` contains an `onEventChangeRollback` stub whose only body is a comment ("No action needed on rollback."). Cosmetic dead code — remove or add real rollback semantics.
- `service/Impl/OddsUpdateServiceImpl.java:104-112` resolves outcomes by name (`event.getHomeName()`, `"Empate"`, `event.getAwayName()`). When team-name renaming is added (Real Football), odds updates will silently stop matching outcomes — should be flagged as `// FIXME` for future maintenance.
- `service/Impl/TournamentServiceImpl.java:1068` retains a commented-out `// int knockoutRoundOffset = …` hint that is dead code; either complete or delete.
- `application.properties:23` keeps a commented-out `gameforecast.rapidapi-key` next to an active different key — credential hygiene issue (should live in env vars, see Pending T10).

No `FIXME`/`HACK` strings exist in `src/main` or `src/test` (verified via `grep -rn`).

---

## 5. Dependency Groups

Recommended batching order when scheduling. Items inside a group can be parallelized; cross-groups have soft dependencies on prior completion.

### Group A — Security / Infra bootstrap (block production)
- T9 Dockerfile + T12 docker-compose.yml + T11 `.env.example`
- T10 Move `application.properties` to env vars + remove committed `.env`
- T20 SEC-4/SEC-5 DTO validation (`@NotNull`, `@Min`, `@Future` for dates)

### Group B — Real Football live lifecycle
1. T17 RF-15 wire `live-poll-enabled` into a `@ConfigurationProperties` bean
2. T14 RF-12 `RealFootballScheduler` (`@Scheduled` `autoStartEvents`, `pollLiveScores`)
3. T15 RF-13 status-to-action mapping (start/finish/cancel)
4. T16 RF-14 cancellation flow — reuse `BetServiceImpl.cancelBetsForEvent` and add `MarketStatus.CANCELLED` cascade
5. T31 FixtureSync knockout round/stage validation (unblocks RF-13)

### Group C — Market model integrity (blocks Custom Stat Markets)
- P5 Market-type filtering: `Tournament.marketTypes` must drive `createMarketAndOutcomesForEvent`
- T30 Other market types resolving in `BetServiceImpl`
- T19 `MarketSuspensionJob`: add `Market.suspendAt` column (`suspend_at TIMESTI` migration), add `EventStatus.SCHEDULED`, then implement the `@Scheduled(fixedDelay=60_000)` job; `completeEvent` (renamed from `finishEvent`) must set `MarketStatus.SETTLED` instead of `CLOSED` (or extend enum)

### Group D — Code quality refactors (low risk, parallelizable)
- P2 Migrate remaining 8 files from `jakarta.transaction.Transactional` to `org.springframework.transaction`
- T22 SPRING-3 add `readOnly=true` to all read-only service methods
- T21 QUAL-3 extract named constants
- T24 DOMAIN-3 move `StatsAccumulator` into `domain/valueobject/`
- T23 PROD-3 `SecureRandom` for `Collections.shuffle`
- T26 BUG-9 NPE hardening in `TournamentServiceImpl`
- P3 DB-1/2/3 add missing indexes & uniqueness

### Group E — Governance / tracking
- Promote Missing Task #1 (live odds recalculation) to a Done line; fix `docs/AGENTS.md` "static odds" claim
- Re-triage Missing Task #2 (ExternalApiLog write path actually called?)
- Decide T28 "End tournament endpoint" — answer the author's `necessario?` question
- Add Missing Task #9 (GroupConfigValidator) to Done section

### Group F — Frontend (out of repo; coordinate with frontend team)
- T1 B22, T2 B24, T3 B27, T4-T6 stats/standings pages, T7 legacy cleanup, T8 ResenhaBetState — all blocked only on the missing frontend repo; backend APIs they need are already shipped (`PlayerController.java:64`, `TournamentController.java:82`)

### Group G — Long-term platform evolution
- F2 Multi-Group platform (no code yet)
- F1 (remaining) Cashout + `outcome_odd_history` table (recalculation half is already done)

---

## 6. Recommended Roadmap

Phased, each phase being ~1 sprint of focused work. Assumes single full-stack engineer; double the time if backend/frontend are split.

### Phase 0 — Trust baseline (1–2 days)
1. Mark F1-recalculation as DONE and patch `docs/AGENTS.md` (spring-boot version, java version, "V1-V36", "static odds" claim).
2. Re-triage Missing #2 (ExternalApiLog write path). If absent, demote the Done item to **Partial**.
3. Reposition P6 as "BetSlipItem has **no** uniqueness — add a guard" rather than "verify migration".

### Phase 1 — Ship-ready infra (3–4 days)
Group A in full. Pull-request gating: app must boot without `application.properties` credentials.

### Phase 2 — Real Football live mode (5–7 days)
Execute Group B in order. RF-15 wiring (T17) is a half-day prerequisite that should land first because it gives every later task a togglable safe-mode.

### Phase 3 — Market model & Custom Stat Markets (4–6 days)
Land Group C before expanding market enumerators. Without `Market.suspendAt` + `MarketSuspensionJob`, multi-market betting will resolve wrongly under fixture cancellations (T16 + T30 share fate).

### Phase 4 — Refactors & perf (3–5 days)
Group D in parallel. Bundle the `TransactionType`/index/`@Transactional(readOnly)` work into one JIRA epic and verify with `EXPLAIN` on hot queries (T25 N+1 work overlaps).

### Phase 5 — Feature backlog (rolling)
T27 transactions endpoint (1 day), T29 past-import endpoint (1 day), T28 end-tournament endpoint (gated on author's decision). Each ships behind admin API guards via `CurrentUserContext.requireAdmin()`.

### Phase 6 — Frontend & platform
Coordinate frontend repo to land Group F. Group G remains future roadmap; revisit Multi-Group only after Phase 3 stabilizes market integrity.

---

### Evidence appendix — key symbol references

- Identity/auth: `controller/AuthController.java:28-52`, `service/Impl/AuthServiceImpl.java:39-94`, `config/SessionFilter.java`, `config/CurrentUserContext.java`
- Wallet: `controller/WalletController.java:21-43`, `service/Impl/WalletServiceImpl.java:40-129`
- Betting: `service/Impl/BetServiceImpl.java:54-346`, `domain/entity/BetSlip.java`, `domain/entity/BetSlipItem.java`
- Markets & odds: `service/Impl/MarketServiceImpl.java`, `service/Impl/OddsCalculatorServiceImpl.java`, `service/Impl/OddsUpdateServiceImpl.java:50-127`, `listener/OddsRecalculationListener.java:21-27`
- Tournaments / brackets: `service/Impl/TournamentServiceImpl.java:81-1305`, `service/validator/TournamentGroupConfigValidator.java`, `controller/TournamentController.java:33-143`
- Events / penalties / knockout: `service/Impl/EventServiceImpl.java:83-294`, `controller/EventController.java:25-94`
- Real football: `service/Impl/FixtureSyncServiceImpl.java:49-169`, `service/Impl/OddsImportServiceImpl.java`, `service/Impl/ApiFootballClientImpl.java`, `config/ApiFootballProperties.java`
- Competitions: `controller/CompetitionController.java`, `service/Impl/CompetitionServiceImpl.java:28-77`
- WebSocket: `config/websocket/WebSocketConfig.java`, `config/websocket/WebSocketBroadcaster.java:24-46`
- Migrations: `src/main/resources/db/migration/V1__…V36__`
- Tests: `src/test/java/com/franciscomaath/resenhaapi/**` — 20 test classes, 164 `@Test` methods.