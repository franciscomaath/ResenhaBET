package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.request.BetRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.BetSlipResponseDTO;
import com.franciscomaath.resenhaapi.service.BetService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/bets")
public class BetController {

    private final BetService betService;

    @PostMapping
    @Operation(summary = "Place a bet", description = "Place a single or multiple bet. Deducts stake from wallet atomically.")
    public ResponseEntity<BetSlipResponseDTO> placeBet(
            @RequestBody @Valid BetRequestDTO dto
    ) {
        return ResponseEntity.ok(betService.placeBet(dto));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my bets", description = "Returns the authenticated user's bet history with items and event data.")
    public ResponseEntity<List<BetSlipResponseDTO>> getMyBets() {
        return ResponseEntity.ok(betService.getUserBets());
    }

    @GetMapping
    @Operation(summary = "Get bets by event", description = "Admin only. Returns all bet slips for a specific event.")
    public ResponseEntity<List<BetSlipResponseDTO>> getBetsByEvent(
            @RequestParam Long eventId
    ) {
        return ResponseEntity.ok(betService.getBetsByEvent(eventId));
    }
}
