# ResenhaBET v2 — Odds Calculation Specification

## Overview

Odds are calculated once at event creation and never change after that (static pre-match odds). Dynamic odds will be implemented in a later version.
The calculation uses two inputs: the current Elo rating of each player and the head-to-head
historical record between them specifically. No house margin is applied.

The `OddsCalculator` is a pure service — no repository dependencies, no side effects.
It receives all data ready and returns the three decimal odds.

---

## Entities and fields involved

**`Player`**
```
current_elo: DECIMAL(10,2) — stored on the entity, updated after every COMPLETED event
```

**`Event`** (at creation, before persisting)
```
player_home_id
home_elo_before
player_away_id
away_elo_before
```

**`H2HRecord`** (not persisted — assembled by EventService at creation time)
```
homeWins:     int — number of times home player won against away player
awayWins:     int — number of times away player won against home player
draws:        int — number of draws between the two
totalMatches: int — homeWins + awayWins + draws
```

**`OddsResult`** (not persisted — returned by OddsCalculator, used to create Outcomes)
```
homeOdd:  BigDecimal — decimal odd for home win  (e.g. 1.85)
drawOdd:  BigDecimal — decimal odd for draw      (e.g. 3.20)
awayOdd:  BigDecimal — decimal odd for away win  (e.g. 4.10)
```

---

## Step 1 — Base probabilities from Elo

Use the standard Elo expected score formula to get the base win probability for each player.

```
prob_home_base = 1 / (1 + 10 ^ ((elo_away - elo_home) / 400))
prob_away_base = 1 - prob_home_base
```

The constant 400 is the standard Elo scale parameter. It means a 400-point difference
implies ~91% win probability for the higher-rated player.

**Examples:**
- Equal Elos (1000 vs 1000): prob_home = 0.500, prob_away = 0.500
- 100-point gap (1100 vs 1000): prob_home = 0.640, prob_away = 0.360
- 200-point gap (1200 vs 1000): prob_home = 0.760, prob_away = 0.240
- 400-point gap (1400 vs 1000): prob_home = 0.909, prob_away = 0.091

---

## Step 2 — Introduce draw probability

The base Elo formula only produces two outcomes. Draw probability is derived by
redistributing from both sides proportionally to how balanced the match is.

The more balanced the match, the higher the draw probability. The more one-sided,
the lower the draw probability.

```
draw_factor    = 0.28  (configurable via application.properties: resenhabet.odds.draw-factor)
prob_draw      = draw_factor × (1 - |prob_home_base - prob_away_base|)
prob_home_elo  = prob_home_base - (prob_draw / 2)
prob_away_elo  = prob_away_base - (prob_draw / 2)
```

At this point: prob_home_elo + prob_draw + prob_away_elo = 1.0

**Why 0.28 as the default draw_factor:**
With equal Elos, this produces a draw probability of 28%, which is close to the
historical draw rate in competitive FIFA among players of similar skill.
Tune this value against your group's actual historical draw rate from v1 data.

**Examples with draw_factor = 0.28:**

Equal Elos (1000 vs 1000):
- prob_draw     = 0.28 × (1 - 0.00) = 0.280
- prob_home_elo = 0.500 - 0.140     = 0.360
- prob_away_elo = 0.500 - 0.140     = 0.360

100-point gap (1100 vs 1000):
- prob_draw     = 0.28 × (1 - 0.28) = 0.202
- prob_home_elo = 0.640 - 0.101     = 0.539
- prob_away_elo = 0.360 - 0.101     = 0.259

200-point gap (1200 vs 1000):
- prob_draw     = 0.28 × (1 - 0.52) = 0.134
- prob_home_elo = 0.760 - 0.067     = 0.693
- prob_away_elo = 0.240 - 0.067     = 0.173

---

## Step 3 — Head-to-head modifier

The H2H modifier adjusts the Elo-based probabilities to reflect the specific
historical record between these two players. It is applied as a weighted blend.

### 3a — Calculate H2H weight based on sample size

The modifier weight scales with the number of direct confrontations to avoid
distorting odds from a small sample.

```
MAX_H2H_WEIGHT = 0.20  (configurable: resenhabet.odds.max-h2h-weight)
h2h_weight     = min(totalMatches / 10.0, MAX_H2H_WEIGHT)
elo_weight     = 1.0 - h2h_weight
```

- 0 direct matches:   h2h_weight = 0.00 — Elo is the only signal
- 3 direct matches:   h2h_weight = 0.06 — minor H2H influence
- 5 direct matches:   h2h_weight = 0.10 — moderate H2H influence
- 10+ direct matches: h2h_weight = 0.20 — maximum H2H influence (20%)

### 3b — Calculate raw H2H probabilities

```
prob_home_h2h = homeWins  / totalMatches
prob_draw_h2h = draws     / totalMatches
prob_away_h2h = awayWins  / totalMatches
```

If totalMatches = 0, skip step 3 entirely and use Elo probabilities as final.

### 3c — Blend Elo and H2H probabilities

```
prob_home_final = (prob_home_elo × elo_weight) + (prob_home_h2h × h2h_weight)
prob_draw_final = (prob_draw     × elo_weight) + (prob_draw_h2h × h2h_weight)
prob_away_final = (prob_away_elo × elo_weight) + (prob_away_h2h × h2h_weight)
```

After blending, renormalize to ensure the three probabilities still sum to exactly 1.0:

```
total           = prob_home_final + prob_draw_final + prob_away_final
prob_home_final = prob_home_final / total
prob_draw_final = prob_draw_final / total
prob_away_final = prob_away_final / total
```

The renormalization is a safeguard against floating point drift. In theory the sum
is already 1.0, but rounding in BigDecimal operations can introduce small errors.

---

## Step 4 — Convert probabilities to decimal odds

```
home_odd = 1 / prob_home_final
draw_odd = 1 / prob_draw_final
away_odd = 1 / prob_away_final
```

Round all three to 2 decimal places.

**Example output for equal Elos, no H2H history:**
- home_odd = 1 / 0.360 = 2.78
- draw_odd = 1 / 0.280 = 3.57
- away_odd = 1 / 0.360 = 2.78

**Example output for 100-point gap, 6 H2H matches (home won 4, drew 1, lost 1):**

H2H raw:
- prob_home_h2h = 4/6 = 0.667
- prob_draw_h2h = 1/6 = 0.167
- prob_away_h2h = 1/6 = 0.167

Weights: h2h_weight = min(6/10, 0.20) = 0.12, elo_weight = 0.88

Blend:
- prob_home_final = (0.539 × 0.88) + (0.667 × 0.12) = 0.474 + 0.080 = 0.554
- prob_draw_final = (0.202 × 0.88) + (0.167 × 0.12) = 0.178 + 0.020 = 0.198
- prob_away_final = (0.259 × 0.88) + (0.167 × 0.12) = 0.228 + 0.020 = 0.248

Odds:
- home_odd = 1 / 0.554 = 1.81
- draw_odd = 1 / 0.198 = 5.05
- away_odd = 1 / 0.248 = 4.03

---

## Step 5 — Minimum odd guard

Prevent extreme Elo gaps from producing unrealistically low odds (below 1.01),
which would imply near-certainty and make betting pointless.

```
MIN_ODD = 1.05  (configurable: resenhabet.odds.min-odd)

home_odd = max(home_odd, MIN_ODD)
draw_odd = max(draw_odd, MIN_ODD)
away_odd = max(away_odd, MIN_ODD)
```

---

## Where each component lives in the codebase

**`OddsCalculator`** (pure service, no repositories)
- Receives: `eloHome`, `eloAway`, `H2HRecord`
- Executes steps 1 through 5
- Returns: `OddsResult`

**`EventService.create()`** (orchestrator)
- Reads `playerHome.currentElo` and `playerAway.currentElo` directly from the Player entity
- Queries `EventRepository` for direct confrontation history between the two players
- Assembles `H2HRecord`
- Calls `OddsCalculator.calculate(eloHome, eloAway, h2hRecord)`
- Creates `Market` with `status = OPEN`
- Creates three `Outcome` entities using the returned `OddsResult`

**`EloService`** (called only from `EventService.finishEvent()`)
- Updates `player.currentElo` for both players after a COMPLETED event
- Uses standard Elo update formula with `tournament_round.multiplier` as K-factor scale
- Saves updated players

**`EventRepository`** (new query method needed)
- `findDirectConfrontations(Long player1Id, Long player2Id, int limit)`
- Returns the last N COMPLETED events between these two players regardless of home/away side

---

## Elo update formula (for EloService reference)

After each COMPLETED event, update both players' Elo:

```
K          = 32 × round.multiplier   (e.g. 32 for regular round, 38.4 for 1.2x multiplier)
expected   = 1 / (1 + 10 ^ ((elo_opponent - elo_player) / 400))
actual     = 1.0 (win), 0.5 (draw), 0.0 (loss)
new_elo    = current_elo + K × (actual - expected)
```

The `round.multiplier` scales the K-factor, meaning high-stakes rounds (finals, semis)
produce larger Elo swings than regular rounds.

---

## Configuration properties

All tunable parameters go in `application.properties` so each self-hosted instance
can calibrate to their group's historical data:

```properties
resenhabet.odds.draw-factor=0.28
resenhabet.odds.max-h2h-weight=0.20
resenhabet.odds.min-odd=1.05
resenhabet.odds.h2h-match-limit=10
```

---

## Unit test cases for OddsCalculator

The following scenarios must be covered:

1. **Equal Elos, no H2H** — home and away odds are identical, draw odd is the lowest of the three
2. **Equal Elos, H2H heavily favoring home** — home odd decreases, away odd increases relative to no-H2H baseline
3. **Large Elo gap, no H2H** — underdog odd is high, favorite odd approaches MIN_ODD
4. **Large Elo gap with H2H favoring underdog** — underdog odd decreases relative to pure-Elo result
5. **H2H with 0 matches** — result is identical to no-H2H calculation (weight = 0)
6. **H2H with 10+ matches** — h2h_weight is capped at MAX_H2H_WEIGHT, does not exceed it
7. **Probabilities sum to 1.0** after renormalization in all scenarios
8. **All odds >= MIN_ODD** in all scenarios including extreme Elo gaps
