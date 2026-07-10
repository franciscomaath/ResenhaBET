package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.response.EventResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.CompletedEventRequestDTO;
import com.franciscomaath.resenhaapi.controller.exception.GlobalExceptionHandler;
import com.franciscomaath.resenhaapi.domain.exception.InvalidStateException;
import com.franciscomaath.resenhaapi.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createEvent_shouldReturnEvent() throws Exception {
        EventResponseDTO response = new EventResponseDTO();
        response.setId(1L);
        response.setTournamentId(10L);
        response.setRoundId(100L);
        response.setPlayerHomeId(5L);
        response.setPlayerAwayId(6L);
        response.setGameDatetime(LocalDateTime.of(2026, 4, 10, 20, 0));
        response.setStatus("PENDING");

        when(eventService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tournamentId": 10,
                                  "roundId": 100,
                                  "playerHomeId": 5,
                                  "playerAwayId": 6
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(eventService).create(any());
    }

    @Test
    void createCompletedEvent_shouldReturnEvent() throws Exception {
        EventResponseDTO response = new EventResponseDTO();
        response.setId(11L);
        response.setStatus("COMPLETED");

        when(eventService.createCompleted(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/events/completed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tournamentId": 10,
                                  "roundId": 100,
                                  "playerHomeId": 5,
                                  "homeScore": 2,
                                  "awayScore": 0,
                                  "isBye": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(eventService).createCompleted(any(CompletedEventRequestDTO.class));
    }

    @Test
    void createEvent_withoutRequiredFields_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roundId": 100,
                                  "playerHomeId": 5
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Input"))
                .andExpect(jsonPath("$.fieldErrors.tournamentId").exists())
                .andExpect(jsonPath("$.fieldErrors.playerAwayId").exists());
    }

    @Test
    void getAllEvents_shouldReturnList() throws Exception {
        EventResponseDTO first = new EventResponseDTO();
        first.setId(1L);
        first.setStatus("PENDING");

        EventResponseDTO second = new EventResponseDTO();
        second.setId(2L);
        second.setStatus("IN_PROGRESS");

        when(eventService.findAll(null, null)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].status").value("IN_PROGRESS"));

        verify(eventService).findAll(null, null);
    }

    @Test
    void getAllEvents_withTournamentId_shouldReturnFilteredList() throws Exception {
        EventResponseDTO event = new EventResponseDTO();
        event.setId(3L);
        event.setTournamentId(10L);
        event.setStatus("CREATED");

        when(eventService.findAll(10L, null)).thenReturn(List.of(event));

        mockMvc.perform(get("/api/v1/events").param("tournamentId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3))
                .andExpect(jsonPath("$[0].tournamentId").value(10));

        verify(eventService).findAll(10L, null);
    }

    @Test
    void updateEventScore_shouldReturnUpdatedScore() throws Exception {
        EventResponseDTO response = new EventResponseDTO();
        response.setId(7L);
        response.setHomeScore(2);
        response.setAwayScore(1);

        when(eventService.updateScore(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/events/7/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "homeScore": 2,
                                  "awayScore": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.homeScore").value(2))
                .andExpect(jsonPath("$.awayScore").value(1));

        verify(eventService).updateScore(any(), any());
    }

    @Test
    void startEvent_shouldReturnInProgressEvent() throws Exception {
        EventResponseDTO response = new EventResponseDTO();
        response.setId(3L);
        response.setStatus("IN_PROGRESS");

        when(eventService.startEvent(3L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/events/3/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(eventService).startEvent(3L);
    }

    @Test
    void resetEvent_shouldReturnCreatedEvent() throws Exception {
        EventResponseDTO response = new EventResponseDTO();
        response.setId(3L);
        response.setStatus("CREATED");

        when(eventService.resetEvent(3L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/events/3/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.status").value("CREATED"));

        verify(eventService).resetEvent(3L);
    }

    @Test
    void finishEvent_whenInvalidState_shouldReturnConflict() throws Exception {
        when(eventService.finishEvent(3L)).thenThrow(new InvalidStateException("Event must be IN_PROGRESS"));

        mockMvc.perform(post("/api/v1/events/3/end"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Invalid State"))
                .andExpect(jsonPath("$.message").value("Event must be IN_PROGRESS"));
    }

    @Test
    void reopenEvent_shouldReturnEvent() throws Exception {
        EventResponseDTO response = new EventResponseDTO();
        response.setId(3L);
        response.setStatus("IN_PROGRESS");

        when(eventService.reopenEvent(3L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/events/3/reopen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(eventService).reopenEvent(3L);
    }
}
