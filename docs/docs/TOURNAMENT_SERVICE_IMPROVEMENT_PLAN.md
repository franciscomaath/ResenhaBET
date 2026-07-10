# Tournament Service Improvement Plan

**Target File:** `src/main/java/com/franciscomaath/resenhaapi/service/Impl/TournamentServiceImpl.java`  
**Related Files:** See "Files to Modify" section below  
**Created:** 2026-06-17  
**Purpose:** Production-grade code review findings for AI agent implementation

---

## Executive Summary

This document contains **31 issues** identified in a production code review of `TournamentServiceImpl.java` (1267 lines). The service handles tournament lifecycle management including CRUD operations, round/event generation, market/odds creation, scoreboard computation, and phase transitions.

**Critical Issues:** 4  
**High Severity:** 10  
**Medium Severity:** 12  
**Low Severity:** 5

**Estimated Effort:** 3-5 days for a senior developer

---

## Files to Modify

### Primary Target
- `src/main/java/com/franciscomaath/resenhaapi/service/Impl/TournamentServiceImpl.java`

### Secondary Targets
- `src/main/java/com/franciscomaath/resenhaapi/domain/entity/Tournament.java`
- `src/main/java/com/franciscomaath/resenhaapi/domain/entity/TournamentPlayer.java`
- `src/main/java/com/franciscomaath/resenhaapi/domain/entity/Event.java`
- `src/main/java/com/franciscomaath/resenhaapi/controller/TournamentController.java`

### New Files to Create
- `src/main/java/com/franciscomaath/resenhaapi/service/TournamentCrudService.java`
- `src/main/java/com/franciscomaath/resenhaapi/service/BracketGenerationService.java`
- `src/main/java/com/franciscomaath/resenhaapi/service/LeagueGenerationService.java`
- `src/main/java/com/franciscomaath/resenhaapi/service/ScoreboardService.java`
- `src/main/java/com/franciscomaath/resenhaapi/service/TournamentMarketService.java`
- `src/main/java/com/franciscomaath/resenhaapi/domain/valueobject/PlayerStats.java`
- Database migration: `src/main/resources/db/migration/V28__add_indexes_and_constraints.sql`

---

## Implementation Order

**Phase 1: Critical Fixes (Do First)**
1. Fix @Transactional import (BUG-1)
2. Add optimistic locking (CONC-1)
3. Fix authorization gaps (SEC-1, SEC-2, SEC-3)
4. Fix determineWinner logic (BUG-2, BUG-3)

**Phase 2: High Priority**
5. Fix NPE risks and edge cases (BUG-4, BUG-6, BUG-9)
6. Eliminate code duplication (QUAL-1, QUAL-2)
7. Fix N+1 queries (PERF-1, PERF-2, PERF-3)
8. Add logging (PROD-1)
9. Add database constraints (DB-1, CONC-2)

**Phase 3: Medium Priority**
10. Fix floating-point precision (BUG-5)
11. Fix business logic issues (BUG-7, DOMAIN-2, ERR-2, ERR-3)
12. Add pagination (PERF-4)
13. Optimize redundant queries (PERF-5, PERF-6)
14. Extract constants (QUAL-3)

**Phase 4: Refactoring**
15. Split into multiple services (SPRING-2, QUAL-4)
16. Extract domain logic (DOMAIN-1, DOMAIN-3)
17. Add read-only transactions (SPRING-3)

**Phase 5: Polish**
18. Remove commented code (QUAL-5)
19. Fix error messages (BUG-8)
20. Add secure random (PROD-3)

---

## Detailed Issues

### CRITICAL SEVERITY

#### BUG-1: Wrong @Transactional Import
**Location:** `TournamentServiceImpl.java:49`  
**Problem:** Uses `jakarta.transaction.Transactional` instead of Spring's `@Transactional`. Jakarta's version has different rollback semantics and may not roll back on `BusinessException`/`InvalidStateException`.  
**Impact:** Partial commits on failure, corrupt tournament state.

**Fix:**
```java
// REMOVE this import:
import jakarta.transaction.Transactional;

// ADD this import:
import org.springframework.transaction.annotation.Transactional;
```

**Verification:** Run existing tests. Create a test that throws `BusinessException` mid-transaction and verify rollback occurs.

---

#### CONC-1: No Optimistic Locking on Tournament Entity
**Location:** `Tournament.java` (entire file)  
**Problem:** No `@Version` field. Concurrent calls to `startTournament` or `advanceToBracket` can both pass status checks and create duplicate rounds/events.  
**Impact:** Duplicate data, corrupted brackets, financial impact via betting markets.

**Fix:**
```java
// Add to Tournament.java after the id field:
@Version
private Long version;
```

**Database Migration (V28):**
```sql
ALTER TABLE tournament ADD COLUMN version BIGINT DEFAULT 0;
```

**Verification:** Write a concurrent test that calls `startTournament` from 2 threads simultaneously. Verify one succeeds and one throws `OptimisticLockException`.

---

#### SEC-1: Missing Authorization on startTournament
**Location:** `TournamentServiceImpl.java:189` (startTournament method)  
**Problem:** No `currentUserContext.requireAdmin()` call. Any authenticated user can start tournaments.  
**Impact:** Privilege escalation, unauthorized tournament state changes, market creation.

**Fix:**
```java
@Override
@Transactional
public TournamentResponseDTO startTournament(Long tournamentId, StartTournamentRequestDTO dto) {
    currentUserContext.requireAdmin(); // ADD THIS LINE
    
    Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));
    // ... rest of method
}
```

**Verification:** Write integration test that calls `startTournament` as non-admin user, expect 401 Unauthorized.

---

#### BUG-2: determineWinner Silently Picks Winner on Tied Matches
**Location:** `TournamentServiceImpl.java:761-775` (determineWinner method)  
**Problem:** When `homeScore == awayScore` and no penalties, method returns `playerAway` arbitrarily. In knockout matches, this is wrong.  
**Impact:** Incorrect tournament advancement, corrupted brackets.

**Fix:**
```java
private Player determineWinner(Event event) {
    if (event.getPlayerHome() == null || event.getPlayerAway() == null) {
        return event.getPlayerHome() != null ? event.getPlayerHome() : event.getPlayerAway();
    }

    boolean usePenalties = event.getPenaltiesHome() != null && event.getPenaltiesAway() != null;
    if (usePenalties) {
        return event.getPenaltiesHome() > event.getPenaltiesAway()
                ? event.getPlayerHome() : event.getPlayerAway();
    }
    
    // ADD THIS CHECK:
    if (event.getHomeScore().equals(event.getAwayScore())) {
        throw new InvalidStateException(
            "Match " + event.getId() + " is tied (" + event.getHomeScore() + 
            "-" + event.getAwayScore() + ") with no penalties. Cannot determine winner.");
    }
    
    return event.getHomeScore() > event.getAwayScore()
            ? event.getPlayerHome() : event.getPlayerAway();
}
```

**Verification:** Write test with tied knockout match, expect `InvalidStateException`.

---

#### BUG-3: determineWinner NPE Risk When Both Players Null
**Location:** `TournamentServiceImpl.java:763`  
**Problem:** If both players are null, returns null. Callers then call `champion.getId()` → NPE.  
**Impact:** 500 error on scoreboard endpoint.

**Fix:**
```java
// In buildBracketPlacements method, around line 636:
if (finalEvent != null) {
    Player champion = determineWinner(finalEvent);
    if (champion != null) {  // ADD NULL CHECK
        Player runnerUp = determineLoser(finalEvent);
        placements.add(createPlacement(champion, 1, "Champion"));
        if (runnerUp != null) {  // ADD NULL CHECK
            placements.add(createPlacement(runnerUp, 2, "Runner-up"));
        }
    }
}

// Similar fix for third place match around line 650:
if (thirdPlaceEvent != null) {
    Player thirdPlace = determineWinner(thirdPlaceEvent);
    if (thirdPlace != null) {  // ADD NULL CHECK
        Player fourthPlace = determineLoser(thirdPlaceEvent);
        placements.add(createPlacement(thirdPlace, 3, "3rd Place"));
        if (fourthPlace != null) {  // ADD NULL CHECK
            placements.add(createPlacement(fourthPlace, 4, "4th Place"));
        }
    }
}
```

**Verification:** Write test with incomplete bracket (null players), verify scoreboard returns without NPE.

---

### HIGH SEVERITY

#### SEC-2: Missing Authorization on addPlayerToTournament
**Location:** `TournamentServiceImpl.java:134`  
**Problem:** No admin check. Any user can add players to tournaments.  
**Impact:** Unauthorized roster manipulation.

**Fix:**
```java
@Override
@Transactional
public TournamentPlayerResponseDTO addPlayerToTournament(Long tournamentId, TournamentPlayerRequestDTO dto) {
    currentUserContext.requireAdmin(); // ADD THIS LINE
    // ... rest of method
}
```

---

#### SEC-3: Missing Authorization on updateTournamentPlayerTeam
**Location:** `TournamentServiceImpl.java:161`  
**Problem:** No admin check. Any user can change team assignments.  
**Impact:** Unauthorized team modifications.

**Fix:**
```java
@Override
@Transactional
public TournamentPlayerResponseDTO updateTournamentPlayerTeam(Long tournamentId, Long playerId, PatchTournamentPlayerTeamRequestDTO dto) {
    currentUserContext.requireAdmin(); // ADD THIS LINE
    // ... rest of method
}
```

---

#### BUG-6: Commented-Out Group Filter in calculateGroupStandings
**Location:** `TournamentServiceImpl.java:812-817`  
**Problem:** When `tournament.getStatus() == CREATED`, the group filter is commented out, returning all players regardless of group.  
**Impact:** Incorrect group standings before tournament starts.

**Fix:**
```java
private List<Player> calculateGroupStandings(Tournament tournament, int groupNumber, List<Event> groupStageEvents) {
    if(tournament.getStatus() == TournamentStatus.CREATED){
        return tournament.getTournamentPlayers().stream()
                .filter(tp -> groupNumber == tp.getGroupNumber()) // UNCOMMENT THIS LINE
                .map(TournamentPlayer::getPlayer)
                .collect(Collectors.toList());
    }
    // ... rest of method
}
```

---

#### BUG-9: Off-by-One Error in createBracketEvents
**Location:** `TournamentServiceImpl.java:903`  
**Problem:** `round(rounds.get(i + 1))` skips first round and will throw `IndexOutOfBoundsException` on last iteration.  
**Impact:** Crash during MANUAL bracket creation.

**Fix:**
```java
private List<List<Event>> createBracketEvents(Tournament tournament, List<TournamentRound> rounds, int numRounds, int nextPowerOf2) {
    List<List<Event>> eventsByRound = new ArrayList<>();

    for (int i = 0; i < numRounds; i++) {
        int eventsInRound = nextPowerOf2 / (int) Math.pow(2, i + 1);
        List<Event> roundEvents = new ArrayList<>();
        for (int j = 0; j < eventsInRound; j++) {
            Event event = Event.builder()
                    .tournament(tournament)
                    .round(rounds.get(i))  // CHANGE from i+1 to i
                    .status(EventStatus.CREATED)
                    .homeScore(0)
                    .awayScore(0)
                    .isKnockout(true)
                    .build();
            event = eventRepository.save(event);
            roundEvents.add(event);
        }
        eventsByRound.add(roundEvents);
    }

    linkBracketEvents(eventsByRound, numRounds);
    return eventsByRound;
}
```

**Verification:** Test MANUAL bracket creation with 8 players, verify no IndexOutOfBoundsException.

---

#### PERF-1: N+1 Query in buildBracketPlacements
**Location:** `TournamentServiceImpl.java:680`  
**Problem:** `playerRepository.findById(entry.getKey())` called in loop for every eliminated player.  
**Impact:** 30+ extra queries for 32-player bracket.

**Fix:**
```java
private List<BracketPlacementDTO> buildBracketPlacements(Tournament tournament) {
    // ... existing code up to line 670 ...
    
    // REPLACE the loop at lines 679-684 with:
    List<Player> allPlayers = playerRepository.findAllById(playerEliminationRound.keySet());
    Map<Long, Player> playerMap = allPlayers.stream()
            .collect(Collectors.toMap(Player::getId, p -> p));
    
    for (Map.Entry<Long, String> entry : sortedEliminations) {
        Player player = playerMap.get(entry.getKey());
        if (player != null && placements.stream().noneMatch(p -> p.getPlayerId().equals(player.getId()))) {
            placements.add(createPlacement(player, position++, entry.getValue()));
        }
    }
    
    return placements;
}
```

---

#### PERF-2: N+1 Saves in linkBracketEvents
**Location:** `TournamentServiceImpl.java:1165-1184`  
**Problem:** Individual `save()` calls inside nested loops.  
**Impact:** Slow tournament start, DB connection pool exhaustion.

**Fix:**
```java
private void linkBracketEvents(List<List<Event>> eventsByRound, int numRounds) {
    List<Event> eventsToSave = new ArrayList<>();
    
    for (int i = 0; i < numRounds; i++) {
        List<Event> currentRoundEvents = eventsByRound.get(i);
        List<Event> nextRoundEvents = i + 1 < numRounds ? eventsByRound.get(i + 1) : null;
        
        for (int j = 0; j < currentRoundEvents.size(); j++) {
            Event currentEvent = currentRoundEvents.get(j);
            if (nextRoundEvents != null) {
                Event nextEvent = nextRoundEvents.get(j / 2);
                currentEvent.setNextRoundEvent(nextEvent);
                if (j % 2 == 0) {
                    nextEvent.setHomeSourceEvent(currentEvent);
                } else {
                    nextEvent.setAwaySourceEvent(currentEvent);
                }
                eventsToSave.add(nextEvent);
            }
            eventsToSave.add(currentEvent);
        }
    }
    
    eventRepository.saveAll(eventsToSave); // BATCH SAVE
}
```

---

#### PERF-3: N+1 Saves in generateLeagueBracketAutoEvents
**Location:** `TournamentServiceImpl.java:1129`  
**Problem:** `tournamentPlayerRepository.save(tp)` inside loop.  
**Impact:** Slow for large tournaments.

**Fix:**
```java
private void generateLeagueBracketAutoEvents(Tournament tournament, List<TournamentPlayer> players) {
    // ... existing code up to line 1122 ...
    
    List<TournamentPlayer> playersToSave = new ArrayList<>();
    
    for (int g = 0; g < numberOfGroups; g++) {
        int groupSize = playersPerGroup + (g < extraPlayers ? 1 : 0);
        List<Player> groupPlayers = new ArrayList<>();
        for (int i = 0; i < groupSize && playerIndex < shuffledPlayers.size(); i++) {
            TournamentPlayer tp = shuffledPlayers.get(playerIndex++);
            tp.setGroupNumber(g + 1);
            playersToSave.add(tp); // COLLECT INSTEAD OF SAVE
            groupPlayers.add(tp.getPlayer());
        }
        groups.add(groupPlayers);
    }
    
    tournamentPlayerRepository.saveAll(playersToSave); // BATCH SAVE
    
    // ... rest of method
}
```

---

#### QUAL-1: Massive Code Duplication Between advanceToBracket and forceAdvanceToBracket
**Location:** `TournamentServiceImpl.java:318-427` vs `431-549`  
**Problem:** ~100 lines of identical bracket-creation logic.  
**Impact:** Bug fixes must be applied twice, high regression risk.

**Fix:**
```java
// Extract shared method:
private TournamentResponseDTO performBracketAdvancement(
        Tournament tournament, 
        List<Player> advancingPlayers,
        int maxExistingOrder) {
    
    int n = advancingPlayers.size();
    int nextPowerOf2 = computeNextPowerOf2(n);
    int numRounds = Integer.numberOfTrailingZeros(nextPowerOf2);

    List<TournamentRound> rounds = new ArrayList<>(tournament.getRounds());
    String[] roundNames = getBracketRoundNames(numRounds);
    BigDecimal[] multipliers = getBracketMultipliers(numRounds);

    for (int i = 0; i < numRounds; i++) {
        TournamentRound round = new TournamentRound();
        round.setName(roundNames[i]);
        round.setMultiplier(multipliers[i]);
        round.setRoundOrder(maxExistingOrder + i + 1);
        round.setPhaseType(PhaseType.KNOCKOUT);
        round.setTournament(tournament);
        rounds.add(round);
    }

    if (Boolean.TRUE.equals(tournament.getHasThirdPlaceMatch()) && numRounds >= 2) {
        TournamentRound thirdPlaceRound = new TournamentRound();
        thirdPlaceRound.setName("3rd Place");
        thirdPlaceRound.setMultiplier(BigDecimal.valueOf(1.4));
        thirdPlaceRound.setRoundOrder(maxExistingOrder + numRounds);
        thirdPlaceRound.setPhaseType(PhaseType.KNOCKOUT);
        thirdPlaceRound.setTournament(tournament);
        rounds.add(rounds.size() - 1, thirdPlaceRound);
        rounds.get(rounds.size() - 1).setRoundOrder(maxExistingOrder + numRounds + 1);
    }

    tournament.setRounds(rounds);
    tournament = tournamentRepository.save(tournament);

    final Tournament finalTournament = tournament;
    List<List<Event>> eventsByRound;
    if (tournament.getGenerationMode() == GenerationMode.AUTO) {
        eventsByRound = generateBracketAutoEvents(tournament,
            advancingPlayers.stream().map(p -> {
                TournamentPlayer tp = new TournamentPlayer();
                tp.setPlayer(p);
                tp.setTournament(finalTournament);
                return tp;
            }).toList(),
            numRounds, nextPowerOf2);
    } else {
        eventsByRound = createBracketEvents(tournament, getStandardBracketRounds(tournament), numRounds, nextPowerOf2);
    }

    createThirdPlaceEvent(tournament, eventsByRound, numRounds);

    return tournamentMapper.toResponse(tournament);
}

// Then refactor advanceToBracket:
@Override
@Transactional
public TournamentResponseDTO advanceToBracket(Long tournamentId) {
    currentUserContext.requireAdmin();
    Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));

    if (tournament.getFormat() != TournamentFormat.LEAGUE_BRACKET) {
        throw new BusinessException("Tournament is not LEAGUE_BRACKET format.");
    }
    if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
        throw new BusinessException("Tournament is not in progress.");
    }

    List<Event> allEvents = eventRepository.findAllByTournamentId(tournamentId);
    List<Event> groupStageEvents = allEvents.stream()
            .filter(e -> e.getRound().getPhaseType() == PhaseType.GROUP_STAGE)
            .toList();

    boolean allGroupCompleted = groupStageEvents.stream()
            .allMatch(e -> e.getStatus() == EventStatus.COMPLETED || e.getStatus() == EventStatus.CANCELLED);

    if (!allGroupCompleted) {
        long pendingCount = groupStageEvents.stream()
                .filter(e -> e.getStatus() != EventStatus.COMPLETED)
                .count();
        throw new InvalidStateException(
                "Cannot advance to bracket: " + pendingCount +
                " group stage event(s) are not yet completed.");
    }

    boolean hasKnockoutRounds = tournament.getRounds().stream()
            .anyMatch(r -> r.getPhaseType() == PhaseType.KNOCKOUT);
    if (hasKnockoutRounds) {
        throw new BusinessException("Knockout phase already exists.");
    }

    int numberOfGroups = tournament.getNumberOfGroups();
    int playersAdvancingPerGroup = tournament.getPlayersAdvancingPerGroup();
    List<Player> advancingPlayers = new ArrayList<>();

    for (int g = 1; g <= numberOfGroups; g++) {
        List<Player> groupStandings = calculateGroupStandings(tournament, g, groupStageEvents);
        int advancingCount = Math.min(playersAdvancingPerGroup, groupStandings.size());
        for (int i = 0; i < advancingCount; i++) {
            advancingPlayers.add(groupStandings.get(i));
        }
    }

    int maxExistingOrder = tournament.getRounds().stream()
            .mapToInt(TournamentRound::getRoundOrder)
            .max()
            .orElse(0);

    return performBracketAdvancement(tournament, advancingPlayers, maxExistingOrder);
}

// Similar refactor for forceAdvanceToBracket
```

---

#### QUAL-2: Duplicated Sorting Logic for Standings
**Location:** `buildLeagueScoreboard()` lines 587-595 vs `calculateGroupStandings()` lines 835-848  
**Problem:** Same comparator written twice with slightly different structure.  
**Impact:** Inconsistent tie-breaking if one is changed but not the other.

**Fix:**
```java
// Extract shared comparator:
private static final Comparator<PlayerStatsResponseDTO> STANDINGS_COMPARATOR = 
    Comparator.comparingInt(PlayerStatsResponseDTO::getPoints).reversed()
        .thenComparing(Comparator.comparingInt(PlayerStatsResponseDTO::getGoalDifference).reversed())
        .thenComparing(Comparator.comparingInt(PlayerStatsResponseDTO::getGoalsScored).reversed())
        .thenComparing(Comparator.comparing(PlayerStatsResponseDTO::getCurrentElo).reversed());

// Use in buildLeagueScoreboard:
entries.sort(STANDINGS_COMPARATOR);

// Use in calculateGroupStandings (convert Player to PlayerStatsResponseDTO first):
List<PlayerStatsResponseDTO> playerStats = sortedPlayers.stream()
        .map(p -> toPlayerStatsDTO(p, statsByPlayer))
        .toList();
playerStats.sort(STANDINGS_COMPARATOR);
return playerStats.stream()
        .map(dto -> playerRepository.findById(dto.getPlayerId()).orElse(null))
        .filter(Objects::nonNull)
        .toList();
```

---

#### PROD-1: Zero Logging Despite @Slf4j
**Location:** Entire `TournamentServiceImpl.java`  
**Problem:** `@Slf4j` declared but no log calls. Critical state transitions have no audit trail.  
**Impact:** Impossible to debug production issues, no audit for financial operations.

**Fix:**
```java
// Add logging at key points:

// In startTournament:
log.info("Starting tournament id={}, format={}, players={}, mode={}", 
    tournamentId, tournament.getFormat(), n, tournament.getGenerationMode());

// In advanceToBracket:
log.info("Advancing tournament id={} to bracket, {} players advancing", 
    tournamentId, advancingPlayers.size());

// In forceAdvanceToBracket:
log.warn("Force advancing tournament id={} to bracket, cancelling {} events", 
    tournamentId, cancellableEvents.size());
log.info("Cancelled event id={}, refunded bets", event.getId());

// In create (tournament creation):
log.info("Created tournament id={}, name={}, format={}", 
    tournament.getId(), tournament.getName(), tournament.getFormat());

// In addPlayerToTournament:
log.info("Added player id={} to tournament id={}", playerId, tournamentId);

// In updateTournamentPlayerTeam:
log.info("Updated team for player id={} in tournament id={} to team id={}", 
    playerId, tournamentId, dto.getTeamId());
```

---

#### DB-1: Missing Composite Unique Constraint on TournamentPlayer
**Location:** `TournamentPlayer.java` entity  
**Problem:** Relies on application-level check, not race-safe.  
**Impact:** Same player can be added twice concurrently.

**Fix:**
```java
// Add to TournamentPlayer.java:
@Entity
@Table(name = "tournament_player", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "player_id"}))
public class TournamentPlayer {
    // ... existing fields
}
```

**Database Migration (V28):**
```sql
ALTER TABLE tournament_player 
ADD CONSTRAINT uk_tournament_player UNIQUE (tournament_id, player_id);
```

**Also add error handling in addPlayerToTournament:**
```java
try {
    tournamentPlayerRepository.save(tp);
} catch (DataIntegrityViolationException e) {
    throw new BusinessException("Player is already in this Tournament.");
}
```

---

#### CONC-2: Race Condition in addPlayerToTournament
**Location:** `TournamentServiceImpl.java:141-154`  
**Problem:** `existsByTournamentIdAndPlayerId` check followed by `save` is not atomic.  
**Impact:** Duplicate players if two requests hit simultaneously.

**Fix:** Covered by DB-1 above (unique constraint + exception handling).

---

### MEDIUM SEVERITY

#### BUG-4: computeNextPowerOf2 Incorrect for n=1 and n=0
**Location:** `TournamentServiceImpl.java:1152-1155`  
**Problem:** For `n=1`, returns 2 instead of 1. For `n=0`, undefined behavior.  
**Impact:** Could create phantom bracket slots.

**Fix:**
```java
private int computeNextPowerOf2(int n) {
    if (n <= 1) return 1;
    return Integer.highestOneBit(n - 1) << 1;
}
```

---

#### BUG-5: Floating-Point Precision in numRounds Calculation
**Location:** `startTournament()` line 252, `advanceToBracket()` line 379, `forceAdvanceToBracket()` line 501  
**Problem:** `Math.log(nextPowerOf2) / Math.log(2)` can yield `2.9999999` → truncates to 2.  
**Impact:** Wrong number of bracket rounds.

**Fix:**
```java
// Replace all occurrences of:
int numRounds = (int) (Math.log(nextPowerOf2) / Math.log(2));

// With:
int numRounds = Integer.numberOfTrailingZeros(nextPowerOf2);
```

---

#### BUG-7: startTournament Overwrites User-Provided startDate
**Location:** `TournamentServiceImpl.java:281`  
**Problem:** `create()` sets `startDate` from DTO, then `startTournament()` overwrites with `LocalDateTime.now()`.  
**Impact:** User's intended start date lost.

**Fix:**
```java
// Option 1: Don't overwrite
// REMOVE line 281: tournament.setStartDate(LocalDateTime.now());

// Option 2: Use separate field
// Add to Tournament.java:
@Column(name = "actual_start_date")
private LocalDateTime actualStartDate;

// In startTournament:
tournament.setActualStartDate(LocalDateTime.now());
// Keep original startDate as planned date
```

**Recommendation:** Use Option 2 for better audit trail.

---

#### SEC-4: startTournament Accepts dto as required=false
**Location:** `TournamentController.java:101`  
**Problem:** `dto` can be null, but LEAGUE_BRACKET format calls `dto.getNumberOfGroups()` without null check.  
**Impact:** NPE → 500 error.

**Fix:**
```java
// In TournamentServiceImpl.java, startTournament method:
if (tournament.getFormat() == TournamentFormat.LEAGUE_BRACKET) {
    if (dto == null) {
        throw new BusinessException("StartTournamentRequestDTO is required for LEAGUE_BRACKET format.");
    }
    
    Integer numberOfGroups = dto.getNumberOfGroups();
    if (numberOfGroups == null || numberOfGroups < 1) {
        throw new BusinessException("numberOfGroups must be set for LEAGUE_BRACKET format.");
    }
    // ... rest of LEAGUE_BRACKET logic
}
```

---

#### SEC-5: No Input Validation on startDate/endDate in create()
**Location:** `TournamentServiceImpl.java:98-99`  
**Problem:** No check that `startDate` is in future or `endDate > startDate`.  
**Impact:** Nonsensical dates in DB.

**Fix:**
```java
// Add to create() method after line 82:
if (dto.getStartDate() != null && dto.getStartDate().isBefore(LocalDateTime.now())) {
    throw new BusinessException("startDate must be in the future.");
}
if (dto.getEndDate() != null && dto.getStartDate() != null 
    && dto.getEndDate().isBefore(dto.getStartDate())) {
    throw new BusinessException("endDate must be after startDate.");
}
```

---

#### PERF-4: findAll() Loads All Tournaments Without Pagination
**Location:** `TournamentServiceImpl.java:110`  
**Problem:** `tournamentRepository.findAll()` loads every tournament.  
**Impact:** OOM or slow response as count grows.

**Fix:**
```java
// Change method signature in TournamentService interface:
Page<TournamentResponseDTO> findAll(Pageable pageable);

// Implementation:
@Override
@Transactional(readOnly = true)
public Page<TournamentResponseDTO> findAll(Pageable pageable) {
    return tournamentRepository.findAll(pageable)
            .map(tournamentMapper::toResponse);
}

// Update controller:
@GetMapping
public ResponseEntity<Page<TournamentResponseDTO>> getAllTournaments(Pageable pageable) {
    return ResponseEntity.ok(tournamentService.findAll(pageable));
}
```

---

#### PERF-5: Redundant DB Queries in calculateGroupStandings
**Location:** `TournamentServiceImpl.java:820`  
**Problem:** Calls `tournamentPlayerRepository.findByTournamentId()` every time, even when caller has data.  
**Impact:** N redundant queries for N groups.

**Fix:**
```java
// Change method signature:
private List<Player> calculateGroupStandings(
        Tournament tournament, 
        int groupNumber, 
        List<Event> groupStageEvents,
        List<TournamentPlayer> allTournamentPlayers) { // ADD PARAMETER
    
    if(tournament.getStatus() == TournamentStatus.CREATED){
        return allTournamentPlayers.stream()
                .filter(tp -> groupNumber == tp.getGroupNumber())
                .map(TournamentPlayer::getPlayer)
                .collect(Collectors.toList());
    }

    List<TournamentPlayer> groupPlayers = allTournamentPlayers.stream()
            .filter(tp -> groupNumber == tp.getGroupNumber())
            .toList();
    
    // ... rest of method unchanged
}

// Update all callers to pass the player list
```

---

#### PERF-6: Multiple Full-Tournament Event Loads in getScoreboard
**Location:** `getScoreboard()` → `buildLeagueBracketGroups()` + `buildBracketPlacements()`  
**Problem:** Both methods call `eventRepository.findAllByTournamentId()`.  
**Impact:** Doubled DB load.

**Fix:**
```java
@Override
public TournamentScoreboardResponseDTO getScoreboard(Long tournamentId) {
    Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));

    TournamentScoreboardResponseDTO response = new TournamentScoreboardResponseDTO();
    response.setTournamentId(tournament.getId());
    response.setTournamentName(tournament.getName());
    response.setFormat(tournament.getFormat().name());

    // LOAD EVENTS ONCE
    List<Event> allEvents = eventRepository.findAllByTournamentId(tournamentId);

    if (tournament.getFormat() == TournamentFormat.LEAGUE) {
        response.setEntries(buildLeagueScoreboard(tournament, allEvents));
    } else if (tournament.getFormat() == TournamentFormat.BRACKET) {
        response.setPlacements(buildBracketPlacements(tournament, allEvents));
    } else if (tournament.getFormat() == TournamentFormat.LEAGUE_BRACKET) {
        response.setGroups(buildLeagueBracketGroups(tournament, allEvents));
        boolean hasKnockout = tournament.getRounds().stream()
                .anyMatch(r -> r.getPhaseType() == PhaseType.KNOCKOUT);
        if (hasKnockout) {
            response.setPlacements(buildBracketPlacements(tournament, allEvents));
        }
    }

    return response;
}

// Update method signatures to accept events parameter
```

---

#### DOMAIN-2: updateTournamentPlayerTeam Allows Changes After Tournament Starts
**Location:** `TournamentServiceImpl.java:161-175`  
**Problem:** No status check. Team can be changed even when `status == IN_PROGRESS`.  
**Impact:** Roster manipulation after matches played.

**Fix:**
```java
@Override
@Transactional
public TournamentPlayerResponseDTO updateTournamentPlayerTeam(Long tournamentId, Long playerId, PatchTournamentPlayerTeamRequestDTO dto) {
    currentUserContext.requireAdmin();
    
    Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));
    
    // ADD THIS CHECK:
    if (tournament.getStatus() != TournamentStatus.CREATED) {
        throw new BusinessException("Cannot change teams after tournament has started.");
    }
    
    Team team = teamRepository.findById(dto.getTeamId())
            .orElseThrow(() -> new ResourceNotFoundException("Team", "id", dto.getTeamId()));

    TournamentPlayer tournamentPlayer = tournamentPlayerRepository.findByTournamentIdAndPlayerId(tournamentId, playerId)
            .orElseThrow(() -> new ResourceNotFoundException("TournamentPlayer", "playerId", playerId));

    tournamentPlayer.setTeam(team);
    tournamentPlayer = tournamentPlayerRepository.save(tournamentPlayer);

    return toTournamentPlayerResponse(tournamentPlayer);
}
```

---

#### ERR-2: numberOfGroups Validation Insufficient
**Location:** `TournamentServiceImpl.java:219-222`  
**Problem:** Only checks null and < 1. Doesn't validate sensible division or power-of-2 requirement for bracket.  
**Impact:** Degenerate brackets.

**Fix:**
```java
if (tournament.getFormat() == TournamentFormat.LEAGUE_BRACKET) {
    if (dto == null) {
        throw new BusinessException("StartTournamentRequestDTO is required for LEAGUE_BRACKET format.");
    }
    
    Integer numberOfGroups = dto.getNumberOfGroups();
    Integer playersAdvancingPerGroup = dto.getPlayersAdvancingPerGroup();
    
    if (numberOfGroups == null || numberOfGroups < 1) {
        throw new BusinessException("numberOfGroups must be at least 1.");
    }
    
    if (playersAdvancingPerGroup == null || playersAdvancingPerGroup < 1) {
        throw new BusinessException("playersAdvancingPerGroup must be at least 1.");
    }
    
    int playersPerGroup = n / numberOfGroups;
    if (playersPerGroup < 2) {
        throw new BusinessException("Not enough players per group. Need at least 2 players per group.");
    }
    
    int totalAdvancing = numberOfGroups * playersAdvancingPerGroup;
    if (totalAdvancing < 2) {
        throw new BusinessException("Total advancing players must be at least 2.");
    }
    
    // Check if total advancing is power of 2
    if ((totalAdvancing & (totalAdvancing - 1)) != 0) {
        throw new BusinessException("Total advancing players (groups × playersAdvancingPerGroup) must be a power of 2.");
    }
    
    // Use existing validator
    TournamentGroupConfigValidator.validateGroupConfig(n, numberOfGroups, playersAdvancingPerGroup);
    
    // ... rest of logic
}
```

---

#### ERR-3: forceAdvanceToBracket Cancels Events Before Checking Knockout Existence
**Location:** `TournamentServiceImpl.java:461-477`  
**Problem:** Events cancelled, then knockout check fails. If transaction doesn't roll back, tournament stuck.  
**Impact:** Events cancelled but no bracket created.

**Fix:**
```java
@Override
@Transactional
public TournamentResponseDTO forceAdvanceToBracket(Long tournamentId) {
    currentUserContext.requireAdmin();

    Tournament tournament = tournamentRepository.findById(tournamentId)
            .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));

    if (tournament.getFormat() != TournamentFormat.LEAGUE_BRACKET) {
        throw new BusinessException("Tournament is not LEAGUE_BRACKET format.");
    }

    if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
        throw new BusinessException("Tournament is not in progress.");
    }

    // MOVE THIS CHECK UP (before cancelling events):
    boolean hasKnockoutRounds = tournament.getRounds().stream()
            .anyMatch(r -> r.getPhaseType() == PhaseType.KNOCKOUT);
    if (hasKnockoutRounds) {
        throw new BusinessException("Knockout phase already exists.");
    }

    List<Event> allEvents = eventRepository.findAllByTournamentId(tournamentId);
    List<Event> groupStageEvents = allEvents.stream()
            .filter(e -> e.getRound().getPhaseType() == PhaseType.GROUP_STAGE)
            .toList();

    boolean hasInProgress = groupStageEvents.stream()
            .anyMatch(e -> e.getStatus() == EventStatus.IN_PROGRESS);
    if (hasInProgress) {
        throw new InvalidStateException(
                "Cannot force advance: there are in-progress group stage events. Finish them first.");
    }

    // NOW cancel events (after all checks pass)
    List<Event> cancellableEvents = groupStageEvents.stream()
            .filter(e -> e.getStatus() != EventStatus.COMPLETED)
            .toList();

    for (Event event : cancellableEvents) {
        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);

        marketRepository.findByEventId(event.getId()).ifPresent(market -> {
            market.setStatus(MarketStatus.CANCELLED);
            marketRepository.save(market);
        });

        betService.cancelBetsForEvent(event);
    }

    // ... rest of method
}
```

---

#### DB-2: Missing Index on Event.tournament_id + status
**Location:** `Event.java` entity  
**Problem:** `findCompletedByTournamentId` and `findAllByTournamentId` are full table scans.  
**Impact:** Slow queries.

**Database Migration (V28):**
```sql
CREATE INDEX idx_event_tournament_status ON event(tournament_id, status);
```

---

#### DB-3: Missing Index on TournamentPlayer.tournament_id + group_number
**Location:** `TournamentPlayer.java` entity  
**Problem:** `calculateGroupStandings` filters by these columns.  
**Impact:** Slow queries.

**Database Migration (V28):**
```sql
CREATE INDEX idx_tournament_player_group ON tournament_player(tournament_id, group_number);
```

---

#### QUAL-3: Magic Numbers Throughout
**Location:** Multiple methods  
**Problem:** Hardcoded values like `1.4`, `2.0`, `0.2`, `64`, `3`, `1` without named constants.  
**Impact:** Hard to understand and maintain.

**Fix:**
```java
// Add constants at top of class:
private static final BigDecimal THIRD_PLACE_MULTIPLIER = BigDecimal.valueOf(1.4);
private static final BigDecimal FINAL_MULTIPLIER = BigDecimal.valueOf(2.0);
private static final BigDecimal MULTIPLIER_INCREMENT = BigDecimal.valueOf(0.2);
private static final int POINTS_PER_WIN = 3;
private static final int POINTS_PER_DRAW = 1;
private static final int ASCII_OFFSET_FOR_GROUPS = 64; // 'A' - 1
private static final int MIN_PLAYERS_PER_GROUP = 2;

// Replace all occurrences:
// Line 209: round.setMultiplier(BigDecimal.ONE); → keep as is (clear intent)
// Line 270: thirdPlaceRound.setMultiplier(BigDecimal.valueOf(1.4)); 
//        → thirdPlaceRound.setMultiplier(THIRD_PLACE_MULTIPLIER);
// Line 739: dto.setPoints(acc.wins * 3 + acc.draws); 
//        → dto.setPoints(acc.wins * POINTS_PER_WIN + acc.draws * POINTS_PER_DRAW);
// Line 233: char letter = (char)(g + 64); 
//        → char letter = (char)(g + ASCII_OFFSET_FOR_GROUPS);
```

---

### LOW SEVERITY

#### BUG-8: Misleading Error Message in generateBracketAutoEvents
**Location:** `TournamentServiceImpl.java:1004`  
**Problem:** Error says "No group stage round found" but filter is for KNOCKOUT.  
**Impact:** Confusing during debugging.

**Fix:**
```java
.orElseThrow(() -> new BusinessException("No knockout round found"));
```

---

#### QUAL-5: Commented-Out Code Left in Production
**Location:** Line 814 (group filter), line 1034 (knockout offset)  
**Problem:** Dead code reduces readability.  
**Impact:** Confusion about intent.

**Fix:** Remove commented lines or implement properly.

---

#### SPRING-3: @Transactional on Read-Only Methods Unnecessary
**Location:** `findPlayersByTournamentId`, `findRoundsByTournamentId`, `getScoreboard`, `findAll`  
**Problem:** Missing `@Transactional(readOnly = true)` optimization hint.  
**Impact:** Missed query optimization opportunities.

**Fix:**
```java
@Override
@Transactional(readOnly = true)
public List<TournamentResponseDTO> findAll() {
    // ...
}

@Override
@Transactional(readOnly = true)
public TournamentPlayersResponseDTO findPlayersByTournamentId(Long tournamentId) {
    // ...
}

// etc.
```

---

#### PROD-3: Collections.shuffle() Uses Non-Secure Random
**Location:** Lines 298, 1118  
**Problem:** `Collections.shuffle()` uses `ThreadLocalRandom`. For betting app, predictable shuffles could be exploited.  
**Impact:** Theoretically exploitable.

**Fix:**
```java
// Add field:
private static final SecureRandom SECURE_RANDOM = new SecureRandom();

// Replace:
Collections.shuffle(shuffledPlayers);
// With:
Collections.shuffle(shuffledPlayers, SECURE_RANDOM);
```

---

#### DOMAIN-3: StatsAccumulator Should Be Proper Domain Class
**Location:** Lines 802-809  
**Problem:** Inner static class with public mutable fields.  
**Impact:** Poor encapsulation.

**Fix:**
```java
// Create new file: src/main/java/com/franciscomaath/resenhaapi/domain/valueobject/PlayerStats.java
package com.franciscomaath.resenhaapi.domain.valueobject;

import lombok.Value;

@Value
public class PlayerStats {
    int matchesPlayed;
    int wins;
    int losses;
    int draws;
    int goalsScored;
    int goalsConceded;
    
    public int getGoalDifference() {
        return goalsScored - goalsConceded;
    }
    
    public int getPoints() {
        return wins * 3 + draws;
    }
}

// Update StatsAccumulator to use PlayerStats
```

---

## Testing Strategy

### Unit Tests
- Test each extracted service independently
- Mock repositories and external services
- Verify all business rules

### Integration Tests
- Test full tournament lifecycle (create → add players → start → complete)
- Test concurrent operations (optimistic locking)
- Test authorization (admin-only endpoints)
- Test database constraints (unique constraints)

### Performance Tests
- Load test with 1000+ tournaments
- Load test with 64-player brackets
- Verify N+1 queries are eliminated (use Hibernate statistics)

### Security Tests
- Test all endpoints as non-admin user
- Test concurrent player additions (race condition)
- Test concurrent tournament starts

---

## Database Migration Script

**File:** `src/main/resources/db/migration/V28__add_indexes_and_constraints.sql`

```sql
-- Add optimistic locking to tournament
ALTER TABLE tournament ADD COLUMN version BIGINT DEFAULT 0;

-- Add unique constraint on tournament_player
ALTER TABLE tournament_player 
ADD CONSTRAINT uk_tournament_player UNIQUE (tournament_id, player_id);

-- Add indexes for performance
CREATE INDEX idx_event_tournament_status ON event(tournament_id, status);
CREATE INDEX idx_tournament_player_group ON tournament_player(tournament_id, group_number);

-- Optional: Add actual_start_date for audit trail
ALTER TABLE tournament ADD COLUMN actual_start_date TIMESTAMP;
```

---

## Verification Checklist

After implementing all fixes:

- [ ] All existing tests pass
- [ ] New unit tests for each fix
- [ ] Integration tests for tournament lifecycle
- [ ] Concurrent operation tests pass
- [ ] Authorization tests pass (admin-only endpoints)
- [ ] Performance tests show improvement
- [ ] No N+1 queries (verify with Hibernate statistics)
- [ ] Database migration runs successfully
- [ ] Code compiles without warnings
- [ ] Linting passes
- [ ] Code review completed

---

## Notes for AI Agent

1. **Work in phases** — Complete Phase 1 (Critical) before moving to Phase 2
2. **Test after each fix** — Don't batch multiple fixes without testing
3. **Preserve existing behavior** — These are bug fixes and improvements, not feature changes
4. **Follow existing code style** — Match the project's naming conventions and formatting
5. **Update documentation** — If you change method signatures, update JavaDoc
6. **Commit frequently** — One commit per logical change for easier review
7. **Run full test suite** — After each phase, run all tests to catch regressions

---

## Success Metrics

After implementation:
- **Zero critical bugs** in production
- **50% reduction** in database queries for scoreboard endpoint
- **100% authorization coverage** on state-changing endpoints
- **Zero data corruption** from concurrent operations
- **Full audit trail** via logging
- **< 200 lines** per service class (after refactoring)

---

**End of Improvement Plan**
