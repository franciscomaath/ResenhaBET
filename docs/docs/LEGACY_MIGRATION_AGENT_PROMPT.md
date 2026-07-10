# Agent Task: ResenhaBET Legacy Data Migration Script

You are generating a single self-contained Python script (`migrate_legacy.py`) that
reads two legacy JSON files and writes a valid PostgreSQL migration SQL file to stdout.

Read this prompt fully before touching any file.

---

## Step 1 — Read the codebase to learn the current schema

**Do not rely on any description in this prompt for schema details.**
The schema has evolved and the authoritative sources are:

1. **Flyway migrations** — read ALL files matching `src/main/resources/db/migration/V*.sql`,
   sorted by version. The highest-numbered ones reflect the latest state.
2. **JPA entities** — read all `.java` files under `src/main/java/**/entity/`.
3. **Enums** — read all `.java` files under `src/main/java/**/enums/`.
4. **Service layer (for business rules only, not schema)** — read
   `TournamentServiceImpl.java`, `BetServiceImpl.java`, `WalletServiceImpl.java`.

From these files extract:
- Every table name and its exact column names, types, nullability, defaults.
- Every sequence name used for ID generation (e.g. `player_id_seq`).
- Every FK relationship.
- Every `CHECK` constraint or enum column (so you insert only valid enum literals).
- The schema prefix (it is `resenha`).

---

## Step 2 — Understand the legacy data

There are two JSON input files. Their structures are described below
**exactly** — do not re-read them from disk to inspect structure; just use the spec.

### File A: `resenha_bet_backup.json`

```
{
  "players": [                    // 18 rows
    {
      "name": "Alexandre",        // string, unique player name (legacy primary key)
      "elo": 991.23,              // current Elo rating after all matches
      "wins": 2, "losses": 2, "draws": 0,
      "goalsFor": 4, "goalsAgainst": 10
    }, ...
  ],
  "matches": [                    // 47 rows
    {
      "id": "match_1752984403530",   // legacy string ID; extract timestamp = int(id.split('_')[1])
      "tournamentId": "tourn_...",   // references tournaments[].id
      "player1": "Tadeu",            // home player name
      "player2": "Kadu",             // away player name
      "score1": 3, "score2": 2,      // final scores
      "winner": "Tadeu",             // winning player name (never null — draws don't occur in backup)
      "onPenalties": false,          // boolean
      "phase": "Final",              // round name (must match tournaments[].phases[].name)
      "timestamp": 1752984403530,    // epoch ms
      "eloChange": 17.31,            // absolute Elo delta (always positive)
      "multiplierApplied": 1,        // combined multiplier used
      ...                            // other elo sub-fields — ignore
    }, ...
  ],
  "tournaments": [                // 4 rows
    {
      "id": "tourn_1752540165693",
      "name": "Copa XerecaBet do Bostil 2024",
      "createdAt": 1752540165693,   // epoch ms
      "champion": "Francisco",      // winner player name (may be null if tournament unfinished)
      "participants": [],           // always empty in this export — ignore
      "phases": [
        { "name": "Quartas de Final", "multiplier": 1 },
        { "name": "Semifinal",        "multiplier": 1.2 },
        { "name": "Final",            "multiplier": 1.4 },
        { "name": "Disputa de Terceiro Lugar", "multiplier": 1.2 }
      ]
    }, ...
  ]
}
```

### File B: `resenhabet_data_MAIS_RECENTE.json`

```
{
  "bettors": {                     // 18 keys, one per player name
    "Alexandre": { "name": "Alexandre", "wallet": 40.0, "hasBetted": false },
    ...
  },
  "betHistory": [                  // 15 rows (ALL isKnockout=true)
    {
      "match": {
        "id": "match_1752973848835",  // legacy ID; NO tournamentId field
        "p1": "Lucas",               // home player name
        "p2": "Fitaroni",            // away player name
        "isKnockout": true,
        "score1": 2, "score2": 1,
        "penaltyScore1": 2, "penaltyScore2": 1,  // only meaningful if onPenalties=true
        "odds": { "p1": "2.48", "p2": "1.68", "draw": null },  // draw=null for knockout
        "bettingOpen": false
      },
      "bets": [
        {
          "bettor": "Nicolas",      // user name
          "on": "Lucas",            // player name they bet on (= p1 or p2)
          "amount": 20.0,           // stake
          "outcome": "win",         // "win" | "loss"
          "payout": 49.6            // actual payout (0 if loss)
        }, ...
      ],
      "winner": "Lucas"             // winning player name
    }, ...
  ],
  "guestBettors": []               // always empty — ignore
}
```

---

## Step 3 — Mapping rules

Apply these mappings in the Python script. Adjust column names to match what
you found in Step 1 (these are **logical** names, not necessarily exact column names).

### 3.1 — ID strategy
All new-schema entities use auto-increment sequences (BigSerial / `nextval`).
Generate IDs by maintaining counters in Python starting from 1 per table,
and emit `ALTER SEQUENCE … RESTART WITH …` at the end of the SQL so
the DB sequences won't collide with inserted rows.

### 3.2 — Player → `player` + `user` + `wallet`

For each of the 18 entries in `players[]`:

- Insert one `player` row:
  - `name` = player name
  - `active` = true
  - `current_elo` = `players[].elo` (DECIMAL, keep full precision)
  - `user` FK = the user created below (nullable — set after user insert)

- Insert one `user` row (same person):
  - `name` = player name
  - `pin_hash` = NULL
  - `user_type` = `'USER'` (only the first user alphabetically named "Francisco"
    should be `'ADMIN'` — this is the group admin)
  - `first_login` = true

- Insert one `wallet` row:
  - `user_id` = the user above
  - `balance` = `bettors[player_name].wallet` from File B (DECIMAL, keep precision;
    default 0.0 if player name not found in bettors map)

After inserting, UPDATE `player.user_id` to point at the user row.
(Or insert with the FK directly if you emit users before players.)

### 3.3 — Tournament → `tournament`

For each of the 4 entries in `tournaments[]`:

- `name` = `tournaments[].name`
- `type` = `'FIFA_MATCH'`
- `status` = `'ENDED'`
- `generation_mode` = `'MANUAL'`
- `format`: infer from phase names:
  - If any phase name contains "Liga" → `'LEAGUE_BRACKET'`
  - Else → `'BRACKET'`
- `created_at` = `to_timestamp(tournaments[].createdAt / 1000.0)`
- `has_third_place_match` = true if any phase name contains "Terceiro"
- `number_of_groups` = NULL, `players_advancing_per_group` = NULL
- `competition_id` = NULL

Keep a Python dict `tourn_legacy_id_to_new_id` mapping `"tourn_..."` strings
to new integer IDs.

### 3.4 — TournamentRound → `tournament_round`

For each tournament, iterate its `phases[]` in order (index = round_order):

- `tournament_id` = new tournament ID
- `name` = `phases[].name`
- `multiplier` = `phases[].multiplier`
- `round_order` = phase index (0-based)
- `phase_type`:
  - `'GROUP_STAGE'` if name contains "Liga"
  - `'KNOCKOUT'` otherwise
- `group_number` = NULL

Keep a dict `(legacy_tourn_id, phase_name) → round_id` for event mapping.

### 3.5 — Event (from File A `matches[]`) → `event`

For each of the 47 matches in File A:

- `tournament_id` = resolved from `match.tournamentId`
- `round_id` = resolved from `(match.tournamentId, match.phase)`
- `player_home_id` = player ID for `match.player1`
- `player_away_id` = player ID for `match.player2`
- `home_score` = `match.score1`
- `away_score` = `match.score2`
- `status` = `'COMPLETED'`
- `is_knockout` = true if round `phase_type == 'KNOCKOUT'`; false otherwise
- `is_bye` = false
- `is_third_place_match` = true if `match.phase` contains "Terceiro"
- `game_datetime` = `to_timestamp(match.timestamp / 1000.0)`
- `home_elo_before` = NULL (not in legacy data)
- `away_elo_before` = NULL
- `penalties_home` = NULL, `penalties_away` = NULL (not stored in backup —
  `match.onPenalties` is true for some, but the individual penalty scores
  are not in File A; leave NULL)
- `team_home_id` = NULL, `team_away_id` = NULL (FIFA_MATCH)
- `external_match_id` = NULL
- `next_round_event` = NULL (bracket linkage not in legacy data)

Keep a dict `legacy_match_id → new_event_id` for bet slip mapping.

**Winner determination** (for Market/Outcome lookup, see 3.7):
- `match.winner == match.player1` → home wins
- `match.winner == match.player2` → away wins
- No draws exist in File A

### 3.6 — Linking betHistory matches to backup events

The betHistory entries have no `tournamentId`. Match them to already-created
events (from section 3.5) using a two-step lookup:

**Step 1 — player-pair key (order-independent):**
```python
key = frozenset([bh_match["p1"], bh_match["p2"]])
candidates = [e for e in backup_events if frozenset([e.p1, e.p2]) == key]
```

**Step 2 — score tiebreak (only needed for 3 ambiguous pairs):**
```python
if len(candidates) > 1:
    bh_scores = {(bh_match["score1"], bh_match["score2"]),
                 (bh_match["score2"], bh_match["score1"])}
    candidates = [e for e in candidates
                  if (e.score1, e.score2) in bh_scores]
```

The combination of player-pair + score uniquely resolves all 15 betHistory
entries — verified: 3 pairs are ambiguous by players alone but all 3 are
disambiguated by score.

If after both steps `len(candidates) != 1`:
- Print a WARNING to stderr with the match details.
- **Skip that betHistory entry entirely** — do not create any bet_slip,
  bet_slip_item, or transaction rows for it.

Keep `bethistory_match_id → resolved_event_id` dict using the matched event.

### 3.7 — Market + Outcome → `market` + `outcome`

For every event created (both File A and betHistory):

- Create one `market` row:
  - `event_id` = the event
  - `market_type` = `'MATCH_RESULT'`
  - `status` = `'CLOSED'`

- Create `outcome` rows:
  - For File A events (no draw possible in legacy data, but the schema still
    expects all 3 for non-knockout, 2 for knockout):
    - If `is_knockout = false`: create 3 outcomes ("Vitória Casa", "Empate", "Vitória Fora")
    - If `is_knockout = true`: create 2 outcomes ("Vitória Casa", "Vitória Fora")
  - Set `odd` = 1.05 (minimum guard) for all — legacy odds are not stored in File A
  - For betHistory events: use `match.odds.p1` for "Vitória Casa",
    `match.odds.p2` for "Vitória Fora" (draw is null / knockout)

Keep a dict `(event_id, label) → outcome_id` for bet slip item mapping.

### 3.8 — BetSlip + BetSlipItem + Transaction

Process File B `betHistory[]` only (File A matches have no betting data).

For each `betHistory[i]`:

  For each `bets[j]` in that entry:

  - Create one `bet_slip` row:
    - `user_id` = user ID for `bets[j].bettor`
    - `tournament_id` = the `tournament_id` of the resolved event (from the backup)
    - `stake` = `bets[j].amount`
    - `status` = `'WON'` if `bets[j].outcome == "win"` else `'LOST'`
    - `potential_return` = `bets[j].payout` if win; else `bets[j].amount * float(relevant_odd)` approximately — use `bets[j].amount * float(odd)` where odd is from `match.odds.p1` or `match.odds.p2` depending on whom they bet on
    - `combined_odd` = the odd for the outcome they bet on
    - `created_at` = `to_timestamp(int(match.id.split('_')[1]) / 1000.0)`

  - Create one `bet_slip_item` row:
    - `bet_slip_id` = the slip above
    - `event_id` = event ID for this betHistory match
    - `outcome_id` = outcome ID for the bet direction:
      - `bets[j].on == match.p1` → "Vitória Casa" outcome
      - `bets[j].on == match.p2` → "Vitória Fora" outcome
    - `odd_snapshot` = the odd for that outcome (from match.odds)
    - `status` = same as bet_slip.status

  - Create one `transaction` row (BET_PLACED, always):
    - `user_id` = bettor user ID
    - `amount` = `-bets[j].amount` (debit — money left wallet)
    - `type` = `'BET_PLACED'`
    - `bet_slip_id` = the slip above
    - `created_at` = same as bet_slip

  - If `bets[j].outcome == "win"` AND `bets[j].payout > 0`, also create a
    `transaction` row (BET_WON):
    - `amount` = `+bets[j].payout`
    - `type` = `'BET_WON'`
    - `bet_slip_id` = the slip above
    - `created_at` = same as bet_slip

---

## Step 4 — Python script requirements

### Invocation
```
python3 migrate_legacy.py \
  --backup    resenha_bet_backup.json \
  --recent    resenhabet_data_MAIS_RECENTE.json \
  --output    V99__LEGACY_DATA_MIGRATION.sql
```

### Script structure

```python
#!/usr/bin/env python3
"""
ResenhaBET v1 → v2 legacy data migration generator.
Reads two legacy JSON files, writes a single idempotent SQL file.
"""
import argparse, json, sys
from datetime import datetime, timezone
from decimal import Decimal

# --- helpers ---
def ts_to_sql(epoch_ms: int) -> str:
    """Convert epoch-ms to PostgreSQL timestamp literal."""
    ...

def esc(s) -> str:
    """Escape single quotes in strings for SQL."""
    ...

class IdSequence:
    """Thread-safe counter for synthetic PK generation."""
    ...

# --- loaders ---
def load_backup(path) -> dict: ...
def load_recent(path) -> dict: ...

# --- builders (one function per table group) ---
def build_players_users_wallets(...) -> list[str]: ...
def build_tournaments_rounds(...) -> list[str]: ...
def build_events_from_backup(...) -> list[str]: ...
def build_markets_outcomes(...) -> list[str]: ...
def build_bet_slips_items_transactions(...) -> list[str]: ...
def build_sequence_resets(...) -> list[str]: ...

# --- main ---
def main():
    parser = argparse.ArgumentParser(...)
    ...
    lines = []
    lines += ["BEGIN;", ""]
    lines += ["-- Schema guard", f"SET search_path TO resenha, public;", ""]
    lines += build_players_users_wallets(...)
    lines += build_tournaments_rounds(...)
    lines += build_events_from_backup(...)
    lines += build_markets_outcomes(...)
    lines += build_bet_slips_items_transactions(...)
    lines += build_sequence_resets(...)
    lines += ["", "COMMIT;"]
    ...

if __name__ == "__main__":
    main()
```

### SQL output requirements

- Wrap everything in `BEGIN; … COMMIT;`.
- Set `search_path TO resenha, public` at the top.
- Use `INSERT INTO resenha.<table> (...) VALUES (...)` — one row per statement
  (not multi-row batching) for readability and debuggability.
- All string values: escape single quotes (`''`).
- Timestamps: use `to_timestamp(<float_seconds>)` or a literal `TIMESTAMP 'YYYY-MM-DD HH:MM:SS'`.
- NULL values: emit `NULL` (not `'NULL'`).
- Decimal/float values: emit with full Python `repr()` precision — no rounding.
- Sequence resets at end: for every sequence used, emit:
  ```sql
  SELECT setval('resenha.<table>_id_seq', <max_id_used>, true);
  ```
- Add a header comment block documenting row counts by table.
- Add inline `-- section` comments separating each table group.

### Error handling in the script

- If a player name in `matches[]` is not found in `players[]` → print a
  WARNING to stderr and skip that match (do not crash).
- If a `(tournamentId, phase)` combination has no matching round → WARNING + skip.
- If a bettor name in `betHistory` is not found in the player/user map →
  WARNING + skip that bet.
- At the end, print a summary to stderr: rows inserted per table.

---

## Step 5 — Deliver

Output exactly two artifacts:

1. **`migrate_legacy.py`** — the complete, runnable Python script.
2. **A short validation checklist** (`MIGRATION_VALIDATION.md`) listing the
   exact SQL queries to run after applying the migration to verify correctness,
   e.g.:
   - Row counts per table vs. expected
   - Wallet balance spot-checks (3 specific players with known values)
   - FK integrity checks
   - Sequence sanity checks

Do not output explanations, plans, or intermediate thoughts — only the two files.
