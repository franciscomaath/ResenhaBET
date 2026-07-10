package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.request.EventRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.CompletedEventRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.EventUpdateRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.FinishEventRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.PatchEventPlayersRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.EventPatchRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.EventDatetimeRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.EventResponseDTO;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface EventService {

    EventResponseDTO create(EventRequestDTO dto);

    EventResponseDTO createCompleted(CompletedEventRequestDTO dto);

    EventResponseDTO findEvent(Long eventId);

    List<EventResponseDTO> findAll(Long tournamentId, EventStatus status);

    EventResponseDTO startEvent(Long eventId);

    EventResponseDTO resetEvent(Long eventId);

    EventResponseDTO finishEvent(Long eventId);

    EventResponseDTO reopenEvent(Long eventId);

    EventResponseDTO updateScore(Long eventId, EventUpdateRequestDTO dto);

    EventResponseDTO patchEventPlayers(Long eventId, PatchEventPlayersRequestDTO dto);

    EventResponseDTO recordPenalties(Long eventId, FinishEventRequestDTO dto);

    EventResponseDTO cancelEvent(Long eventId);

    EventResponseDTO editEvent(Long eventId, EventPatchRequestDTO dto);

    EventResponseDTO rescheduleEvent(Long eventId, EventDatetimeRequestDTO dto);

    void softDeleteEvent(Long eventId);
}
