package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.request.CompetitionRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.CompetitionResponseDTO;
import com.franciscomaath.resenhaapi.service.CompetitionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/competitions")
public class CompetitionController {

    private final CompetitionService competitionService;

    @PostMapping
    @Operation(summary = "Create a competition", description = "Creates a new external competition. Admin only.")
    public ResponseEntity<CompetitionResponseDTO> createCompetition(
            @RequestBody @Valid CompetitionRequestDTO dto
    ) {
        return ResponseEntity.ok(competitionService.create(dto));
    }

    @GetMapping
    @Operation(summary = "List competitions", description = "Lists all competitions. Filters by active=true by default.")
    public ResponseEntity<List<CompetitionResponseDTO>> getAllCompetitions(
            @RequestParam(required = false, defaultValue = "true") Boolean active
    ) {
        return ResponseEntity.ok(competitionService.findAll(active));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a competition", description = "Returns a specific competition by ID.")
    public ResponseEntity<CompetitionResponseDTO> getCompetition(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(competitionService.findById(id));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Toggle competition active status", description = "Toggles the active flag of a competition. Admin only.")
    public ResponseEntity<CompetitionResponseDTO> toggleCompetitionActive(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(competitionService.toggleActive(id));
    }
}
