package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.response.MarketResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.OutcomeResponseDTO;
import com.franciscomaath.resenhaapi.controller.exception.GlobalExceptionHandler;
import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.service.MarketService;
import com.franciscomaath.resenhaapi.testsupport.MultiGroupFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MarketControllerTest {

    @Mock
    private MarketService marketService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    @Mock
    private CurrentUserContext currentUserContext;

    @InjectMocks
    private MarketController marketController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(marketController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getMarketByEvent_shouldReturnMarketWithOutcomes() throws Exception {
        MarketResponseDTO response = new MarketResponseDTO();
        response.setId(10L);
        response.setEventId(1L);
        response.setName("Resultado Final");
        response.setStatus("OPEN");

        OutcomeResponseDTO outcome1 = new OutcomeResponseDTO();
        outcome1.setId(100L);
        outcome1.setName("Jogador Casa");
        outcome1.setOdd(new BigDecimal("2.10"));

        OutcomeResponseDTO outcome2 = new OutcomeResponseDTO();
        outcome2.setId(101L);
        outcome2.setName("Empate");
        outcome2.setOdd(new BigDecimal("3.20"));

        OutcomeResponseDTO outcome3 = new OutcomeResponseDTO();
        outcome3.setId(102L);
        outcome3.setName("Jogador Fora");
        outcome3.setOdd(new BigDecimal("3.50"));

        response.setOutcomes(List.of(outcome1, outcome2, outcome3));

        when(marketService.findAllByEventId(1L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/markets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].eventId").value(1))
                .andExpect(jsonPath("$[0].name").value("Resultado Final"))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].outcomes").isArray())
                .andExpect(jsonPath("$[0].outcomes.length()").value(3))
                .andExpect(jsonPath("$[0].outcomes[0].name").value("Jogador Casa"))
                .andExpect(jsonPath("$[0].outcomes[0].odd").value(2.10))
                .andExpect(jsonPath("$[0].outcomes[1].name").value("Empate"))
                .andExpect(jsonPath("$[0].outcomes[2].name").value("Jogador Fora"));
    }

    @Test
    void getMarketByEvent_whenNotFound_shouldReturnEmptyList() throws Exception {
        when(marketService.findAllByEventId(999L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/markets/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void setMarketStatus_open_shouldReturn200() throws Exception {
        doNothing().when(marketService).openMarket(1L);
        when(eventRepository.findById(1L)).thenReturn(java.util.Optional.of(event(TournamentType.FIFA_MATCH)));

        MarketResponseDTO response = new MarketResponseDTO();
        response.setId(10L);
        response.setEventId(1L);
        response.setStatus("OPEN");

        when(marketService.findAllByEventId(1L)).thenReturn(List.of(response));

        mockMvc.perform(post("/api/v1/markets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"OPEN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OPEN"));

        verify(marketService).openMarket(1L);
        verify(marketService).findAllByEventId(1L);
    }

    @Test
    void setMarketStatus_close_shouldReturn200() throws Exception {
        doNothing().when(marketService).closeMarket(1L);
        when(eventRepository.findById(1L)).thenReturn(java.util.Optional.of(event(TournamentType.FIFA_MATCH)));

        MarketResponseDTO response = new MarketResponseDTO();
        response.setId(10L);
        response.setEventId(1L);
        response.setStatus("CLOSED");

        when(marketService.findAllByEventId(1L)).thenReturn(List.of(response));

        mockMvc.perform(post("/api/v1/markets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("CLOSED"));

        verify(marketService).closeMarket(1L);
        verify(marketService).findAllByEventId(1L);
    }

    @Test
    void setMarketStatus_invalidStatus_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/v1/markets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"INVALID\"}"))
                .andExpect(status().isBadRequest());

        verify(marketService, never()).openMarket(any());
        verify(marketService, never()).closeMarket(any());
    }

    private Event event(TournamentType type) {
        Event event = new Event();
        event.setTournament(MultiGroupFixtures.tournament(99L, type));
        return event;
    }
}
