# ENTITY_EXPORT.md — ResenhaBET Database Mapping

---

## Section 1 — Entity Inventory

---

### User

Class:
com.franciscomaath.resenhaapi.domain.entity.User

Table:
users

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY/SEQUENCE)

Generation Strategy:
IDENTITY

Unique Constraints:
* name

Indexes:
* uk_users_name (unique, on name column)

Auditing Fields:
* createdAt (CreationTimestamp)

Soft Delete Fields:
None

---

### Wallet

Class:
com.franciscomaath.resenhaapi.domain.entity.Wallet

Table:
wallet

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
* user_id (OneToOne)

Indexes:
None

Auditing Fields:
None

Soft Delete Fields:
None

---

### Player

Class:
com.franciscomaath.resenhaapi.domain.entity.Player

Table:
player

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
* user_id

Indexes:
* uk_player_user_id (unique, on user_id column)

Auditing Fields:
None

Soft Delete Fields:
None

---

### Tournament

Class:
com.franciscomaath.resenhaapi.domain.entity.Tournament

Table:
tournament

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
* uuid

Indexes:
None

Auditing Fields:
* createdAt

Soft Delete Fields:
None

---

### TournamentRound

Class:
com.franciscomaath.resenhaapi.domain.entity.TournamentRound

Table:
tournament_round

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
None

Indexes:
None

Auditing Fields:
None

Soft Delete Fields:
None

---

### TournamentPlayer

Class:
com.franciscomaath.resenhaapi.domain.entity.TournamentPlayer

Table:
tournament_player

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
* uk_tournament_player (tournament_id, player_id)

Indexes:
* idx_tournament_player_group (tournament_id, group_number)

Auditing Fields:
None

Soft Delete Fields:
None

---

### Team

Class:
com.franciscomaath.resenhaapi.domain.entity.Team

Table:
team

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
* name
* api_football_team_id
* game_forecast_team_id

Indexes:
None

Auditing Fields:
None

Soft Delete Fields:
None

---

### Event

Class:
com.franciscomaath.resenhaapi.domain.entity.Event

Table:
event

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
None

Indexes:
* idx_event_tournament_status (tournament_id, status)

Auditing Fields:
None

Soft Delete Fields:
None

---

### Market

Class:
com.franciscomaath.resenhaapi.domain.entity.Market

Table:
market

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
* market_event_market_type_key (event_id, market_type)

Indexes:
None

Auditing Fields:
None

Soft Delete Fields:
None

---

### Outcome

Class:
com.franciscomaath.resenhaapi.domain.entity.Outcome

Table:
outcome

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
None

Indexes:
None

Auditing Fields:
None

Soft Delete Fields:
None

---

### BetSlip

Class:
com.franciscomaath.resenhaapi.domain.entity.BetSlip

Table:
bet_slip

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
None

Indexes:
None

Auditing Fields:
* createdAt

Soft Delete Fields:
None

---

### BetSlipItem

Class:
com.franciscomaath.resenhaapi.domain.entity.BetSlipItem

Table:
bet_slip_item

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
None (but business rule prevents two items from same event in one slip)

Indexes:
None

Auditing Fields:
None

Soft Delete Fields:
None

---

### Transaction

Class:
com.franciscomaath.resenhaapi.domain.entity.Transaction

Table:
transaction

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
None

Indexes:
None

Auditing Fields:
* createdAt (CreationTimestamp)

Soft Delete Fields:
None

---

### Session

Class:
com.franciscomaath.resenhaapi.domain.entity.Session

Table:
session

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
* token

Indexes:
* uk_session_token (unique, on token column)

Auditing Fields:
* createdAt (CreationTimestamp)

Soft Delete Fields:
None

---

### Competition

Class:
com.franciscomaath.resenhaapi.domain.entity.Competition

Table:
competition

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
* uuid
* uq_competition_external (api_football_league_id, api_football_country_id, season)

Indexes:
None

Auditing Fields:
* createdAt

Soft Delete Fields:
None

---

### ExternalApiLog

Class:
com.franciscomaath.resenhaapi.domain.entity.ExternalApiLog

Table:
external_api_log

Schema:
resenha

Primary Key:
id (BIGINT, IDENTITY)

Generation Strategy:
IDENTITY

Unique Constraints:
None

Indexes:
* idx_external_api_log_lookup (provider, endpoint, request_key, fetched_at DESC)

Auditing Fields:
* fetchedAt (CreationTimestamp)

Soft Delete Fields:
None

---

### tournament_market_type (ElementCollection)

Class:
com.franciscomaath.resenhaapi.domain.entity.Tournament (via @ElementCollection)

Table:
tournament_market_type

Schema:
resenha

Primary Key:
(tournament_id, market_type) — composite

Generation Strategy:
N/A (not an entity)

Unique Constraints:
PRIMARY KEY (tournament_id, market_type)

Indexes:
None

Auditing Fields:
None

Soft Delete Fields:
None

---

## Section 2 — Complete Field Mapping

---

### User (table: users)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| name | String | name | false | null | 255 | true | |
| pinHash | String | pin_hash | true | null | 255 | false | SHA-256 hash |
| salt | String | salt | true | null | 255 | false | Per-user salt for PIN hashing |
| userType | UserType (enum) | user_type | false | USER | 50 (VARCHAR) | false | Stored as STRING |
| firstLogin | boolean | is_first_login | false | true | N/A | false | |
| createdAt | LocalDateTime | created_at | true | CURRENT_TIMESTAMP | N/A | false | @CreationTimestamp |

---

### Wallet (table: wallet)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| user | User | user_id | true | null | N/A | true | OneToOne, unique FK |
| balance | BigDecimal | balance | true | null | 19,2 (DECIMAL) | false | |

---

### Player (table: player)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| name | String | name | true | null | 255 | false | NOT NULL in DB migration |
| active | Boolean | is_active | true | null | N/A | false | NOT NULL in DB with DEFAULT TRUE |
| currentElo | BigDecimal | current_elo | false | 1000.00 | 10,2 (DECIMAL) | false | @Builder.Default = BigDecimal.valueOf(1000) |
| user | User | user_id | true | null | N/A | true (unique) | ManyToOne, nullable |

---

### Tournament (table: tournament)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| version | Long | version | true | 0 | N/A | false | @Version for optimistic locking |
| uuid | UUID | uuid | false | null | N/A | true | |
| name | String | name | false | null | 255 | false | |
| type | TournamentType (enum) | type | true | FIFA_MATCH | 50 (VARCHAR) | false | Stored as STRING |
| format | TournamentFormat (enum) | format | true | null | 50 (VARCHAR) | false | Stored as STRING; NOT NULL in migration |
| status | TournamentStatus (enum) | status | true | null | 50 (VARCHAR) | false | Stored as STRING |
| startDate | LocalDateTime | start_date | true | null | N/A | false | |
| endDate | LocalDateTime | end_date | true | null | N/A | false | |
| createdAt | LocalDateTime | created_at | true | CURRENT_TIMESTAMP | N/A | false | |
| generationMode | GenerationMode (enum) | generation_mode | false | MANUAL | 10 (VARCHAR) | false | Stored as STRING; @Builder.Default = MANUAL |
| hasThirdPlaceMatch | Boolean | has_third_place_match | false | false | N/A | false | @Builder.Default = false |
| numberOfGroups | Integer | number_of_groups | false | 1 | N/A | false | @Builder.Default = 1 |
| playersAdvancingPerGroup | Integer | players_advancing_per_group | false | 2 | N/A | false | @Builder.Default = 2 |
| competition | Competition | competition_id | true | null | N/A | false | ManyToOne, LAZY |
| marketTypes | Set<MarketType> (enum) | market_type | true | null | 50 (VARCHAR) | false | @ElementCollection in tournament_market_type table; stored as STRING |

---

### TournamentRound (table: tournament_round)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| tournament | Tournament | tournament_id | true | null | N/A | false | ManyToOne |
| name | String | name | true | null | 255 | false | NOT NULL in DB |
| multiplier | BigDecimal | multiplier | true | null | 10,4 (DECIMAL) | false | NOT NULL dropped in V31 |
| roundOrder | Integer | round_order | true | null | N/A | false | NOT NULL in DB |
| phaseType | PhaseType (enum) | phase_type | false | GROUP_STAGE | 15 (VARCHAR) | false | Stored as STRING; @Builder.Default = GROUP_STAGE |
| groupNumber | Integer | group_number | true | null | N/A | false | |

---

### TournamentPlayer (table: tournament_player)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| tournament | Tournament | tournament_id | false | null | N/A | false | ManyToOne |
| player | Player | player_id | false | null | N/A | false | ManyToOne |
| team | Team | team_id | true | null | N/A | false | ManyToOne, nullable |
| groupNumber | Integer | group_number | true | null | N/A | false | |

---

### Team (table: team)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| name | String | name | false | null | 255 | true | |
| abbreviation | String | abbreviation | false | null | 4 | false | UNIQUE constraint dropped in V32 |
| apiFootballTeamId | String | api_football_team_id | true | null | 20 | true | Replaces external_api_id in V35 |
| gameForecastTeamId | String | game_forecast_team_id | true | null | 20 | true | |
| badgeUrl | String | badge_url | true | null | 255 | false | |

---

### Event (table: event)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| tournament | Tournament | tournament_id | true | null | N/A | false | NOT NULL in DB |
| round | TournamentRound | round_id | true | null | N/A | false | ManyToOne |
| playerHome | Player | player_home_id | true | null | N/A | false | originally NOT NULL, nullable since V25 |
| playerAway | Player | player_away_id | true | null | N/A | false | originally NOT NULL, nullable since V25 |
| teamHome | Team | team_home_id | true | null | N/A | false | Added in V29 |
| teamAway | Team | team_away_id | true | null | N/A | false | Added in V29 |
| externalMatchId | String | external_match_id | true | null | 20 | false | Added in V29 |
| gameDatetime | LocalDateTime | game_datetime | true | null | N/A | false | originally NOT NULL, nullable since V25 |
| status | EventStatus (enum) | status | false | null | 50 (VARCHAR) | false | Stored as STRING |
| homeScore | Integer | home_score | true | null | N/A | false | |
| awayScore | Integer | away_score | true | null | N/A | false | |
| homeEloBefore | BigDecimal | home_elo_before | true | null | 10,2 (DECIMAL) | false | |
| awayEloBefore | BigDecimal | away_elo_before | true | null | 10,2 (DECIMAL) | false | |
| isKnockout | Boolean | is_knockout | true | null | N/A | false | |
| isBye | Boolean | is_bye | false | false | N/A | false | @Builder.Default = false |
| penaltiesHome | Integer | penalties_home | true | null | N/A | false | Added in V22 |
| penaltiesAway | Integer | penalties_away | true | null | N/A | false | Added in V22 |
| nextRoundEvent | Event | next_round_event_id | true | null | N/A | false | Self-referencing FK; Added in V22 |
| homeSourceEvent | Event | home_source_event_id | true | null | N/A | false | Self-referencing FK; Added in V22 |
| awaySourceEvent | Event | away_source_event_id | true | null | N/A | false | Self-referencing FK; Added in V22 |
| thirdPlaceMatch | boolean | is_third_place_match | false | false | N/A | false | @Builder.Default = false; Added in V27 |

---

### Market (table: market)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| event | Event | event_id | true | null | N/A | false | NOT NULL in DB |
| name | String | name | true | null | 255 | false | NOT NULL in DB |
| status | MarketStatus (enum) | status | false | null | 50 (VARCHAR) | false | Stored as STRING |
| marketType | MarketType (enum) | market_type | false | MATCH_RESULT | 30 (VARCHAR) | false | Part of unique(event_id, market_type) |

---

### Outcome (table: outcome)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| market | Market | market_id | true | null | N/A | false | NOT NULL in DB |
| name | String | name | true | null | 255 | false | NOT NULL in DB |
| odd | BigDecimal | odd | true | null | 10,2 (DECIMAL) | false | NOT NULL in DB |

---

### BetSlip (table: bet_slip)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| user | User | user_id | true | null | N/A | false | NOT NULL in DB |
| tournament | Tournament | tournament_id | true | null | N/A | false | NOT NULL in DB |
| stake | BigDecimal | stake | true | null | 19,2 (DECIMAL) | false | NOT NULL in DB |
| combinedOdd | BigDecimal | combined_odd | true | null | 10,2 (DECIMAL) | false | NOT NULL in DB |
| potentialReturn | BigDecimal | potential_return | true | null | 19,2 (DECIMAL) | false | NOT NULL in DB |
| status | BetSlipStatus (enum) | status | true | null | 50 (VARCHAR) | false | Stored as STRING |
| createdAt | LocalDateTime | created_at | true | CURRENT_TIMESTAMP | N/A | false | |

---

### BetSlipItem (table: bet_slip_item)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| betSlip | BetSlip | bet_slip_id | true | null | N/A | false | NOT NULL in DB |
| event | Event | event_id | true | null | N/A | false | NOT NULL in DB |
| outcome | Outcome | outcome_id | true | null | N/A | false | NOT NULL in DB |
| oddSnapshot | BigDecimal | odd_snapshot | true | null | 10,2 (DECIMAL) | false | NOT NULL in DB |
| status | BetSlipItemStatus (enum) | status | true | null | 50 (VARCHAR) | false | Stored as STRING |

---

### Transaction (table: transaction)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| wallet | Wallet | wallet_id | true | null | N/A | false | NOT NULL in DB |
| betSlip | BetSlip | bet_slip_id | true | null | N/A | false | Added in V19, nullable |
| type | TransactionType (enum) | type | true | null | 50 (VARCHAR) | false | Stored as STRING |
| value | BigDecimal | value | true | null | 19,2 (DECIMAL) | false | NOT NULL in DB |
| createdAt | LocalDateTime | created_at | true | CURRENT_TIMESTAMP | N/A | false | @CreationTimestamp |

---

### Session (table: session)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key; No @Table annotation, so defaults to "session" |
| user | User | user_id | false | null | N/A | false | ManyToOne, LAZY |
| token | UUID | token | false | null | N/A | true | |
| createdAt | LocalDateTime | created_at | false | CURRENT_TIMESTAMP | N/A | false | @CreationTimestamp |
| expiresAt | LocalDateTime | expires_at | false | null | N/A | false | |

---

### Competition (table: competition)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| uuid | UUID | uuid | false | null | N/A | true | |
| name | String | name | false | null | 255 | false | |
| season | String | season | false | null | 20 | false | |
| apiFootballLeagueId | String | api_football_league_id | false | null | 20 | false | |
| apiFootballCountryId | String | api_football_country_id | false | null | 20 | false | |
| gameForecastLeagueId | String | game_forecast_league_id | false | null | 20 | false | |
| active | Boolean | active | false | true | N/A | false | @Builder.Default = true |
| createdAt | LocalDateTime | created_at | false | LocalDateTime.now() | N/A | false | @Builder.Default = LocalDateTime.now() |

---

### ExternalApiLog (table: external_api_log, schema: resenha)

| Field | Java Type | Column Name | Nullable | Default | Length | Unique | Notes |
|-------|-----------|-------------|----------|---------|--------|--------|-------|
| id | Long | id | false | auto (IDENTITY) | N/A | true | Primary key |
| provider | String | provider | false | null | 30 | false | e.g. 'API_FOOTBALL', 'GAME_FORECAST' |
| endpoint | String | endpoint | false | null | 100 | false | e.g. 'get_events', '/events' |
| requestKey | String | requestKey | false | null | 255 | false | Deterministic key for request params |
| responseBody | String | response_body | false | null | N/A | false | @JdbcTypeCode(SqlTypes.JSON), columnDefinition = "jsonb" |
| statusCode | Integer | status_code | true | null | N/A | false | |
| fetchedAt | LocalDateTime | fetched_at | false | CURRENT_TIMESTAMP | N/A | false | @CreationTimestamp |

---

## Section 3 — Enum Definitions

---

### TournamentStatus

Stored As:
STRING

Values:
* CREATED
* IN_PROGRESS
* COMPLETED
* CANCELLED

---

### TournamentType

Stored As:
STRING

Values:
* REAL_FOOTBALL
* FIFA_MATCH

---

### TournamentFormat

Stored As:
STRING

Values:
* LEAGUE
* BRACKET
* LEAGUE_BRACKET

---

### EventStatus

Stored As:
STRING

Values:
* CREATED
* IN_PROGRESS
* PENALTIES
* COMPLETED
* CANCELLED

---

### MarketStatus

Stored As:
STRING

Values:
* OPEN
* SUSPENDED
* CLOSED
* CANCELLED

---

### MarketType

Stored As:
STRING

Values:
* MATCH_RESULT
* OVER_UNDER_25
* OVER_UNDER_35
* BTTS
* EXACT_SCORE
* FIRST_HALF_RESULT

---

### BetSlipStatus

Stored As:
STRING

Values:
* PENDING
* WON
* LOST
* CANCELLED

---

### BetSlipItemStatus

Stored As:
STRING

Values:
* PENDING
* WON
* LOST
* CANCELLED

---

### BetStatus

Stored As:
STRING (not used in current entities, exists as legacy enum)

Values:
* PENDING
* WON
* LOST
* CANCELLED

---

### GenerationMode

Stored As:
STRING

Values:
* AUTO
* MANUAL

---

### PhaseType

Stored As:
STRING

Values:
* GROUP_STAGE
* KNOCKOUT

---

### TransactionType

Stored As:
STRING

Values:
* DEPOSIT
* WITHDRAWAL
* BET_PLACED
* BET_WON
* BET_REFUND

---

### UserType

Stored As:
STRING

Values:
* ADMIN
* USER

---

## Section 4 — Relationships

---

### User -> Wallet

Type:
OneToOne

Mapped By:
wallet.user

FK Column:
user_id (in wallet table)

Nullable:
true (FK column)

Cascade:
NONE

Fetch:
EAGER (default for @OneToOne)

OrphanRemoval:
false

Referenced Entity:
User

---

### User -> BetSlip

Type:
OneToMany (mapped by betSlip.user — inferred from BetSlip entity's ManyToOne)

FK Column:
user_id (in bet_slip table)

Nullable:
false

Cascade:
NONE

Fetch:
LAZY (default for @OneToMany)

Referenced Entity:
User

---

### User -> Session

Type:
OneToMany (mapped by session.user — inferred)

FK Column:
user_id (in session table)

Nullable:
false

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
User

---

### User -> Player

Type:
OneToMany (mapped by player.user — inferred)

FK Column:
user_id (in player table)

Nullable:
true

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
User

---

### Wallet -> Transaction

Type:
OneToMany (mapped by transaction.wallet — inferred)

FK Column:
wallet_id (in transaction table)

Nullable:
false

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Wallet

---

### Player -> TournamentPlayer

Type:
OneToMany (mapped by tournamentPlayer.player — inferred)

FK Column:
player_id (in tournament_player table)

Nullable:
false

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Player

---

### Player -> Event (as playerHome)

Type:
OneToMany (mapped by event.playerHome — inferred)

FK Column:
player_home_id (in event table)

Nullable:
true

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Player

---

### Player -> Event (as playerAway)

Type:
OneToMany (mapped by event.playerAway — inferred)

FK Column:
player_away_id (in event table)

Nullable:
true

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Player

---

### Tournament -> TournamentPlayer

Type:
OneToMany

Mapped By:
tournamentPlayer.tournament

FK Column:
tournament_id (in tournament_player table)

Nullable:
false

Cascade:
ALL

OrphanRemoval:
true

Fetch:
LAZY

Referenced Entity:
Tournament

---

### Tournament -> TournamentRound

Type:
OneToMany

Mapped By:
round.tournament

FK Column:
tournament_id (in tournament_round table)

Nullable:
false

Cascade:
ALL

OrphanRemoval:
false

Fetch:
LAZY

Referenced Entity:
Tournament

---

### Tournament -> Event

Type:
OneToMany (mapped by event.tournament — inferred)

FK Column:
tournament_id (in event table)

Nullable:
false

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Tournament

---

### Tournament -> BetSlip

Type:
OneToMany (mapped by betSlip.tournament — inferred)

FK Column:
tournament_id (in bet_slip table)

Nullable:
false

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Tournament

---

### Tournament -> Competition

Type:
ManyToOne

FK Column:
competition_id (in tournament table)

Nullable:
true

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Competition

---

### Tournament -> Set<MarketType> (ElementCollection)

Type:
ElementCollection

Join Table:
tournament_market_type

Join Column:
tournament_id

Column:
market_type (VARCHAR, stored as STRING)

Nullable:
N/A

Cascade:
ALL

Fetch:
LAZY (default for @ElementCollection)

---

### TournamentRound -> Tournament

Type:
ManyToOne

FK Column:
tournament_id

Nullable:
true (entity), NOT NULL in DB

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
Tournament

---

### TournamentRound -> Event

Type:
OneToMany (mapped by event.round — inferred)

FK Column:
round_id (in event table)

Nullable:
true

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
TournamentRound

---

### TournamentPlayer -> Tournament

Type:
ManyToOne

FK Column:
tournament_id

Nullable:
false

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
Tournament

---

### TournamentPlayer -> Player

Type:
ManyToOne

FK Column:
player_id

Nullable:
false

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
Player

---

### TournamentPlayer -> Team

Type:
ManyToOne

FK Column:
team_id

Nullable:
true

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
Team

---

### Event -> Tournament

Type:
ManyToOne

FK Column:
tournament_id

Nullable:
true (entity), NOT NULL in DB

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
Tournament

---

### Event -> TournamentRound

Type:
ManyToOne

FK Column:
round_id

Nullable:
true

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
TournamentRound

---

### Event -> Player (as playerHome)

Type:
ManyToOne

FK Column:
player_home_id

Nullable:
true

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Player

---

### Event -> Player (as playerAway)

Type:
ManyToOne

FK Column:
player_away_id

Nullable:
true

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Player

---

### Event -> Team (as teamHome)

Type:
ManyToOne

FK Column:
team_home_id

Nullable:
true

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Team

---

### Event -> Team (as teamAway)

Type:
ManyToOne

FK Column:
team_away_id

Nullable:
true

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Team

---

### Event -> Event (self-ref as nextRoundEvent)

Type:
ManyToOne

FK Column:
next_round_event_id

Nullable:
true

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Event (self)

---

### Event -> Event (self-ref as homeSourceEvent)

Type:
ManyToOne

FK Column:
home_source_event_id

Nullable:
true

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Event (self)

---

### Event -> Event (self-ref as awaySourceEvent)

Type:
ManyToOne

FK Column:
away_source_event_id

Nullable:
true

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Event (self)

---

### Event -> Market

Type:
OneToMany (mapped by market.event — inferred)

FK Column:
event_id (in market table)

Nullable:
false

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Event

---

### Event -> BetSlipItem

Type:
OneToMany (mapped by betSlipItem.event — inferred)

FK Column:
event_id (in bet_slip_item table)

Nullable:
false

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Event

---

### Market -> Outcome

Type:
OneToMany (mapped by outcome.market — inferred)

FK Column:
market_id (in outcome table)

Nullable:
false

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Market

---

### Market -> Event

Type:
ManyToOne

FK Column:
event_id

Nullable:
true (entity), NOT NULL in DB

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
Event

---

### Outcome -> Market

Type:
ManyToOne

FK Column:
market_id

Nullable:
true (entity), NOT NULL in DB

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
Market

---

### Outcome -> BetSlipItem

Type:
OneToMany (mapped by betSlipItem.outcome — inferred)

FK Column:
outcome_id (in bet_slip_item table)

Nullable:
false

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
Outcome

---

### BetSlip -> User

Type:
ManyToOne

FK Column:
user_id

Nullable:
true (entity), NOT NULL in DB

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
User

---

### BetSlip -> Tournament

Type:
ManyToOne

FK Column:
tournament_id

Nullable:
true (entity), NOT NULL in DB

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
Tournament

---

### BetSlip -> BetSlipItem

Type:
OneToMany

Mapped By:
betSlipItem.betSlip

FK Column:
bet_slip_id

Nullable:
false

Cascade:
ALL

OrphanRemoval:
true

Fetch:
LAZY

Referenced Entity:
BetSlip

---

### BetSlip -> Transaction

Type:
OneToMany (mapped by transaction.betSlip — inferred)

FK Column:
bet_slip_id (in transaction table)

Nullable:
true

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
BetSlip

---

### BetSlipItem -> BetSlip

Type:
ManyToOne

FK Column:
bet_slip_id

Nullable:
true (entity), NOT NULL in DB

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
BetSlip

---

### BetSlipItem -> Event

Type:
ManyToOne

FK Column:
event_id

Nullable:
true (entity), NOT NULL in DB

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
Event

---

### BetSlipItem -> Outcome

Type:
ManyToOne

FK Column:
outcome_id

Nullable:
true (entity), NOT NULL in DB

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
Outcome

---

### Transaction -> Wallet

Type:
ManyToOne

FK Column:
wallet_id

Nullable:
true (entity), NOT NULL in DB

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
Wallet

---

### Transaction -> BetSlip

Type:
ManyToOne

FK Column:
bet_slip_id

Nullable:
true

Cascade:
NONE

Fetch:
EAGER (default for @ManyToOne)

Referenced Entity:
BetSlip

---

### Session -> User

Type:
ManyToOne

FK Column:
user_id

Nullable:
false

Cascade:
NONE

Fetch:
LAZY

Referenced Entity:
User

---

### Competition -> Tournament

Type:
OneToMany

Mapped By:
tournament.competition

FK Column:
competition_id

Nullable:
true

Cascade:
NONE

OrphanRemoval:
false

Fetch:
LAZY

Referenced Entity:
Competition

---

## Section 5 — Database Creation Order

Foreign key dependencies determine the required insert order:

1. **Competition** — no FKs
2. **User** — no FKs
3. **Wallet** — depends on User (user_id FK)
4. **Player** — depends on User (user_id FK, nullable)
5. **Team** — no FKs
6. **Session** — depends on User (user_id FK)
7. **Tournament** — depends on Competition (competition_id FK, nullable)
8. **tournament_market_type** — depends on Tournament (tournament_id FK)
9. **TournamentRound** — depends on Tournament (tournament_id FK)
10. **TournamentPlayer** — depends on Tournament (tournament_id FK) and Player (player_id FK); optionally Team (team_id FK)
11. **Event** — depends on Tournament (tournament_id FK), TournamentRound (round_id FK, nullable), Player (player_home_id, player_away_id, nullable), Team (team_home_id, team_away_id, nullable), Event self-refs (next_round_event_id, home_source_event_id, away_source_event_id)
12. **Market** — depends on Event (event_id FK)
13. **Outcome** — depends on Market (market_id FK)
14. **BetSlip** — depends on User (user_id FK), Tournament (tournament_id FK)
15. **BetSlipItem** — depends on BetSlip (bet_slip_id FK), Event (event_id FK), Outcome (outcome_id FK)
16. **Transaction** — depends on Wallet (wallet_id FK), BetSlip (bet_slip_id FK, nullable)
17. **ExternalApiLog** — no FKs

NOTE: For Event self-referencing FKs (next_round_event_id, home_source_event_id, away_source_event_id), these can reference existing Event IDs. If using pre-generated IDs, ensure the referenced events exist before inserting the referencing event, or insert all events first with null self-refs, then UPDATE them.

---

## Section 6 — Mandatory Fields

Fields that MUST be populated for a valid INSERT (NOT NULL constraints from DB migrations, not entity annotations).

### Competition

Required:
* uuid
* name
* season
* api_football_league_id
* api_football_country_id
* game_forecast_league_id
* active
* created_at

Optional:
* id (auto-generated)

---

### User

Required:
* name
* user_type (default 'USER')
* is_first_login (default TRUE)

Optional:
* id (auto-generated)
* pin_hash
* salt
* created_at (default CURRENT_TIMESTAMP)

---

### Wallet

Required:
* user_id
* balance (default 0.00)

Optional:
* id (auto-generated)

---

### Player

Required:
* name
* is_active (default TRUE)
* current_elo (default 1000.00)

Optional:
* id (auto-generated)
* user_id

---

### Team

Required:
* name
* abbreviation

Optional:
* id (auto-generated)
* api_football_team_id
* game_forecast_team_id
* badge_url

---

### Session

Required:
* user_id
* token
* created_at (default CURRENT_TIMESTAMP)
* expires_at

Optional:
* id (auto-generated)

---

### Tournament

Required:
* uuid
* name
* format (added in V12, NOT NULL DEFAULT 'LEAGUE')
* status
* version (default 0)
* generation_mode (default 'MANUAL')
* has_third_place_match (default false)
* number_of_groups (default 1)
* players_advancing_per_group (default 2)

Optional:
* id (auto-generated)
* type (default 'FIFA_MATCH')
* start_date
* end_date
* created_at (default CURRENT_TIMESTAMP)
* competition_id

---

### tournament_market_type (ElementCollection)

Required:
* tournament_id
* market_type

---

### TournamentRound

Required:
* tournament_id
* name
* round_order
* phase_type (default 'GROUP_STAGE')

Optional:
* id (auto-generated)
* multiplier (nullable since V31)

---

### TournamentPlayer

Required:
* tournament_id
* player_id

Optional:
* id (auto-generated)
* team_id
* group_number

---

### Event

Required:
* tournament_id
* status
* is_bye (default false)
* is_third_place_match (default false)

Optional:
* id (auto-generated)
* round_id
* player_home_id (nullable since V25)
* player_away_id (nullable since V25)
* team_home_id
* team_away_id
* external_match_id
* game_datetime (nullable since V25)
* home_score
* away_score
* home_elo_before
* away_elo_before
* is_knockout
* penalties_home
* penalties_away
* next_round_event_id
* home_source_event_id
* away_source_event_id

---

### Market

Required:
* event_id
* name
* status
* market_type (default 'MATCH_RESULT')

Optional:
* id (auto-generated)

---

### Outcome

Required:
* market_id
* name
* odd

Optional:
* id (auto-generated)

---

### BetSlip

Required:
* user_id
* tournament_id
* stake
* combined_odd
* potential_return
* status

Optional:
* id (auto-generated)
* created_at (default CURRENT_TIMESTAMP)

---

### BetSlipItem

Required:
* bet_slip_id
* event_id
* outcome_id
* odd_snapshot
* status

Optional:
* id (auto-generated)

---

### Transaction

Required:
* wallet_id
* type
* value

Optional:
* id (auto-generated)
* bet_slip_id
* created_at (default CURRENT_TIMESTAMP)

---

### ExternalApiLog

Required:
* provider
* endpoint
* request_key
* response_body (jsonb)
* fetched_at (default now())

Optional:
* id (auto-generated)
* status_code

---

## Section 7 — Flyway Analysis

### Current Schema Version

Latest migration:
V37

Flyway target:
37 (configured in application.properties)

---

### V1__CREATE_USER_TABLE.sql

Purpose:
Create users table

Tables created:
* users (id BIGSERIAL PK, name VARCHAR(255) NOT NULL, pin_hash VARCHAR(255), salt VARCHAR(255), user_type VARCHAR(50) NOT NULL DEFAULT 'USER', is_first_login BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)

Columns added:
* id, name, pin_hash, salt, user_type, is_first_login, created_at

---

### V2__CREATE_WALLET_TABLE.sql

Purpose:
Create wallet table with FK to users

Tables created:
* wallet (id BIGSERIAL PK, user_id BIGINT NOT NULL UNIQUE, balance DECIMAL(19,2) NOT NULL DEFAULT 0.0, FK fk_wallet_user → users)

Columns added:
* id, user_id, balance

---

### V3__CREATE_TRANSACTION_TABLE.sql

Purpose:
Create transaction table with FK to wallet

Tables created:
* transaction (id BIGSERIAL PK, wallet_id BIGINT NOT NULL, type VARCHAR(50) NOT NULL, value DECIMAL(19,2) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, FK fk_transaction_wallet → wallet)

Columns added:
* id, wallet_id, type, value, created_at

---

### V4__CREATE_PLAYER_TABLE.sql

Purpose:
Create player table with optional FK to users

Tables created:
* player (id BIGSERIAL PK, name VARCHAR(255) NOT NULL, is_active BOOLEAN NOT NULL DEFAULT TRUE, current_elo DECIMAL(10,2) NOT NULL DEFAULT 1000.00, user_id BIGINT UNIQUE, FK fk_player_user → users)

Columns added:
* id, name, is_active, current_elo, user_id

---

### V5__CREATE_TOURNAMENT_TABLE.sql

Purpose:
Create tournament table

Tables created:
* tournament (id BIGSERIAL PK, uuid UUID NOT NULL UNIQUE, name VARCHAR(255) NOT NULL, type VARCHAR(50) NOT NULL DEFAULT 'FIFA_MATCH', status VARCHAR(50) NOT NULL, start_date TIMESTAMP, end_date TIMESTAMP, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)

Columns added:
* id, uuid, name, type, status, start_date, end_date, created_at

---

### V6__CREATE_TOURNAMENT_ROUND_TABLE.sql

Purpose:
Create tournament_round table with FK to tournament

Tables created:
* tournament_round (id BIGSERIAL PK, tournament_id BIGINT NOT NULL, name VARCHAR(255) NOT NULL, multiplier DECIMAL(10,4) NOT NULL, round_order INT NOT NULL, FK fk_round_tournament → tournament)

Columns added:
* id, tournament_id, name, multiplier, round_order

---

### V7__CREATE_TOURNAMENT_PLAYER_TABLE.sql

Purpose:
Create tournament_player join table (composite PK tournament_id, player_id)

Tables created:
* tournament_player (tournament_id BIGINT NOT NULL, player_id BIGINT NOT NULL, PRIMARY KEY (tournament_id, player_id), FK fk_tp_tournament → tournament, FK fk_tp_player → player)

Columns added:
* tournament_id, player_id

---

### V8__CREATE_EVENT_TABLE.sql

Purpose:
Create event table

Tables created:
* event (id BIGSERIAL PK, tournament_id BIGINT NOT NULL, round_id BIGINT, player_home_id BIGINT NOT NULL, home_elo_before DECIMAL(10,2), player_away_id BIGINT NOT NULL, away_elo_before DECIMAL(10,2), game_datetime TIMESTAMP NOT NULL, status VARCHAR(50) NOT NULL, home_score INT, away_score INT, is_knockout BOOLEAN NOT NULL DEFAULT FALSE, is_bye BOOLEAN NOT NULL DEFAULT FALSE, FKs: tournament, round, player_home, player_away)

Columns added:
* id, tournament_id, round_id, player_home_id, home_elo_before, player_away_id, away_elo_before, game_datetime, status, home_score, away_score, is_knockout, is_bye

---

### V9__CREATE_MARKET_TABLE.sql

Purpose:
Create market table with FK to event

Tables created:
* market (id BIGSERIAL PK, event_id BIGINT NOT NULL, name VARCHAR(255) NOT NULL, status VARCHAR(50) NOT NULL, FK fk_market_event → event)

Columns added:
* id, event_id, name, status

---

### V10__CREATE_OUTCOME_TABLE.sql

Purpose:
Create outcome table with FK to market

Tables created:
* outcome (id BIGSERIAL PK, market_id BIGINT NOT NULL, name VARCHAR(255) NOT NULL, odd DECIMAL(10,2) NOT NULL, FK fk_outcome_market → market)

Columns added:
* id, market_id, name, odd

---

### V11__CREATE_BET_TABLE.sql

Purpose:
Create legacy bet table (later dropped in V19)

Tables created:
* bet (id BIGSERIAL PK, user_id BIGINT NOT NULL, event_id BIGINT NOT NULL, outcome_id BIGINT NOT NULL, amount DECIMAL(19,2) NOT NULL, odd_snapshot DECIMAL(10,2) NOT NULL, potential_return DECIMAL(19,2) NOT NULL, status VARCHAR(50) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, FKs: user, event, outcome)

Columns added:
* id, user_id, event_id, outcome_id, amount, odd_snapshot, potential_return, status, created_at

---

### V12__ADD_TOURNAMENT_FORMAT_COLUMN.sql

Purpose:
Add format column to tournament

Tables created:
None

Columns added:
* format VARCHAR(50) NOT NULL DEFAULT 'LEAGUE' (to tournament)

---

### V13__CREATE_TEAM_TABLE.sql

Purpose:
Create team table

Tables created:
* team (id BIGSERIAL PK, name VARCHAR(255) NOT NULL UNIQUE, abbreviation VARCHAR(4) NOT NULL UNIQUE, external_api_id BIGINT UNIQUE, badge_url VARCHAR(255))

Columns added:
* id, name, abbreviation, external_api_id, badge_url

---

### V14__ALTER_TOURNAMENT_PLAYER.sql

Purpose:
Change tournament_player from composite PK to single-column PK, add team_id FK

Tables modified:
* tournament_player

Columns added:
* id (BIGSERIAL PRIMARY KEY — replaces composite PK)
* team_id BIGINT (FK → team)

Columns removed:
* PRIMARY KEY constraint (tournament_id, player_id) — dropped

---

### V15__CREATE_SESSION_TABLE.sql

Purpose:
Create session table

Tables created:
* session (id BIGSERIAL PK, user_id BIGINT NOT NULL, token UUID NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, expires_at TIMESTAMP NOT NULL, FK fk_user_id → users)

Columns added:
* id, user_id, token, created_at, expires_at

---

### V16__NORMALIZE_PLAYER_USER_LINK.sql

Purpose:
Ensure player.user_id column exists with unique index and FK

Tables modified:
* player

Columns added:
None (idempotent IF NOT EXISTS)

Indexes created:
* uk_player_user_id (UNIQUE on player.user_id)

Data migrations:
None

---

### V17__NORMALIZE_USER_IDENTITY.sql

Purpose:
Normalize users table — add missing columns idempotently, remove legacy columns, add unique name index

Tables modified:
* users

Columns added:
* name VARCHAR(255), pin_hash VARCHAR(255), salt VARCHAR(255), user_type VARCHAR(50) NOT NULL DEFAULT 'USER', is_first_login BOOLEAN NOT NULL DEFAULT TRUE (all IF NOT EXISTS)

Columns removed:
* player_id, email, hashcode

Indexes created:
* uk_users_name (UNIQUE on users.name)

---

### V18__NORMALIZE_SESSION_TABLE.sql

Purpose:
Normalize session table — ensure columns exist, add unique token index, ensure FK

Tables modified:
* session

Columns added:
None (idempotent IF NOT EXISTS)

Indexes created:
* uk_session_token (UNIQUE on session.token)

---

### V19__CREATE_BET_SLIP.sql

Purpose:
Drop legacy bet table; create bet_slip and bet_slip_item tables; add bet_slip_id to transaction

Tables dropped:
* bet (CASCADE)

Tables created:
* bet_slip (id BIGSERIAL PK, user_id BIGINT NOT NULL, tournament_id BIGINT NOT NULL, stake DECIMAL(19,2) NOT NULL, combined_odd DECIMAL(10,2) NOT NULL, potential_return DECIMAL(19,2) NOT NULL, status VARCHAR(50) NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, FKs: user, tournament)
* bet_slip_item (id BIGSERIAL PK, bet_slip_id BIGINT NOT NULL, event_id BIGINT NOT NULL, outcome_id BIGINT NOT NULL, odd_snapshot DECIMAL(10,2) NOT NULL, status VARCHAR(50) NOT NULL, FKs: bet_slip, event, outcome)

Columns added:
* bet_slip_id BIGINT to transaction (FK → bet_slip)

---

### V20__EXPAND_TOURNAMENT_FOR_BRACKET.sql

Purpose:
Add bracket/LEAGUE_BRACKET columns to tournament

Tables modified:
* tournament

Columns added:
* generation_mode VARCHAR(10) NOT NULL DEFAULT 'MANUAL'
* has_third_place_match BOOLEAN NOT NULL DEFAULT FALSE
* number_of_groups INTEGER NOT NULL DEFAULT 1
* players_advancing_per_group INTEGER NOT NULL DEFAULT 2

Constraints added:
* chk_tournament_format CHECK(format IN ('LEAGUE', 'BRACKET', 'LEAGUE_BRACKET'))
* chk_tournament_generation_mode CHECK(generation_mode IN ('AUTO', 'MANUAL'))

---

### V21__ADD_TOURNAMENT_ROUND_PHASE_TYPE.sql

Purpose:
Add phase_type to tournament_round

Tables modified:
* tournament_round

Columns added:
* phase_type VARCHAR(15) NOT NULL DEFAULT 'GROUP_STAGE'

Constraints added:
* chk_tournament_round_phase_type CHECK(phase_type IN ('GROUP_STAGE', 'KNOCKOUT'))

---

### V22__EXPAND_EVENT_FOR_KNOCKOUT.sql

Purpose:
Add penalties, bracket advancement fields, and self-referential FKs to event

Tables modified:
* event

Columns added:
* penalties_home INTEGER NULL
* penalties_away INTEGER NULL
* next_round_event_id BIGINT NULL
* home_source_event_id BIGINT NULL
* away_source_event_id BIGINT NULL

Constraints added:
* fk_event_next_round FK → event
* fk_event_home_source FK → event
* fk_event_away_source FK → event

---

### V23__ADD_TOURNAMENT_PLAYER_GROUP_NUMBER.sql

Purpose:
Add group_number to tournament_player for LEAGUE_BRACKET format

Tables modified:
* tournament_player

Columns added:
* group_number INTEGER NULL

---

### V24__ADD_PENALTIES_EVENT_STATUS.sql

Purpose:
Add PENALTIES to EventStatus enum constraint

Tables modified:
* event

Constraints added:
* chk_event_status CHECK(status IN ('CREATED', 'IN_PROGRESS', 'PENALTIES', 'COMPLETED', 'CANCELLED'))

---

### V25__ALTER_EVENT_MAKE_PLAYERS_NULLABLE.sql

Purpose:
Make player_home_id, player_away_id, game_datetime nullable

Tables modified:
* event

Columns modified:
* player_home_id DROP NOT NULL
* player_away_id DROP NOT NULL
* game_datetime DROP NOT NULL

---

### V26__ADD_TOURNAMENT_ROUND_GROUP_NUMBER.sql

Purpose:
Add group_number to tournament_round

Tables modified:
* tournament_round

Columns added:
* group_number INTEGER NULL

---

### V27__ADD_EVENT_THIRD_PLACE_MATCH.sql

Purpose:
Add is_third_place_match flag to event

Tables modified:
* event

Columns added:
* is_third_place_match BOOLEAN NOT NULL DEFAULT FALSE

---

### V28__ADD_TOURNAMENT_VERSION_AND_CONSTRAINTS.sql

Purpose:
Add version column to tournament, add unique constraint and indexes

Tables modified:
* tournament
* tournament_player

Columns added:
* version BIGINT DEFAULT 0 (to tournament)

Constraints added:
* uk_tournament_player UNIQUE (tournament_id, player_id)

Indexes created:
* idx_event_tournament_status ON event(tournament_id, status)
* idx_tournament_player_group ON tournament_player(tournament_id, group_number)

---

### V29__ADD_EVENT_REAL_FOOTBALL_COLUMNS.sql

Purpose:
Add real football columns to event

Tables modified:
* event

Columns added:
* team_home_id BIGINT FK → team
* team_away_id BIGINT FK → team
* external_match_id VARCHAR(20)

---

### V30__ADD_MARKET_TYPE.sql

Purpose:
Add market_type to market, add unique constraint (event_id, market_type)

Tables modified:
* market

Columns added:
* market_type VARCHAR(30) NOT NULL DEFAULT 'MATCH_RESULT'

Constraints added:
* market_event_market_type_key UNIQUE (event_id, market_type)

Constraints removed:
* market_event_id_key (if existed)

Data migrations:
* UPDATE market SET market_type = 'MATCH_RESULT' (backfill)

---

### V31__DROP_MULTIPLIER_NOT_NULL.sql

Purpose:
Make multiplier column nullable in tournament_round

Tables modified:
* tournament_round

Columns modified:
* multiplier DROP NOT NULL

---

### V32__DROP_UNIQUE_TEAM_ABBREVIATION.sql

Purpose:
Drop unique constraint on team.abbreviation

Tables modified:
* team

Constraints removed:
* team_abbreviation_key

---

### V33__CREATE_COMPETITION_TABLE.sql

Purpose:
Create competition table

Tables created:
* competition (id BIGSERIAL PK, uuid UUID NOT NULL UNIQUE, name VARCHAR(255) NOT NULL, season VARCHAR(20) NOT NULL, api_football_league_id VARCHAR(20) NOT NULL, api_football_country_id VARCHAR(20) NOT NULL, game_forecast_league_id VARCHAR(20) NOT NULL, active BOOLEAN NOT NULL DEFAULT true, created_at TIMESTAMP NOT NULL DEFAULT now(), UNIQUE (api_football_league_id, api_football_country_id, season))

Columns added:
* id, uuid, name, season, api_football_league_id, api_football_country_id, game_forecast_league_id, active, created_at

---

### V34__ADD_COMPETITION_TO_TOURNAMENT.sql

Purpose:
Add competition_id FK to tournament; create tournament_market_type table; insert default competition

Tables modified:
* tournament

Tables created:
* tournament_market_type (tournament_id BIGINT FK → tournament, market_type VARCHAR(50) NOT NULL, PRIMARY KEY (tournament_id, market_type))

Columns added:
* competition_id BIGINT FK → competition (to tournament)

Data migrations:
* INSERT default competition row (Copa do Mundo 2026)

---

### V35__ADD_GAMEFORECAST_TEAM_ID.sql

Purpose:
Replace external_api_id with api_football_team_id and game_forecast_team_id

Tables modified:
* team

Columns removed:
* external_api_id

Columns added:
* api_football_team_id VARCHAR(20) UNIQUE
* game_forecast_team_id VARCHAR(20) UNIQUE

---

### V36__CREATE_EXTERNAL_API_LOG.sql

Purpose:
Create external_api_log table for caching external API responses

Tables created:
* external_api_log (id BIGSERIAL PK, provider VARCHAR(30) NOT NULL, endpoint VARCHAR(100) NOT NULL, request_key VARCHAR(255) NOT NULL, response_body JSONB NOT NULL, status_code INT, fetched_at TIMESTAMP NOT NULL DEFAULT now())

Indexes created:
* idx_external_api_log_lookup ON (provider, endpoint, request_key, fetched_at DESC)

---

### V37__INSERT_EXTERNAL_DATA.sql

Purpose:
Insert seed data for external APIs (GameForecast predictions, API-Football events/standings, teams)

Tables seeded:
* external_api_log — GameForecast predictions and API-Football events/standings
* team — 48 real football teams with badges

Data migrations:
* INSERT INTO external_api_log (GameForecast events with prediction data for 2026 World Cup)
* INSERT INTO external_api_log (API-Football events and standings)
* INSERT INTO team (48 teams with name, abbreviation, badge_url, api_football_team_id, game_forecast_team_id)

---

### V999__SEED_LEGACY_DATA.sql

Purpose:
Seed legacy data for development/demo

Tables seeded:
* player (18 players: Alexandre, Bichas, Bruno, Conrado, Fitaroni, Francisco, Gustavo, Kadu, Leo, Luan, Lucas, Macario, Nicolas, Paulo, Puskas, Ribas, Tadeu, Vini)
* tournament (5 tournaments with UUIDs)
* tournament_round (19 rounds across 5 tournaments)
* tournament_player (48 tournament-player registrations with auto-generated IDs)
* event (events with scores, elo_before values)

---

### Schema Inconsistencies / Migration Drift

1. **player.name**: Entity has `@Column(name = "name")` with no `nullable = false`, but DB migration V4 has `name VARCHAR(255) NOT NULL`. Entity does not enforce NOT NULL at JPA level, but DB does.

2. **player.is_active**: Entity field type is `Boolean` (nullable wrapper), DB has `is_active BOOLEAN NOT NULL DEFAULT TRUE`. Entity's nullable annotation is absent, but the Java type allows null. DB will reject null inserts.

3. **tournament.type**: Entity has `TournamentType type` (nullable), but Migration V5 has `VARCHAR(50) NOT NULL DEFAULT 'FIFA_MATCH'`. The DB default will populate if null. Entity enum has `REAL_FOOTBALL` and `FIFA_MATCH`.

4. **tournament.format**: Entity has `TournamentFormat format` (nullable, no `@Column`), but V12 adds `format VARCHAR(50) NOT NULL DEFAULT 'LEAGUE'`. Not annotated on entity with nullable=false, but DB enforces it.

5. **tournament.status**: Entity has `TournamentStatus status` (nullable), but V5 has `status VARCHAR(50) NOT NULL`. DB enforces NOT NULL.

6. **tournament_round.multiplier**: Entity has `BigDecimal multiplier` (nullable), V31 explicitly made it nullable. Consistent.

7. **tournament_round.name**: Entity has `String name` (nullable), but V6 has `name VARCHAR(255) NOT NULL`. DB enforces NOT NULL.

8. **tournament_round.round_order**: Entity has `Integer roundOrder` (nullable), but V6 has `round_order INT NOT NULL`. DB enforces NOT NULL.

9. **event.tournament_id**: Entity `@JoinColumn(name = "tournament_id")` has no `nullable = false`, but V8 has `tournament_id BIGINT NOT NULL`. DB enforces NOT NULL.

10. **market.event_id**: Entity `@JoinColumn(name = "event_id")` has no `nullable = false`, but V9 has `event_id BIGINT NOT NULL`. DB enforces NOT NULL.

11. **market.name**: Entity `@Column(name = "name")` has no `nullable = false`, but V9 has `name VARCHAR(255) NOT NULL`. DB enforces NOT NULL.

12. **outcome.market_id**: Entity `@JoinColumn(name = "market_id")` has no `nullable = false`, but V10 has `market_id BIGINT NOT NULL`. DB enforces NOT NULL.

13. **outcome.name**: Entity `private String name` has no `nullable = false`, but V10 has `name VARCHAR(255) NOT NULL`. DB enforces NOT NULL.

14. **outcome.odd**: Entity `private BigDecimal odd` has no `nullable = false`, but V10 has `odd DECIMAL(10,2) NOT NULL`. DB enforces NOT NULL.

15. **bet_slip.***: Entity has no nullable=false annotations, but V19 specifies NOT NULL for all required columns.

16. **bet_slip_item.***: Same as bet_slip — entity lacks NOT NULL annotations but DB enforces them.

17. **table `session`**: Entity has no `@Table(name = ...)` so it uses default name "session". Migration V18 uses `IF NOT EXISTS` pattern. Consistent.

18. **ExternalApiLog schema**: Entity explicitly specifies `schema = "resenha"` in @Table, unlike all other entities which rely on the default schema. Consistent with application.properties default_schema setting.

19. **Event entity does NOT specify `nullable = false`** for fields that are required in DB. This is a consistent pattern across all entities — constraints are enforced at the DB level, not entity level.

---

## Section 8 — Legacy Migration Feasibility Matrix

Based on the V999 seed and project context (v1 was Node.js storing state in memory + JSON), the following mapping applies:

| Entity | Can be Generated From Legacy Data? | Source JSON | Missing Data Required |
|--------|-----------------------------------|-------------|----------------------|
| Competition | NO | N/A | Competition is a new concept introduced in V33. No legacy data exists. |
| User | PARTIAL | bettors[] or users[] | missing pinHash, salt, userType (assume USER), firstLogin (assume true). Legacy likely had name only. |
| Wallet | YES | bettors.wallet | legacy balance can be migrated |
| Player | YES | players[] | name, active (assume true), currentElo can be migrated |
| Team | NO | N/A | Team is a real football concept, no legacy FIFA data |
| Session | NO | N/A | Sessions are runtime-only, no legacy persistence |
| Tournament | YES | tournaments[] | name, type (FIFA_MATCH), format, status, dates |
| tournament_market_type | NO | N/A | New concept — market types not present in legacy |
| TournamentRound | YES | tournaments[].rounds | name, multiplier, roundOrder, phaseType (assume KNOCKOUT or GROUP_STAGE) |
| TournamentPlayer | YES | tournaments[].players[] | tournament_id, player_id mapping |
| Event | YES | tournaments[].events[] | tournament_id, round_id, player_home_id, player_away_id, scores, elo_before, status |
| Market | PARTIAL | events[].market | Legacy likely had implicit single market per event. marketType must default to MATCH_RESULT. Outcomes need to be reconstructed. |
| Outcome | PARTIAL | events[].outcomes | Legacy odds data can be mapped to 3 outcomes per market (home win, draw, away win). For knockout: 2 outcomes. |
| BetSlip | YES | bets[] or betSlips[] | user_id, tournament_id, stake, combined_odd, potential_return, status |
| BetSlipItem | YES | betSlipItems[] or bet selections | bet_slip_id, event_id, outcome_id, odd_snapshot, status |
| Transaction | PARTIAL | wallet.transactions[] | wallet_id, type, value, bet_slip_id (nullable). Legacy transaction types need mapping to TransactionType enum. |
| ExternalApiLog | NO | N/A | External API logging is new |

---

## Section 9 — SQL Examples

---

### Competition

```sql
INSERT INTO resenha.competition (uuid, name, season, api_football_league_id, api_football_country_id, game_forecast_league_id, active, created_at)
VALUES (:uuid, :name, :season, :apiFootballLeagueId, :apiFootballCountryId, :gameForecastLeagueId, :active, :createdAt);
```

---

### User

```sql
INSERT INTO resenha.users (name, pin_hash, salt, user_type, is_first_login, created_at)
VALUES (:name, :pinHash, :salt, :userType, :firstLogin, :createdAt);
```

---

### Wallet

```sql
INSERT INTO resenha.wallet (user_id, balance)
VALUES (:userId, :balance);
```

---

### Player

```sql
INSERT INTO resenha.player (name, is_active, current_elo, user_id)
VALUES (:name, :active, :currentElo, :userId);
```

---

### Team

```sql
INSERT INTO resenha.team (name, abbreviation, api_football_team_id, game_forecast_team_id, badge_url)
VALUES (:name, :abbreviation, :apiFootballTeamId, :gameForecastTeamId, :badgeUrl);
```

---

### Session

```sql
INSERT INTO resenha.session (user_id, token, created_at, expires_at)
VALUES (:userId, :token, :createdAt, :expiresAt);
```

---

### Tournament

```sql
INSERT INTO resenha.tournament (uuid, name, type, format, status, start_date, end_date, created_at, generation_mode, has_third_place_match, number_of_groups, players_advancing_per_group, version, competition_id)
VALUES (:uuid, :name, :type, :format, :status, :startDate, :endDate, :createdAt, :generationMode, :hasThirdPlaceMatch, :numberOfGroups, :playersAdvancingPerGroup, :version, :competitionId);
```

---

### tournament_market_type

```sql
INSERT INTO resenha.tournament_market_type (tournament_id, market_type)
VALUES (:tournamentId, :marketType);
```

---

### TournamentRound

```sql
INSERT INTO resenha.tournament_round (tournament_id, name, multiplier, round_order, phase_type, group_number)
VALUES (:tournamentId, :name, :multiplier, :roundOrder, :phaseType, :groupNumber);
```

---

### TournamentPlayer

```sql
INSERT INTO resenha.tournament_player (tournament_id, player_id, team_id, group_number)
VALUES (:tournamentId, :playerId, :teamId, :groupNumber);
```

---

### Event

```sql
INSERT INTO resenha.event (tournament_id, round_id, player_home_id, player_away_id, team_home_id, team_away_id, external_match_id, game_datetime, status, home_score, away_score, home_elo_before, away_elo_before, is_knockout, is_bye, penalties_home, penalties_away, next_round_event_id, home_source_event_id, away_source_event_id, is_third_place_match)
VALUES (:tournamentId, :roundId, :playerHomeId, :playerAwayId, :teamHomeId, :teamAwayId, :externalMatchId, :gameDatetime, :status, :homeScore, :awayScore, :homeEloBefore, :awayEloBefore, :isKnockout, :isBye, :penaltiesHome, :penaltiesAway, :nextRoundEventId, :homeSourceEventId, :awaySourceEventId, :thirdPlaceMatch);
```

---

### Market

```sql
INSERT INTO resenha.market (event_id, name, status, market_type)
VALUES (:eventId, :name, :status, :marketType);
```

---

### Outcome

```sql
INSERT INTO resenha.outcome (market_id, name, odd)
VALUES (:marketId, :name, :odd);
```

---

### BetSlip

```sql
INSERT INTO resenha.bet_slip (user_id, tournament_id, stake, combined_odd, potential_return, status, created_at)
VALUES (:userId, :tournamentId, :stake, :combinedOdd, :potentialReturn, :status, :createdAt);
```

---

### BetSlipItem

```sql
INSERT INTO resenha.bet_slip_item (bet_slip_id, event_id, outcome_id, odd_snapshot, status)
VALUES (:betSlipId, :eventId, :outcomeId, :oddSnapshot, :status);
```

---

### Transaction

```sql
INSERT INTO resenha.transaction (wallet_id, bet_slip_id, type, value, created_at)
VALUES (:walletId, :betSlipId, :type, :value, :createdAt);
```

---

### ExternalApiLog

```sql
INSERT INTO resenha.external_api_log (provider, endpoint, request_key, response_body, status_code, fetched_at)
VALUES (:provider, :endpoint, :requestKey, :responseBody::jsonb, :statusCode, :fetchedAt);
```

---

## Section 10 — Hidden Constraints

---

### Business Rules Enforced at Service Layer (Not in Database)

#### Player
1. **Player name is effectively required** (service layer `PlayerRequestDTO` has no validation annotation, but DB column is NOT NULL).
2. **Player can only be linked to one User**: `PlayerServiceImpl.linkUser()` checks `playerRepository.existsByUserId(user.getId())` before allowing link. Also with unique constraint on user_id.
3. **Player can only be linked once**: `PlayerServiceImpl.linkUser()` checks `player.getUser() != null` before allowing link.
4. **Only admin can create/update/link players**: `PlayerServiceImpl.create()`, `update()`, `linkUser()` all call `currentUserContext.requireAdmin()`.

#### User
5. **User name must be unique and not blank**: `UserServiceImpl.create()` validates name is not null/blank, then checks `userRepository.existsByName(name)`. DB also has UNIQUE constraint.
6. **User can only be created by admin or via public endpoint**: `POST /api/v1/users` is listed as public.
7. **PIN must be exactly 4 numeric digits**: `PinServiceImpl.validatePin()` checks `rawPin.matches("\\d{4}")`.
8. **PIN uses SHA-256 with per-user salt**: Not a DB constraint, but a hashing requirement.
9. **First login flag**: When `user.isFirstLogin()` is true, login sets it to false and saves. PIN update also clears it.
10. **Admin is seeded on first startup**: `PlayerInitializer` checks `userRepository.existsByUserType(UserType.ADMIN)` before creating admin.

#### Wallet
11. **Wallet is auto-created when User is created**: `UserServiceImpl.create()` creates a Wallet with balance 0 alongside the User.
12. **Deposit amount must have max 2 decimal places**: `WalletServiceImpl.deposit()` validates `amount.scale() > 2`.
13. **Deposit amount must be greater than zero**: `WalletServiceImpl.deposit()` validates `amount.compareTo(BigDecimal.ZERO) <= 0`.
14. **Only admin can deposit**: `WalletServiceImpl.deposit()` calls `currentUserContext.requireAdmin()`.

#### Tournament
15. **Tournament name is required**: `TournamentRequestDTO` has `@NotNull` on name.
16. **Tournament format is required**: `TournamentRequestDTO` has `@NotNull` on format.
17. **Competition is required for REAL_FOOTBALL type**: `TournamentServiceImpl.create()` checks `competitionId == null` for REAL_FOOTBALL and throws BusinessException.
18. **Competition must be null for FIFA_MATCH type**: `TournamentServiceImpl.create()` throws BusinessException if competitionId is provided for FIFA_MATCH.
19. **Tournament must be in CREATED status to add players**: `TournamentServiceImpl.addPlayerToTournament()` checks `tournament.getStatus() != TournamentStatus.CREATED`.
20. **Tournament must have at least 2 players to start**: `TournamentServiceImpl.startTournament()` checks `n < 2`.
21. **Tournament cannot be started twice**: `startTournament()` checks `status != CREATED`.
22. **LEAGUE_BRACKET requires group config**: `startTournament()` validates numberOfGroups >= 1 and playersAdvancingPerGroup >= 1.
23. **LEAGUE_BRACKET advancing players must be power of 2**: `getRoundsPerGroup()` validates `(totalAdvancing & (totalAdvancing - 1)) != 0`.
24. **advanceToBracket requires all GROUP_STAGE events COMPLETED or CANCELLED**: Service checks every group stage event.
25. **Only admin can create/start/modify tournaments**: `currentUserContext.requireAdmin()`.

#### TournamentPlayer
26. **Player cannot be added twice to same tournament**: `TournamentPlayerRepository.existsByTournamentIdAndPlayerId()` check, plus `DataIntegrityViolationException` catch, plus DB unique constraint `uk_tournament_player`.
27. **Team can only be set after registration**: PATCH endpoint for team assignment.

#### Event
28. **Business invariant for tournament types (not enforced in DB)**: For FIFA_MATCH events, playerHome and playerAway must be non-null, teamHome/teamAway must be null. For REAL_FOOTBALL, teamHome/teamAway must be non-null, playerHome/playerAway must be null. This is validated in the service layer `EventService` (not shown in detail but documented in `LEGACY_REAL_FOOTBALL_BRIEFING.md`).
29. **Market must be OPEN to accept bets**: `BetServiceImpl.placeBet()` checks `market.getStatus() != MarketStatus.OPEN`.
30. **Event status transitions**: CREATED → IN_PROGRESS → COMPLETED/CANCELLED. PENALTIES status exists for knockout matches.

#### BetSlip
31. **Bet must belong to a single tournament**: All items must reference events from the same tournament (validated in `BetServiceImpl.placeBet()`).
32. **No duplicate event picks in same slip**: Implied by business logic (no explicit check in code shown, but constraint via unique index on bet_slip_item not present in DB).
33. **Cannot bet on same events in multiple pending slips**: `BetServiceImpl.placeBet()` checks `betSlipRepository.findByUserAndEventIdsAndStatus()` for existing pending slips.
34. **Stake must be greater than zero**: `BetServiceImpl.placeBet()` validates `stake.compareTo(BigDecimal.ZERO) <= 0`.
35. **Stake must not exceed wallet balance**: `BetServiceImpl.placeBet()` checks `wallet.getBalance().compareTo(stake) < 0`.
36. **Wallet balance is deducted at bet placement**: BetServiceImpl subtracts stake from wallet and creates a BET_PLACED transaction.
37. **Winning bets are paid from the prize pool**: `resolveBetsForEvent()` credits `potentialReturn` to wallet and creates BET_WON transaction.

#### Bet Resolution
38. **Knockout events require penalty scores if tied**: `BetServiceImpl.buildWinningNamesByType()` throws BusinessException if knockout event tied and penalties are null.
39. **BetSlip resolution**: All items must be resolved (none PENDING) and none LOST for the slip to be WON. If any item LOST, the whole slip is LOST.
40. **If all remaining items are CANCELLED, stake is refunded**: `cancelBetsForEvent()` refunds stake to wallet and creates BET_REFUND transaction.
41. **Market status changes to CLOSED after resolution**: `resolveBetsForEvent()` sets all markets to CLOSED.

#### Outcome Naming Convention
42. **For non-knockout events**:
    * `MATCH_RESULT`: outcomes are named after player names or team names (home, away) and "Empate" (draw)
    * `OVER_UNDER_25`: "Over 2.5", "Under 2.5"
    * `OVER_UNDER_35`: "Over 3.5", "Under 3.5"
    * `BTTS`: "Sim", "Não"
    * `EXACT_SCORE`: formatted as `{homeScore}-{awayScore}` (e.g., "2-1")
43. **For knockout events**:
    * No draw outcome — only home and away player/team names
44. **Winning outcome naming for resolution**:
    * Match result winner = the player/team name for home or away, "Empate" for draw
    * Penalty resolution uses `penaltiesHome` vs `penaltiesAway` to determine winner

#### Session
45. **Session expires after 24 hours**: `AuthServiceImpl.login()` sets `expiresAt` to 24 hours from now.
46. **Login verifies PIN**: `AuthServiceImpl.login()` calls `pinService.verifyPin()`.

#### Competition
47. **Competition must be active to be used**: `CompetitionRepository.findByActiveTrue()` filters.
48. **REAL_FOOTBALL tournaments require a competition reference**: Service-layer validation.
49. **External API IDs for competition must be unique per (league_id, country_id, season)**: DB unique constraint.

#### Auditing
50. **createdAt is set by @CreationTimestamp**: Hibernate auto-populates on persist. For Competition, it uses `@Builder.Default = LocalDateTime.now()`.
