# TournamentServiceImpl — Bug Report & Fix Tasks

> Scope: bugs identified in bracket generation, LEAGUE_BRACKET flow,
> and `advanceToBracket()`. All line references are from the uploaded file.

---

## BUG-01 — CRITICAL: `advanceToBracket()` assigns events to GROUP_STAGE rounds

**Location:** `advanceToBracket()` lines 440–446 and `generateBracketAutoEvents()` line 1040

**What happens:**
`tournament.getRounds()` at the time `advanceToBracket()` runs contains both
GROUP_STAGE rounds (created at `startTournament()`) and the KNOCKOUT rounds just
added in the same method. The `standardRounds` filter only excludes `"3rd Place"`,
so it includes GROUP_STAGE rounds. `createBracketEvents()` and
`generateBracketAutoEvents()` then assign knockout events to group stage rounds
instead of knockout rounds.

**Broken code:**
```java
// advanceToBracket() lines 440-446
List<TournamentRound> standardRounds = new ArrayList<>();
for (TournamentRound r : tournament.getRounds()) {
    if (!"3rd Place".equals(r.getName())) {
        standardRounds.add(r); // includes GROUP_STAGE rounds — wrong
    }
}

// generateBracketAutoEvents() line 1040
List<TournamentRound> standardRounds = tournament.getRounds().stream()
    .filter(r -> !"3rd Place".equals(r.getName()))
    .toList(); // same problem when called from advanceToBracket
```

**Fix — both locations:**
```java
List<TournamentRound> standardRounds = tournament.getRounds().stream()
    .filter(r -> r.getPhaseType() == PhaseType.KNOCKOUT
              && !"3rd Place".equals(r.getName()))
    .sorted(Comparator.comparingInt(TournamentRound::getRoundOrder))
    .toList();
```

---

## BUG-02 — CRITICAL: KNOCKOUT `roundOrder` collides with GROUP_STAGE `roundOrder`

**Location:** `advanceToBracket()` lines 403–422

**What happens:**
GROUP_STAGE rounds created at `startTournament()` have `roundOrder` values starting
from 1. The KNOCKOUT rounds added in `advanceToBracket()` also start from 1
(`round.setRoundOrder(i + 1)`). This produces duplicate `roundOrder` values within
the same tournament, breaking any global sort or display that relies on this field.

**Broken code:**
```java
for (int i = 0; i < numRounds; i++) {
    round.setRoundOrder(i + 1); // always starts from 1 — collides with group stage
    rounds.add(round);
}

// 3rd place also collides
thirdPlaceRound.setRoundOrder(numRounds); // same value as last standard round
rounds.get(numRounds).setRoundOrder(numRounds + 1); // ignores group stage offset
```

**Fix:**
Compute the current max `roundOrder` across all existing rounds and offset from it:
```java
int maxExistingOrder = tournament.getRounds().stream()
    .mapToInt(TournamentRound::getRoundOrder)
    .max()
    .orElse(0);

for (int i = 0; i < numRounds; i++) {
    round.setRoundOrder(maxExistingOrder + i + 1);
    rounds.add(round);
}

if (Boolean.TRUE.equals(tournament.getHasThirdPlaceMatch()) && numRounds >= 2) {
    thirdPlaceRound.setRoundOrder(maxExistingOrder + numRounds);
    rounds.add(numRounds - 1, thirdPlaceRound);
    // shift Final to last
    rounds.get(rounds.size() - 1).setRoundOrder(maxExistingOrder + numRounds + 1);
}
```

---

## BUG-03 — CRITICAL: `homeSourceEvent` and `awaySourceEvent` are never populated

**Location:** `generateBracketAutoEvents()` lines 1090–1100
and `createBracketEvents()` lines 877–887

**What happens:**
The two new fields (`homeSourceEvent`, `awaySourceEvent`) were added to the `Event`
entity but are never assigned during bracket generation. Only `nextRoundEvent` is set.
The frontend TBD slot label feature will not work — the fields will always be null.

**Broken code:**
```java
// only nextRoundEvent is set; source events on the destination are never assigned
for (int j = 0; j < currentRoundEvents.size(); j++) {
    Event currentEvent = currentRoundEvents.get(j);
    Event nextEvent = nextRoundEvents.get(j / 2);
    currentEvent.setNextRoundEvent(nextEvent);
    eventRepository.save(currentEvent);
    // homeSourceEvent and awaySourceEvent on nextEvent never set
}
```

**Fix — apply in both `generateBracketAutoEvents()` and `createBracketEvents()`:**
```java
for (int j = 0; j < currentRoundEvents.size(); j++) {
    Event currentEvent = currentRoundEvents.get(j);
    Event nextEvent = nextRoundEvents.get(j / 2);
    currentEvent.setNextRoundEvent(nextEvent);

    // populate source events on the destination slot
    if (j % 2 == 0) {
        nextEvent.setHomeSourceEvent(currentEvent);
    } else {
        nextEvent.setAwaySourceEvent(currentEvent);
    }

    eventRepository.save(currentEvent);
    eventRepository.save(nextEvent);
}
```

**Fix — 3rd place event (after creating it):**
The 3rd place event must have its source events set to the two semi-final events
so that the frontend can display the losers' names correctly.
`isThirdPlaceMatch = true` must also be set so the frontend knows to show the loser,
not the winner, from each source event.
```java
List<Event> semiFinals = eventsByRound.get(numRounds - 2);
if (semiFinals.size() >= 2) {
    thirdPlaceEvent.setHomeSourceEvent(semiFinals.get(0));
    thirdPlaceEvent.setAwaySourceEvent(semiFinals.get(1));
    thirdPlaceEvent.setThirdPlaceMatch(true);
    eventRepository.save(thirdPlaceEvent);
}
```

---

## BUG-04 — CRITICAL: odd-sized groups do not generate all matchups

**Location:** `generateLeagueBracketAutoEvents()` lines 1180–1231
and `generateLeagueAutoEvents()` lines 898–956

**What happens:**
The circle method round-robin algorithm only works correctly for even `n`.
For odd `n`, `n / 2` (integer division) rounds down, leaving one player without
an opponent in each round. The result is that not all pairs play each other.

Example with `groupSize = 3`:
- `n / 2 = 1` → 1 match per round, `n - 1 = 2` rounds → 2 total matches
- A complete single round-robin for 3 players needs 3 matches
- One pair (the middle two in the list) is never generated

The number of rounds created is also wrong: `(n-1)*2` rounds are created for the
group but only `(n-1)*1` pairs would be generated per half. For `n=3`, 4 rounds
are created but only 2 matches generated per half = 4 total instead of 6.

**Fix:**
When `n` is odd, add a `null` dummy player to make the list even, then skip any
match where either player is `null`. Also correct the round count to `n` (not `n-1`)
for the first half when `n` is odd.

Extract a shared helper used by both methods:

```java
private void generateRoundRobinForGroup(
        List<Player> groupPlayers,
        List<TournamentRound> rounds,
        Tournament tournament,
        boolean doubleRoundRobin,
        boolean isKnockout) {

    List<Player> playerList = new ArrayList<>(groupPlayers);
    boolean hasDummy = playerList.size() % 2 != 0;
    if (hasDummy) playerList.add(null); // dummy player for odd n

    int n = playerList.size(); // now always even
    int halfRounds = n - 1;   // correct for even n

    // First half
    for (int round = 0; round < halfRounds; round++) {
        TournamentRound currentRound = rounds.get(round);
        for (int i = 0; i < n / 2; i++) {
            Player home = playerList.get(i);
            Player away = playerList.get(n - 1 - i);
            if (home == null || away == null) continue; // skip bye slot
            createEventAndMarket(tournament, currentRound, home, away, isKnockout);
        }
        rotateCircle(playerList);
    }

    if (!doubleRoundRobin) return;

    // Second half: same pairings with home/away swapped
    // After halfRounds rotations the list is back to its original state
    for (int round = 0; round < halfRounds; round++) {
        TournamentRound currentRound = rounds.get(round + halfRounds);
        for (int i = 0; i < n / 2; i++) {
            Player home = playerList.get(n - 1 - i);
            Player away = playerList.get(i);
            if (home == null || away == null) continue;
            createEventAndMarket(tournament, currentRound, home, away, isKnockout);
        }
        rotateCircle(playerList);
    }
}

private void rotateCircle(List<Player> list) {
    Player last = list.get(list.size() - 1);
    for (int i = list.size() - 1; i > 1; i--) {
        list.set(i, list.get(i - 1));
    }
    list.set(1, last);
}
```

**Round count correction for odd n:**
In `startTournament()` LEAGUE_BRACKET section (line 229):
```java
// before fix:
int roundsPerGroup = (playersPerGroup - 1) * 2;

// after fix — account for odd group size:
int effectiveSize = playersPerGroup % 2 == 0 ? playersPerGroup : playersPerGroup + 1;
int roundsPerGroup = (effectiveSize - 1) * 2;
```

Apply the same correction in `generateLeagueAutoEvents()` for the LEAGUE format.

---

## BUG-05 — IMPORTANT: bye event score is wrong when the bye player is in the away slot

**Location:** `generateBracketAutoEvents()` lines 1107–1112

**What happens:**
The code always sets `homeScore = 1, awayScore = 0` for bye events regardless of
which slot the actual player occupies. If the bye player is in the `playerAway`
position (because `playerHome` is null), the event is stored with the wrong winner
from `determineWinner()`'s perspective: `homeScore > awayScore` returns `playerHome`
which is null.

**Broken code:**
```java
Player byeWinner = event.getPlayerHome() != null
    ? event.getPlayerHome() : event.getPlayerAway();
event.setStatus(EventStatus.COMPLETED);
event.setIsBye(true);
event.setHomeScore(1); // always 1-0 regardless of player position
event.setAwayScore(0);
```

**Fix:**
```java
Player byeWinner = event.getPlayerHome() != null
    ? event.getPlayerHome() : event.getPlayerAway();
event.setStatus(EventStatus.COMPLETED);
event.setIsBye(true);

if (event.getPlayerHome() != null) {
    event.setHomeScore(1);
    event.setAwayScore(0);
} else {
    event.setHomeScore(0);
    event.setAwayScore(1);
}
```

---

## BUG-06 — IMPORTANT: `autoCompleteGroupStageEvents()` silently corrupts Elo and stats

**Location:** `advanceToBracket()` lines 363–369 and
`autoCompleteGroupStageEvents()` lines 802–812

**What happens:**
If not all group stage events are completed when `advanceToBracket()` is called,
the system silently completes them as 0x0 draws. This corrupts:
- Player Elo ratings (which are updated on finishEvent)
- Player statistics (goals, wins, draws)
- Group standings (which determine who advances)

The task specification (B18) explicitly required throwing an exception instead.

**Broken code:**
```java
if (!allGroupCompleted) {
    autoCompleteGroupStageEvents(groupStageEvents); // silently corrupts data
}
```

**Fix:**
```java
if (!allGroupCompleted) {
    long pendingCount = groupStageEvents.stream()
        .filter(e -> e.getStatus() != EventStatus.COMPLETED)
        .count();
    throw new InvalidStateException(
        "Cannot advance to bracket: " + pendingCount +
        " group stage event(s) are not yet completed. " +
        "Please register all match results before advancing."
    );
}
```

Delete `autoCompleteGroupStageEvents()` entirely after this fix — it should not exist.

---

## BUG-07 — MINOR: round lists used by index are not sorted by `roundOrder`

**Location:** Multiple points throughout the file

**What happens:**
`tournament.getRounds()` and any stream result without an explicit sort may return
rounds in Hibernate's loading order (typically insertion order or ID order), which
may not match `roundOrder`. Any code that calls `.get(i)` on a filtered round list
assumes the list is ordered by `roundOrder`, but this is not guaranteed.

Affected locations:
- `generateBracketAutoEvents()` line 1040: `standardRounds.get(0)` used as round 1
- `generateLeagueBracketAutoEvents()` line 1173–1176: `roundsForGroup` filtered stream
- `createBracketEvents()` line 864: `rounds.get(i)` for each event
- `startTournament()` BRACKET MANUAL lines 300–305: `standardRounds` built from loop

**Fix — add sort to every stream that produces a round list used by index:**
```java
// pattern to apply everywhere:
.sorted(Comparator.comparingInt(TournamentRound::getRoundOrder))
.toList()
```

---

## BUG-08 — MINOR: `numRounds` and `nextPowerOf2` calculated twice in `startTournament()`

**Location:** `startTournament()` lines 246–250 and 288–292

**What happens:**
For `TournamentFormat.BRACKET`, both `nextPowerOf2` and `numRounds` are calculated
identically in two separate blocks — once to create the rounds and once to create
the events. If one is ever modified without updating the other, results diverge.

**Fix:**
Extract to local variables before the first block and reuse:
```java
// compute once, before the rounds block
int nextPowerOf2Bracket = 0;
int numRoundsBracket = 0;

if (tournament.getFormat() == TournamentFormat.BRACKET) {
    nextPowerOf2Bracket = computeNextPowerOf2(n);
    numRoundsBracket = (int) (Math.log(nextPowerOf2Bracket) / Math.log(2));
    // ... create rounds using these values
}

// ... save tournament ...

if (tournament.getFormat() == TournamentFormat.BRACKET) {
    // reuse the same values — no recalculation
    generateBracketAutoEvents(tournament, players, numRoundsBracket, nextPowerOf2Bracket);
}
```

Extract as a helper:
```java
private int computeNextPowerOf2(int n) {
    int result = Integer.highestOneBit(n - 1) << 1;
    return result < n ? Integer.highestOneBit(n) << 1 : result;
}
```

---

## Fix implementation order

Apply fixes in this order to avoid regressions:

1. **BUG-07** — sort all round lists. Low risk, no logic change, makes subsequent fixes easier to verify.
2. **BUG-08** — extract `nextPowerOf2` computation. Housekeeping before touching bracket logic.
3. **BUG-05** — bye event score. Isolated fix, no cascade.
4. **BUG-06** — replace `autoCompleteGroupStageEvents` with exception. Delete the method.
5. **BUG-04** — odd-n round-robin. Requires updating round count creation AND event generation. Test with groupSize=3 and groupSize=5.
6. **BUG-02** — roundOrder offset in `advanceToBracket`. Test with a LEAGUE_BRACKET tournament to verify no collisions.
7. **BUG-01** — standardRounds filter by KNOCKOUT phase. Depends on BUG-02 being fixed first so the rounds are correctly typed.
8. **BUG-03** — homeSourceEvent / awaySourceEvent assignment. Apply to both `generateBracketAutoEvents` and `createBracketEvents`. Include 3rd place event wiring.

---

## Test scenarios to verify all fixes

After applying all fixes, run the following scenarios:

| Scenario | Expected result |
|---|---|
| BRACKET AUTO, 4 players | 2 semis + 1 final, correct seeding, nextRoundEvent chained, homeSourceEvent/awaySourceEvent set |
| BRACKET AUTO, 5 players | 4 QF slots, 3 byes for top seeds, bye score matches player position |
| BRACKET AUTO, 4 players + 3rd place | Extra 3rd place event with homeSourceEvent=SF1, awaySourceEvent=SF2, isThirdPlaceMatch=true |
| LEAGUE AUTO, 3 players | 6 events total (3 pairs × 2 legs), all pairs covered |
| LEAGUE AUTO, 5 players | 20 events total (10 pairs × 2 legs), all pairs covered |
| LEAGUE_BRACKET, 6 players, 2 groups | Group rounds 1-4 for G1 (order 1-4), Group rounds 1-4 for G2 (order 5-8), KNOCKOUT rounds starting from order 9 |
| LEAGUE_BRACKET, 5 players, 1 group | Odd group: 10 events in group stage, all pairs covered |
| advanceToBracket with incomplete group | InvalidStateException thrown, no events auto-completed |
| advanceToBracket success | KNOCKOUT events assigned to KNOCKOUT rounds only, roundOrders do not collide with GROUP_STAGE |