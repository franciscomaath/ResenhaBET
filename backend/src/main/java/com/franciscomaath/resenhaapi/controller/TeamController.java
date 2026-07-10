package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.request.TeamRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.UpdateGameForecastTeamIdRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TeamResponseDTO;
import com.franciscomaath.resenhaapi.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    @PostMapping
    @Operation(summary = "Create a new team", description = "Creates a new team manually.")
    public ResponseEntity<TeamResponseDTO> createTeam(
            @RequestBody @Valid TeamRequestDTO dto
    ) {
        return ResponseEntity.ok(teamService.create(dto));
    }

    @GetMapping
    @Operation(summary = "Get all teams", description = "List all existing teams.")
    public ResponseEntity<List<TeamResponseDTO>> getAllTeams() {
        return ResponseEntity.ok(teamService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a team", description = "Get a specific team by id.")
    public ResponseEntity<TeamResponseDTO> getTeamById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(teamService.findById(id));
    }

    @PatchMapping("/{id}/game-forecast-id")
    @Operation(summary = "Update team game forecast ID", description = "Updates the gameForecastTeamId for a team.")
    public ResponseEntity<TeamResponseDTO> updateGameForecastTeamId(
            @PathVariable Long id,
            @RequestBody @Valid UpdateGameForecastTeamIdRequestDTO dto
    ) {
        return ResponseEntity.ok(teamService.updateGameForecastTeamId(id, dto.getGameForecastTeamId()));
    }
}
