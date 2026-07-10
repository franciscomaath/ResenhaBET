package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.request.LinkUserRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.PlayerRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.PlayerUpdateRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.PlayerActiveRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerInviteResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerStatsResponseDTO;
import com.franciscomaath.resenhaapi.service.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerService playerService;

    @PostMapping
    @Operation(summary = "Create a new player", description = "Creates a new player for the tournaments.")
    public ResponseEntity<PlayerResponseDTO> createPlayer(
            @RequestBody @Valid PlayerRequestDTO dto
    ){
        return ResponseEntity.ok(playerService.create(dto));
    }

    @GetMapping
    @Operation(summary = "Get all players", description = "List all existing players.")
    public ResponseEntity<List<PlayerResponseDTO>> getAllPlayers(){
        return ResponseEntity.ok(playerService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a player", description = "Get an specific player.")
    public ResponseEntity<PlayerResponseDTO> getPlayer(
            @PathVariable Long id
    ){
        return ResponseEntity.ok(playerService.findPlayerById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a player", description = "Edit an specific player, changing its name and status.")
    public ResponseEntity<PlayerResponseDTO> togglePlayerStatus(
            @PathVariable Long id,
            @RequestBody @Valid PlayerUpdateRequestDTO dto
    ){
        return ResponseEntity.ok(playerService.update(id, dto));
    }

    @PatchMapping("/{id}/link-user")
    @Operation(summary = "Link player to user", description = "Links an existing user to an existing player. Admin only.")
    public ResponseEntity<PlayerResponseDTO> linkUser(
            @PathVariable Long id,
            @RequestBody @Valid LinkUserRequestDTO dto
    ){
        return ResponseEntity.ok(playerService.linkUser(id, dto));
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get player stats", description = "Returns all-time stats or tournament-filtered stats including average round multiplier.")
    public ResponseEntity<PlayerStatsResponseDTO> getPlayerStats(
            @PathVariable Long id,
            @RequestParam(required = false) Long tournamentId
    ){
        return ResponseEntity.ok(playerService.getPlayerStats(id, tournamentId));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Change player active status", description = "Change player active status without deleting it.")
    public ResponseEntity<PlayerResponseDTO> changeActiveStatus(
            @PathVariable Long id,
            @RequestBody @Valid PlayerActiveRequestDTO dto
    ){
        return ResponseEntity.ok(playerService.changeActiveStatus(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete player", description = "Soft deletes a player if it's not currently playing in an active tournament.")
    public ResponseEntity<Void> softDeletePlayer(
            @PathVariable Long id
    ){
        playerService.softDeletePlayer(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/invite")
    @Operation(summary = "Generate invite token", description = "Generates an invite token for a user to claim this player profile.")
    public ResponseEntity<PlayerInviteResponseDTO> generateInvite(
            @PathVariable Long id
    ){
        return ResponseEntity.ok(playerService.generateInvite(id));
    }

}
