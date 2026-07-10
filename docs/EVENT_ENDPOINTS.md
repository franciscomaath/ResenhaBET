# Event Endpoints

## 1. Insert Completed Event

`POST /api/v1/events/completed`

Creates an event already finished with scores. Supports normal matches and bye matches.

### Request DTO: `CompletedEventRequestDTO`

| Field | Type | Required | Notes |
|---|---|---:|---|
| `tournamentId` | `Long` | Yes | Tournament that owns the event. |
| `roundId` | `Long` | Yes | Round where the event belongs. |
| `playerHomeId` | `Long` | No | Home player. For bye matches, exactly one of `playerHomeId` or `playerAwayId` must be provided. |
| `playerAwayId` | `Long` | No | Away player. For bye matches, exactly one of `playerHomeId` or `playerAwayId` must be provided. |
| `homeScore` | `Integer` | Yes | Final home score. |
| `awayScore` | `Integer` | Yes | Final away score. |
| `gameDatetime` | `LocalDateTime` | No | If omitted, server uses the current date/time. |
| `isBye` | `Boolean` | No | When `true`, the event is treated as a bye match. |

### Response

Returns `EventResponseDTO`.

### Behavior

- Creates the event with status `COMPLETED`.
- Applies the same completion flow used by `finishEvent`.
- Recalculates odds and other event-driven side effects after completion.
- Bye matches are accepted when `isBye = true` and exactly one player is provided.

---

## 2. Reopen Completed Event

`POST /api/v1/events/{id}/reopen`

Reopens a completed event so the admin can adjust the result and finish it again.

### Request Body

None.

### Response

Returns `EventResponseDTO`.

### Behavior

- Only events in `COMPLETED` status can be reopened.
- Changes the event status to `IN_PROGRESS`.
- Rolls back bet resolution for the event.
- Restores affected bet slips and items to `PENDING`.
- Reverses winning wallet credits and soft-deletes the related winning transactions.
- Keeps markets closed.
- Reverts tournament completion state when needed.
- Recalculates Elo after the rollback.

---

## 3. Reset In-Progress Event

`POST /api/v1/events/{id}/reset`

Returns an in-progress event to `CREATED` so the admin can start it again.

### Request Body

None.

### Response

Returns `EventResponseDTO`.

### Behavior

- Only events in `IN_PROGRESS` status can be reset.
- Changes the event status back to `CREATED`.
- Clears score fields and penalty fields.
- Clears Elo snapshot fields.
- Reopens the event markets.

## Common `EventResponseDTO` Fields

These endpoints return the same event response shape.

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` | Event identifier. |
| `tournamentId` | `Long` | Tournament identifier. |
| `roundId` | `Long` | Round identifier. |
| `playerHomeId` | `Long` | Home player id. |
| `playerHomeName` | `String` | Home player name. |
| `playerAwayId` | `Long` | Away player id. |
| `playerAwayName` | `String` | Away player name. |
| `teamHomeId` | `Long` | Home team id, when applicable. |
| `teamHomeName` | `String` | Home team name, when applicable. |
| `teamAwayId` | `Long` | Away team id, when applicable. |
| `teamAwayName` | `String` | Away team name, when applicable. |
| `externalMatchId` | `String` | External provider match id, when present. |
| `gameDatetime` | `LocalDateTime` | Scheduled/played date. |
| `status` | `String` | Event status. |
| `homeScore` | `Integer` | Home score. |
| `awayScore` | `Integer` | Away score. |
| `isKnockout` | `Boolean` | Knockout flag. |
| `isBye` | `Boolean` | Bye flag. |
| `penaltiesHome` | `Integer` | Penalty score for home side. |
| `penaltiesAway` | `Integer` | Penalty score for away side. |
| `nextRoundEventId` | `Long` | Next knockout event, when linked. |
| `homeSourceEventId` | `Long` | Source event for the home slot. |
| `awaySourceEventId` | `Long` | Source event for the away slot. |
| `isThirdPlaceMatch` | `boolean` | Third-place match flag. |
