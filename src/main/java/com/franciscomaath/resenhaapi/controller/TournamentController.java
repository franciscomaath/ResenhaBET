package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.request.StartTournamentRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.PatchTournamentPlayerTeamRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.TournamentPlayerRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.TournamentRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.TournamentPatchRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.*;
import com.franciscomaath.resenhaapi.service.FixtureSyncService;
import com.franciscomaath.resenhaapi.service.OddsImportService;
import com.franciscomaath.resenhaapi.service.validator.TournamentGroupConfigValidator;
import com.franciscomaath.resenhaapi.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tournaments")
public class TournamentController {

    private final TournamentService tournamentService;
    private final TournamentGroupConfigValidator tournamentGroupConfigValidator;
    private final FixtureSyncService fixtureSyncService;
    private final OddsImportService oddsImportService;

    @PostMapping
    @Operation(summary = "Create a new tournament", description = "Creates a new tournament.")
    public ResponseEntity<TournamentResponseDTO> createTournament(
            @RequestBody @Valid TournamentRequestDTO dto
    ) {
        return ResponseEntity.ok(tournamentService.create(dto));
    }

    @GetMapping
    @Operation(summary = "Get all tournaments", description = "List all existing tournaments.")
    public ResponseEntity<Page<TournamentResponseDTO>> getAllTournaments(Pageable pageable) {
        return ResponseEntity.ok(tournamentService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tournament details", description = "Get details of a specific tournament.")
    public ResponseEntity<TournamentResponseDTO> getTournamentById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(tournamentService.getTournamentById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update tournament", description = "Updates a tournament's settings. Admin only. Not allowed for REAL_FOOTBALL.")
    public ResponseEntity<TournamentResponseDTO> updateTournament(
            @PathVariable("id") Long id,
            @RequestBody @Valid TournamentPatchRequestDTO dto
    ) {
        return ResponseEntity.ok(tournamentService.updateTournament(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete tournament", description = "Deletes a tournament if no bets or games have started. Admin only.")
    public ResponseEntity<Void> softDeleteTournament(@PathVariable("id") Long id) {
        tournamentService.softDeleteTournament(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel tournament", description = "Cancels a tournament, refunding all bets and cancelling open markets/events. Admin only.")
    public ResponseEntity<Void> cancelTournament(@PathVariable("id") Long id) {
        tournamentService.cancelTournament(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/players")
    @Operation(summary = "Get players in a tournament", description = "Lists all registered tournament players with player and team information.")
    public ResponseEntity<TournamentPlayersResponseDTO> getTournamentPlayers(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(tournamentService.findPlayersByTournamentId(id));
    }

    @PostMapping("/{id}/players/{playerId}/team")
    @Operation(summary = "Set or update a player's team", description = "Sets or updates the team of a registered player in the tournament.")
    public ResponseEntity<TournamentPlayerResponseDTO> patchTournamentPlayerTeam(
            @PathVariable("id") Long id,
            @PathVariable("playerId") Long playerId,
            @RequestBody @Valid PatchTournamentPlayerTeamRequestDTO dto
    ) {
        return ResponseEntity.ok(tournamentService.updateTournamentPlayerTeam(id, playerId, dto));
    }

    @GetMapping("/tournament-group-config")
    @Operation(summary = "Get valid tournament group configurations",
            description = "Returns valid group counts and player distribution for a given player count.")
    public ResponseEntity<TournamentGroupConfigResponseDTO> getTournamentGroupConfig(
            @RequestParam("playerCount") int playerCount
    ) {
        return ResponseEntity.ok(tournamentGroupConfigValidator.getValidConfigs(playerCount));
    }

    @GetMapping("/{id}/rounds")
    @Operation(summary = "Get tournament rounds", description = "Lists all rounds with name, multiplier and order.")
    public ResponseEntity<List<TournamentRoundResponseDTO>> getTournamentRounds(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(tournamentService.findRoundsByTournamentId(id));
    }

    @GetMapping("/{id}/scoreboard")
    @Operation(summary = "Get tournament scoreboard", description = "Returns the leaderboard with all players stats for the tournament.")
    public ResponseEntity<TournamentScoreboardResponseDTO> getScoreboard(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(tournamentService.getScoreboard(id));
    }

    @PostMapping("/{id}/players")
    @Operation(summary = "Add a player to a tournament", description = "Adds an existing player to an existing tournament.")
    public ResponseEntity<TournamentPlayerResponseDTO> addPlayerToTournament(
            @PathVariable("id") Long id,
            @RequestBody @Valid TournamentPlayerRequestDTO dto
    ) {
        return ResponseEntity.ok(tournamentService.addPlayerToTournament(id, dto));
    }

    @DeleteMapping("/{id}/players/{playerId}")
    @Operation(summary = "Remove a player from a tournament", description = "Removes a player from a tournament. Only allowed when tournament status is CREATED.")
    public ResponseEntity<Void> removePlayerFromTournament(
            @PathVariable("id") Long id,
            @PathVariable("playerId") Long playerId
    ) {
        tournamentService.removePlayerFromTournament(id, playerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start the tournament", description = "Starts the tournament, changing its status to IN_PROGRESS and creating all rounds.")
    public ResponseEntity<TournamentResponseDTO> startTournament(
            @PathVariable("id") Long id,
            @RequestBody(required = false) @Valid StartTournamentRequestDTO dto
    ){
        return ResponseEntity.ok(tournamentService.startTournament(id, dto));
    }

    @PostMapping("/{id}/advance-to-bracket")
    @Operation(summary = "Advance to bracket phase", description = "Transitions a LEAGUE_BRACKET tournament from group stage to knockout phase. Admin only.")
    public ResponseEntity<TournamentResponseDTO> advanceToBracket(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(tournamentService.advanceToBracket(id));
    }

    @PostMapping("/{id}/force-advance-to-bracket")
    @Operation(summary = "Force advance to bracket phase",
            description = "Cancels remaining group stage events, refunds bettors, and transitions a LEAGUE_BRACKET tournament to knockout phase. Admin only.")
    public ResponseEntity<TournamentResponseDTO> forceAdvanceToBracket(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(tournamentService.forceAdvanceToBracket(id));
    }

    @PostMapping("/{id}/sync-fixtures")
    @Operation(summary = "Sync tournament fixtures",
            description = "Recalculates and updates all tournament fixtures based on current player data. Useful for correcting issues or after manual adjustments. Admin only.")
    public ResponseEntity<SyncResult> syncFixtures(
            @PathVariable("id") Long id
            ) {
        return ResponseEntity.ok(fixtureSyncService.sync(id));
    }

    @PostMapping("/{id}/sync-odds")
    @Operation(summary = "Sync tournament odds",
            description = "Recalculates and updates all REAL_FOOTBALL tournament odds. Useful for correcting issues or after manual adjustments. Admin only.")
    public ResponseEntity<OddsImportResult> syncOdds(
            @PathVariable Long id
    ){
        return ResponseEntity.ok(oddsImportService.importForTournament(id));
    }

    @GetMapping("/{id}/ranking")
    @Operation(summary = "Get tournament bet ranking",
            description = "Returns the bet ranking for the tournament based on wallet balance.")
    public ResponseEntity<List<BetRankingResponseDTO>> getBetRanking(
            @PathVariable Long id
    ){
        return ResponseEntity.ok(tournamentService.getBetRanking(id));
    }
}
