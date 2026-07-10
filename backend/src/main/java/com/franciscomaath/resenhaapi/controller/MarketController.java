package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.request.MarketStatusRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.MarketResponseDTO;
import com.franciscomaath.resenhaapi.domain.exception.ValidationException;
import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.service.MarketService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/markets")
public class MarketController {

    private final MarketService marketService;
    private final EventRepository eventRepository;
    private final GroupAuthorizationService groupAuthorizationService;
    private final CurrentUserContext currentUserContext;

    private static final Set<String> VALID_STATUSES = Set.of("OPEN", "CLOSED");

    @GetMapping("/{eventId}")
    @Operation(summary = "Get markets by event", description = "Returns all markets with outcomes and odds for a specific event.")
    public ResponseEntity<List<MarketResponseDTO>> getMarketsByEvent(@PathVariable Long eventId) {
        return ResponseEntity.ok(marketService.findAllByEventId(eventId));
    }

    @PostMapping("/{eventId}/status")
    @Operation(summary = "Set market status", description = "Admin only. Opens or closes all markets for an event.")
    public ResponseEntity<List<MarketResponseDTO>> setMarketStatus(
            @PathVariable Long eventId,
            @RequestBody @Valid MarketStatusRequestDTO dto
    ) {
        if (!VALID_STATUSES.contains(dto.getStatus())) {
            throw new ValidationException("Status invalido. Use OPEN ou CLOSED.");
        }
        requireMarketMutation(eventId);

        if ("OPEN".equals(dto.getStatus())) {
            marketService.openMarket(eventId);
        } else {
            marketService.closeMarket(eventId);
        }

        return ResponseEntity.ok(marketService.findAllByEventId(eventId));
    }

    private void requireMarketMutation(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));
        Long tournamentId = event.getTournament().getId();
        if (event.getTournament().getType() == TournamentType.REAL_FOOTBALL) {
            currentUserContext.requireAdmin();
            groupAuthorizationService.requireTournamentAccess(tournamentId);
        } else {
            groupAuthorizationService.requireTournamentAdmin(tournamentId);
        }
    }
}
