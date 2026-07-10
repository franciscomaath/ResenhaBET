package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.request.EventRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.CompletedEventRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.EventUpdateRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.FinishEventRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.PatchEventPlayersRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.EventPatchRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.EventDatetimeRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.EventResponseDTO;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @Operation(summary = "Create a new event", description = "Creates a new event for the tournament.")
    public ResponseEntity<EventResponseDTO> createEvent(
            @RequestBody @Valid EventRequestDTO dto
    ) {
        return ResponseEntity.ok(eventService.create(dto));
    }

    @PostMapping("/completed")
    @Operation(summary = "Insert a completed event", description = "Creates a completed event with scores, including bye matches.")
    public ResponseEntity<EventResponseDTO> createCompletedEvent(
            @RequestBody @Valid CompletedEventRequestDTO dto
    ) {
        return ResponseEntity.ok(eventService.createCompleted(dto));
    }

    @GetMapping
    @Operation(summary = "Get all events", description = "List all existing events.")
    public ResponseEntity<List<EventResponseDTO>> getAllEvents(
            @RequestParam(value = "tournamentId", required = false) Long tournamentId,
            @RequestParam(value = "status", required = false) EventStatus status
    ) {
        return ResponseEntity.ok(eventService.findAll(tournamentId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an specific event", description = "Get an specific event.")
    public ResponseEntity<EventResponseDTO> getEvent(
            @PathVariable Long id
    ) {
        
        return ResponseEntity.ok(eventService.findEvent(id));
    }

    @PostMapping("/{id}/score")
    @Operation(summary = "Update event score", description = "Updates the score of an existing event.")
    public ResponseEntity<EventResponseDTO> updateEventScore(
            @PathVariable("id") Long id,
            @RequestBody @Valid EventUpdateRequestDTO dto
    ) {
        return ResponseEntity.ok(eventService.updateScore(id, dto));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start the event", description = "Starts the event, changing its status to IN_PROGRESS.")
    public ResponseEntity<EventResponseDTO> startEvent(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(eventService.startEvent(id));
    }

    @PostMapping("/{id}/reset")
    @Operation(summary = "Reset the event", description = "Returns an IN_PROGRESS event to CREATED and reopens its markets.")
    public ResponseEntity<EventResponseDTO> resetEvent(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(eventService.resetEvent(id));
    }

    @PostMapping("/{id}/end")
    @Operation(summary = "Finish the event", description = "Finishes the event, changing its status to FINISHED.")
    public ResponseEntity<EventResponseDTO> finishEvent(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(eventService.finishEvent(id));
    }

    @PostMapping("/{id}/reopen")
    @Operation(summary = "Reopen the event", description = "Reopens a completed event, rolls back bets, and lets the admin change the score again.")
    public ResponseEntity<EventResponseDTO> reopenEvent(
            @PathVariable("id") Long id
    ) {
        return ResponseEntity.ok(eventService.reopenEvent(id));
    }

    @PatchMapping("/{id}/players")
    @Operation(summary = "Assign players to event", description = "Assigns playerHome and/or playerAway to an existing event. Admin only.")
    public ResponseEntity<EventResponseDTO> patchEventPlayers(
            @PathVariable Long id,
            @RequestBody @Valid PatchEventPlayersRequestDTO dto
    ) {
        return ResponseEntity.ok(eventService.patchEventPlayers(id, dto));
    }

    @PatchMapping("/{id}/penalties")
    @Operation(summary = "Record penalty scores", description = "Updates penalty scores for an event in PENALTIES status. Send status: COMPLETED to finalize.")
    public ResponseEntity<EventResponseDTO> recordPenalties(
            @PathVariable Long id,
            @RequestBody @Valid FinishEventRequestDTO dto
    ) {
        return ResponseEntity.ok(eventService.recordPenalties(id, dto));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Edit event details", description = "Edits general details of a CREATED event. System admin required for REAL_FOOTBALL.")
    public ResponseEntity<EventResponseDTO> editEvent(
            @PathVariable Long id,
            @RequestBody @Valid EventPatchRequestDTO dto
    ) {
        return ResponseEntity.ok(eventService.editEvent(id, dto));
    }

    @PatchMapping("/{id}/datetime")
    @Operation(summary = "Reschedule event", description = "Changes the game datetime of an event. System admin required for REAL_FOOTBALL.")
    public ResponseEntity<EventResponseDTO> rescheduleEvent(
            @PathVariable Long id,
            @RequestBody @Valid EventDatetimeRequestDTO dto
    ) {
        return ResponseEntity.ok(eventService.rescheduleEvent(id, dto));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel event", description = "Cancels a CREATED or IN_PROGRESS event, cancels its markets and refunds pending bets.")
    public ResponseEntity<EventResponseDTO> cancelEvent(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(eventService.cancelEvent(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete event", description = "Soft deletes an event if it is CREATED and has no bets.")
    public ResponseEntity<Void> softDeleteEvent(
            @PathVariable Long id
    ) {
        eventService.softDeleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
