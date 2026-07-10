# ResenhaBET — Betting, Markets & Odds System Reference

> This document describes every endpoint, entity, enum, and business rule related to the betting/market/outcomes system, the Elo-based odds calculator, and the bet resolution flow. It is intended for an AI agent implementing frontend visual changes.

---

## Table of Contents

1. [Overview](#overview)
2. [Entities & Enums](#entities--enums)
3. [Odds Calculation](#odds-calculation)
4. [Elo System](#elo-system)
5. [Event Lifecycle & Market Status](#event-lifecycle--market-status)
6. [API Endpoints](#api-endpoints)
   - [Events](#events)
   - [Markets](#markets)
   - [Bets](#bets)
   - [Wallet](#wallet)
7. [Business Rules Summary](#business-rules-summary)
8. [Frontend Integration Notes](#frontend-integration-notes)

---

## Overview

ResenhaBET is a private FIFA tournament platform with a betting system. The betting flow is:

1. An **Event** (match) is created for a tournament round → a **Market** with 3 **Outcomes** is auto-generated with Elo-based odds
2. Users place **Bets** (BetSlips) on outcomes while the market is OPEN
3. When the event **starts**, the market is **CLOSED** (no more bets)
4. When the event **finishes**, bets are **resolved** automatically: winning outcomes credit the user's wallet, losing outcomes are marked LOST
5. Elo ratings are updated after each event

---

## Entities & Enums

### EventStatus

| Value | Meaning |
|---|---|
| `CREATED` | Match created, market is OPEN, bets accepted |
| `IN_PROGRESS` | Match started, market is CLOSED, no bets accepted |
| `COMPLETED` | Match finished, bets resolved, Elo updated |
| `CANCELLED` | Match cancelled |

### MarketStatus

| Value | Meaning | Can users bet? |
|---|---|---|
| `OPEN` | Accepting bets | ✅ Yes |
| `SUSPENDED` | Temporarily paused (reserved for future) | ❌ No |
| `CLOSED` | Bets locked (match started or finished) | ❌ No |

### BetSlipStatus

| Value | Meaning |
|---|---|
| `PENDING` | Bilhete awaiting resolution (some items not yet decided) |
| `WON` | All items won → wallet credited with `potentialReturn` |
| `LOST` | At least one item lost |
| `CANCELLED` | Reserved for future use (event cancelled, etc.) |

### BetSlipItemStatus

| Value | Meaning |
|---|---|
| `PENDING` | Item awaiting event result |
| `WON` | The outcome matched the event result |
| `LOST` | The outcome did not match the event result |

### TransactionType

| Value | Meaning |
|---|---|
| `DEPOSIT` | Admin deposited funds into wallet |
| `WITHDRAWAL` | Reserved for future use |
| `BET_PLACED` | Stake deducted from wallet when bet is placed |
| `BET_WON` | Potential return credited to wallet when bet slip wins |

---

### Key Entities

#### Market
```
Market {
  id: Long
  event: Event          // Each event has exactly 1 market
  name: String          // Always "Resultado Final"
  status: MarketStatus  // OPEN | SUSPENDED | CLOSED
}
```

#### Outcome (3 per market)
```
Outcome {
  id: Long
  market: Market
  name: String          // "Vitória Casa" | "Empate" | "Vitória Fora"
  odd: BigDecimal       // Decimal odds (e.g. 2.50)
}
```

**Outcome names and their meaning:**
| Outcome Name | Meaning |
|---|---|
| `Vitória Casa` | Home player wins (homeScore > awayScore) |
| `Empate` | Draw (homeScore == awayScore) |
| `Vitória Fora` | Away player wins (homeScore < awayScore) |

#### BetSlip (the betting ticket)
```
BetSlip {
  id: Long
  user: User
  tournament: Tournament          // All items must be from same tournament
  stake: BigDecimal               // Amount wagered (deducted 1x from wallet)
  combinedOdd: BigDecimal         // Product of all item odds (odd1 × odd2 × odd3...)
  potentialReturn: BigDecimal     // stake × combinedOdd (what user wins if all items hit)
  status: BetSlipStatus
  createdAt: LocalDateTime
  items: List<BetSlipItem>        // 1..N selections
}
```

#### BetSlipItem (each selection in a ticket)
```
BetSlipItem {
  id: Long
  betSlip: BetSlip
  event: Event
  outcome: Outcome
  oddSnapshot: BigDecimal         // Odd value locked at bet placement time
  status: BetSlipItemStatus
}
```

#### Transaction
```
Transaction {
  id: Long
  wallet: Wallet
  betSlip: BetSlip?               // Nullable. Linked when type is BET_PLACED or BET_WON
  type: TransactionType
  value: BigDecimal
  createdAt: LocalDateTime
}
```

---

## Odds Calculation

### Algorithm (5-step process)

The odds for each event are calculated when the event is **created** and stored as `Outcome.odd`. They are NOT recalculated dynamically.

**Step 1 — Elo-based base probability:**
```
homeBase = 1 / (1 + 10^((eloAway - eloHome) / eloScale))
awayBase = 1 - homeBase
```
- Default `eloScale` = 400

**Step 2 — Draw probability:**
```
drawProbability = drawFactor × (1 - |homeBase - awayBase|)
homeElo = homeBase - (drawProbability / 2)
awayElo = awayBase - (drawProbability / 2)
```
- Default `drawFactor` = 0.28

**Step 3 — H2H blending (if past matches exist):**
```
h2hWeight = min(totalMatches / 10, maxH2hWeight)   // capped at 0.20
eloWeight = 1 - h2hWeight

homeFinal = (homeElo × eloWeight) + (homeH2h × h2hWeight)
drawFinal = (drawProbability × eloWeight) + (drawH2h × h2hWeight)
awayFinal = (awayElo × eloWeight) + (awayH2h × h2hWeight)
```
- `homeH2h` = homeWins / totalMatches
- H2H is based on direct confrontations between the two players in completed events
- Max last 10 matches considered (`h2hMatchLimit`)

**Step 4 — Normalization:**
```
total = homeFinal + drawFinal + awayFinal
homeProb = homeFinal / total
drawProb = drawFinal / total
awayProb = awayFinal / total
```

**Step 5 — Decimal odds:**
```
homeOdd = max(1 / homeProb, minOdd)    // rounded to 2 decimals
drawOdd = max(1 / drawProb, minOdd)
awayOdd = max(1 / awayProb, minOdd)
```
- Default `minOdd` = 1.05

### Configuration (`application.properties`)

| Property | Default | Description |
|---|---|---|
| `resenhabet.odds.elo-scale` | 400 | Scale factor for Elo probability calculation |
| `resenhabet.odds.draw-factor` | 0.28 | How likely a draw is relative to skill difference |
| `resenhabet.odds.max-h2h-weight` | 0.20 | Maximum weight given to head-to-head record |
| `resenhabet.odds.min-odd` | 1.05 | Minimum allowed odd value (floor) |
| `resenhabet.odds.h2h-match-limit` | 10 | Max number of past H2H matches considered |

### Example

For two players with Elo 1200 vs 1000 and no H2H history:
- Home probability ≈ 0.594 → odd ≈ 1.68
- Draw probability ≈ 0.178 → odd ≈ 5.63
- Away probability ≈ 0.228 → odd ≈ 4.38

---

## Elo System

### Elo Formula

Standard Elo with K=32 × round multiplier:

```
expectedScore = 1 / (1 + 10^((opponentElo - playerElo) / 400))
newElo = currentElo + K × (actualScore - expectedScore)
```

| Outcome | actualScore |
|---|---|
| Win | 1.0 |
| Draw | 0.5 |
| Loss | 0.0 |

- K-factor = 32 × `round.multiplier` (default multiplier = 1.0)
- Initial Elo = 1000
- Elo is applied per-event after bet resolution

### When Elo Updates

Elo updates happen in `finishEvent` AFTER bet resolution, using the Elo values **before** the match (`homeEloBefore`, `awayEloBefore`).

---

## Event Lifecycle & Market Status

### State Machine

```
CREATED ──[startEvent]──> IN_PROGRESS ──[finishEvent]──> COMPLETED
                                                         │
                                            [bet resolution]
                                            [market → CLOSED]
                                            [Elo update]
```

### Market Status Transitions

| Trigger | Market Status | Notes |
|---|---|---|
| Event created | OPEN | Bets accepted |
| `startEvent` called | CLOSED | No more bets. Happens automatically when admin starts the match |
| `finishEvent` called | CLOSED | Stays closed. Bets resolved |
| Admin `POST /markets/{eventId}/status` | OPEN or CLOSED | Manual override. Cannot open a completed event's market |

### Bet Resolution Flow (on `finishEvent`)

```
1. Determine winner by score:
   homeScore > awayScore → "Vitória Casa"
   homeScore == awayScore → "Empate"
   homeScore < awayScore → "Vitória Fora"

2. Resolve each BetSlipItem PENDING for this event:
   - If item.outcome matches winner → item.status = WON
   - Otherwise → item.status = LOST

3. For each affected BetSlip:
   - If ANY item is LOST → BetSlip.status = LOST
   - If ALL items are resolved and ALL WON → BetSlip.status = WON
     → wallet.balance += potentialReturn
     → Transaction(type=BET_WON, value=potentialReturn)
   - If some items still PENDING → BetSlip.status stays PENDING (waiting for other events)

4. Market status → CLOSED

5. Elo update for both players
```

---

## API Endpoints

### Events

#### POST `/api/v1/events`
Create a new event (admin). Auto-generates a Market with 3 Outcomes.

**Request:**
```json
{
  "tournamentId": 1,
  "roundId": 1,
  "playerHomeId": 10,
  "playerAwayId": 20,
  "gameDatetime": "2026-07-15T20:00:00"
}
```

**Response:** `EventResponseDTO`
```json
{
  "id": 5,
  "tournamentId": 1,
  "roundId": 1,
  "playerHomeId": 10,
  "playerAwayId": 20,
  "gameDatetime": "2026-07-15T20:00:00",
  "status": "CREATED",
  "homeScore": 0,
  "awayScore": 0,
  "isKnockout": false,
  "isBye": false
}
```

**Side effects:**
- Creates `Event` with status `CREATED`
- Creates `Market` with status `OPEN`, name "Resultado Final"
- Creates 3 `Outcome`s: "Vitória Casa", "Empate", "Vitória Fora" with Elo-based odds

---

#### GET `/api/v1/events`
List all events. Optional filter: `?tournamentId=1`.

**Response:** Array of `EventResponseDTO`

---

#### GET `/api/v1/events/{id}`
Get a single event.

**Response:** `EventResponseDTO`

---

#### POST `/api/v1/events/{id}/start`
Start an event (admin). Changes status to `IN_PROGRESS`.

**Side effects:**
- `event.status` → `IN_PROGRESS`
- `market.status` → `CLOSED` (betting blocked)

**Validations:**
- Event must be `CREATED`
- Event must have a round

---

#### POST `/api/v1/events/{id}/score`
Update the score of an in-progress event (admin).

**Request:**
```json
{
  "homeScore": 3,
  "awayScore": 1
}
```

**Validations:**
- Event must be `IN_PROGRESS`

---

#### POST `/api/v1/events/{id}/end`
Finish an event (admin). Changes status to `COMPLETED`.

**Side effects (in order, all in one `@Transactional`):**
1. `event.status` → `COMPLETED`
2. Bet resolution (see [Bet Resolution Flow](#bet-resolution-flow-on-finishevent))
3. `market.status` → `CLOSED` (if not already)
4. Elo calculation and update for both players

**Validations:**
- Event must be `IN_PROGRESS`
- `homeScore` and `awayScore` must not be null

---

### Markets

#### GET `/api/v1/markets/{eventId}`
Get the market (with outcomes and odds) for an event.

**Response:** `MarketResponseDTO`
```json
{
  "id": 1,
  "eventId": 5,
  "name": "Resultado Final",
  "status": "OPEN",
  "outcomes": [
    {
      "id": 1,
      "name": "Vitória Casa",
      "odd": 1.68
    },
    {
      "id": 2,
      "name": "Empate",
      "odd": 5.63
    },
    {
      "id": 3,
      "name": "Vitória Fora",
      "odd": 4.38
    }
  ]
}
```

**When `status` is `OPEN`:** Users can place bets on these outcomes.
**When `status` is `CLOSED`:** No bets accepted. Display as "locked" or "closed" in the UI.

---

#### POST `/api/v1/markets/{eventId}/status`
Set market status (admin). Opens or closes the market for betting.

**Request:**
```json
{
  "status": "CLOSED"
}
```

Valid values: `"OPEN"` or `"CLOSED"` (case-sensitive).

**Response:** `MarketResponseDTO` (with updated status)

**Validations:**
- `status` must be `"OPEN"` or `"CLOSED"` (returns 400 for anything else)
- Cannot open a market for a `COMPLETED` event (returns 400 BusinessException)
- Market must exist for the event (returns 404)

**Use cases:**
- Admin manually closes a market before the match starts (e.g., suspicious activity)
- Admin reopens a market that was accidentally closed

---

### Bets

#### POST `/api/v1/bets`
Place a bet (authenticated user). Creates a BetSlip with one or more selections.

**Request (single bet):**
```json
{
  "tournamentId": 1,
  "stake": 50.00,
  "items": [
    {
      "eventId": 5,
      "outcomeId": 1
    }
  ]
}
```

**Request (multiple/parlay bet):**
```json
{
  "tournamentId": 1,
  "stake": 25.00,
  "items": [
    {
      "eventId": 5,
      "outcomeId": 1
    },
    {
      "eventId": 6,
      "outcomeId": 8
    },
    {
      "eventId": 7,
      "outcomeId": 12
    }
  ]
}
```

**Response:** `BetSlipResponseDTO`
```json
{
  "id": 10,
  "userId": 2,
  "tournamentId": 1,
  "stake": 25.00,
  "combinedOdd": 20.53,
  "potentialReturn": 513.25,
  "status": "PENDING",
  "createdAt": "2026-07-15T18:30:00",
  "items": [
    {
      "id": 15,
      "eventId": 5,
      "outcomeId": 1,
      "outcomeName": "Vitória Casa",
      "oddSnapshot": 1.68,
      "status": "PENDING",
      "event": { ... }
    },
    {
      "id": 16,
      "eventId": 6,
      "outcomeId": 8,
      "outcomeName": "Empate",
      "oddSnapshot": 3.45,
      "status": "PENDING",
      "event": { ... }
    },
    {
      "id": 17,
      "eventId": 7,
      "outcomeId": 12,
      "outcomeName": "Vitória Fora",
      "oddSnapshot": 3.54,
      "status": "PENDING",
      "event": { ... }
    }
  ]
}
```

**Validations (in order):**
1. `tournamentId` must exist
2. `items` must not be empty
3. No duplicate `eventId` in the same bet slip (one bet per event)
4. All events must exist
5. All events must belong to the specified tournament
6. User must not have another PENDING bet slip with any of the same events
7. All markets for the events must be `OPEN`
8. Each outcome must belong to the market of its event
9. `stake` must be > 0 with max 2 decimal places
10. User's wallet balance must be ≥ stake

**Side effects (all in `@Transactional`):**
- `wallet.balance -= stake`
- `Transaction(type=BET_PLACED, value=stake, betSlip=betSlip)`
- `combinedOdd = odd1 × odd2 × odd3 × ...` (rounded to 2 decimals)
- `potentialReturn = stake × combinedOdd` (rounded to 2 decimals)

---

#### GET `/api/v1/bets/me`
Get the authenticated user's bet history.

**Response:** Array of `BetSlipResponseDTO`, ordered by `createdAt` descending.

Each slip includes its `items` with nested `event` data.

---

#### GET `/api/v1/bets?eventId={id}`
Get all bet slips for a specific event (admin only).

**Response:** Array of `BetSlipResponseDTO`

**Authentication:** Requires admin (`userType = ADMIN`).

---

### Wallet

#### GET `/api/v1/wallet?userId={id}`
Get wallet balance.

**Response:** `WalletResponseDTO`
```json
{
  "userId": 2,
  "balance": 150.00
}
```

#### POST `/api/v1/wallet/deposit`
Deposit into a user's wallet (admin only).

**Request:**
```json
{
  "userId": 2,
  "amount": 100.00
}
```

#### POST `/api/v1/wallet/deposit-all?amount=50.00`
Deposit the same amount into all wallets (admin only).

---

## Business Rules Summary

### Bet Placement Rules
| Rule | Description |
|---|---|
| Market must be OPEN | `market.status == OPEN` is required to place bets |
| Same tournament only | All events in a BetSlip must belong to the same tournament |
| No duplicate events | Each `eventId` can appear only once per BetSlip |
| No overlapping pending | User cannot have another PENDING bet slip with overlapping events |
| Sufficient balance | `wallet.balance >= stake` |
| Stake > 0 | Minimum stake is 0.01 |
| Max 2 decimal places | Both stake and odd values are rounded to 2 decimal places |

### Combined Odd Calculation
```
combinedOdd = product of all item.oddSnapshot values
potentialReturn = stake × combinedOdd

Example: 3 items with odds 1.68, 3.45, 3.54
combinedOdd = 1.68 × 3.45 × 3.54 = 20.53 (rounded)
potentialReturn = 25.00 × 20.53 = 513.25
```

### Bet Resolution Rules
| Condition | BetSlip Result | Wallet Effect |
|---|---|---|
| Any item LOST | `LOST` | None (stake already deducted) |
| All items WON | `WON` | `wallet.balance += potentialReturn` + Transaction(BET_WON) |
| Some items PENDING (waiting for other events) | `PENDING` | None (waiting) |

### Market Auto-Close
- Market is automatically **CLOSED** when `startEvent` is called
- Market is also set to **CLOSED** during bet resolution in `finishEvent`
- Admin can manually open/close via `POST /markets/{eventId}/status`
- Cannot open a market for a `COMPLETED` event

---

## Frontend Integration Notes

### Betting UI Flow

1. **View markets:** `GET /api/v1/markets/{eventId}` → show odds for each outcome
2. **Check market status:** If `status !== "OPEN"`, show "Betting closed" badge and disable bet button
3. **Place bet:** `POST /api/v1/bets` with selected outcomes and stake
4. **Check balance:** `GET /api/v1/wallet?userId={id}` before placing bet
5. **View bet history:** `GET /api/v1/bets/me` → show slips with item statuses

### Key UI States for BetSlip Items

| `item.status` | `betSlip.status` | Visual |
|---|---|---|
| PENDING | PENDING | Gray/neutral — waiting for event to finish |
| WON | WON | Green — winning bet, wallet credited |
| LOST | LOST | Red — losing bet |
| WON | PENDING | Yellow/partial — this item won, but slip still waiting for other events |
| LOST | PENDING | Red — this item lost means slip will be LOST once all items resolve |

### Key UI States for Markets

| `market.status` | Event Status | Visual |
|---|---|---|
| OPEN | CREATED | Green badge "Open" — accept bets |
| CLOSED | IN_PROGRESS | Red badge "Live" — match in progress, no bets |
| CLOSED | COMPLETED | Gray badge "Finished" — show final result |
| SUSPENDED | any | Yellow badge "Suspended" — temporarily paused |

### Admin Views

| Action | Endpoint | Notes |
|---|---|---|
| Start match | `POST /events/{id}/start` | Auto-closes market |
| Update score | `POST /events/{id}/score` | During match |
| Finish match | `POST /events/{id}/end` | Resolves bets + updates Elo |
| Override market status | `POST /markets/{eventId}/status` | Manually open/close market |
| View all bets for event | `GET /bets?eventId={id}` | Admin only |
| Deposit to wallet | `POST /wallet/deposit` | Admin only |

### Error Codes

| HTTP Status | Exception | When |
|---|---|---|
| 400 | BusinessException | Market not OPEN, duplicate event, overlapping pending bets |
| 402 | InsufficientFundsException | Wallet balance < stake |
| 404 | ResourceNotFoundException | Event/tournament/outcome/wallet not found |
| 409 | InvalidStateException | Event not in correct status for action |

### Response Field Reference

#### BetSlipResponseDTO
| Field | Type | Description |
|---|---|---|
| `id` | Long | BetSlip ID |
| `userId` | Long | User who placed the bet |
| `tournamentId` | Long | Tournament all items belong to |
| `stake` | BigDecimal | Amount wagered |
| `combinedOdd` | BigDecimal | Product of all item odds |
| `potentialReturn` | BigDecimal | `stake × combinedOdd` |
| `status` | String | `PENDING`, `WON`, `LOST`, `CANCELLED` |
| `createdAt` | LocalDateTime | When the bet was placed |
| `items` | Array[BetSlipItemResponseDTO]Nested selections|

#### BetSlipItemResponseDTO
| Field | Type | Description |
|---|---|---|
| `id` | Long | Item ID |
| `eventId` | Long | Event this item refers to |
| `outcomeId` | Long | Outcome selected |
| `outcomeName` | String | "Vitória Casa", "Empate", or "Vitória Fora" |
| `oddSnapshot` | BigDecimal | Odd at the time of bet placement (locked) |
| `status` | String | `PENDING`, `WON`, `LOST` |
| `event` | EventResponseDTO | Nested event data |

#### MarketResponseDTO
| Field | Type | Description |
|---|---|---|
| `id` | Long | Market ID |
| `eventId` | Long | Associated event |
| `name` | String | Always "Resultado Final" |
| `status` | String | `OPEN`, `SUSPENDED`, `CLOSED` |
| `outcomes` | Array[OutcomeResponseDTO] | The 3 betting options with odds |

#### OutcomeResponseDTO
| Field | Type | Description |
|---|---|---|
| `id` | Long | Outcome ID (used in `outcomeId` for placing bets) |
| `name` | String | "Vitória Casa", "Empate", "Vitória Fora" |
| `odd` | BigDecimal | Current decimal odd (may change after market creation; snapshot at bet time is used for payout) |

---

## Database Schema (Relevant Tables)

```sql
-- Each event has one market
market (id, event_id, name, status)

-- Each market has 3 outcomes
outcome (id, market_id, name, odd)

-- Each bet slip belongs to a user and tournament
bet_slip (id, user_id, tournament_id, stake, combined_odd, potential_return, status, created_at)

-- Each item is one selection within a bet slip
bet_slip_item (id, bet_slip_id, event_id, outcome_id, odd_snapshot, status)

-- Transactions track wallet movements
transaction (id, wallet_id, bet_slip_id, type, value, created_at)
```