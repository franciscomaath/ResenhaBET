package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.response.CompetitionResponseDTO;
import com.franciscomaath.resenhaapi.controller.exception.GlobalExceptionHandler;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.service.CompetitionService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CompetitionControllerTest {

    @Mock
    private CompetitionService competitionService;

    @InjectMocks
    private CompetitionController competitionController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(competitionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createCompetition_shouldReturnCompetition() throws Exception {
        CompetitionResponseDTO response = new CompetitionResponseDTO();
        response.setId(1L);
        response.setName("Copa do Mundo");

        when(competitionService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/competitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Copa do Mundo",
                                  "season": "2026",
                                  "apiFootballLeagueId": "28",
                                  "apiFootballCountryId": "8",
                                  "gameForecastLeagueId": "149",
                                  "startDate": "2026-06-01T00:00:00",
                                  "endDate": "2026-07-20T23:59:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Copa do Mundo"));
    }

    @Test
    void createCompetition_withoutName_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/competitions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "season": "2026",
                                  "apiFootballLeagueId": "28",
                                  "apiFootballCountryId": "8",
                                  "gameForecastLeagueId": "149",
                                  "startDate": "2026-06-01T00:00:00",
                                  "endDate": "2026-07-20T23:59:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Input"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void getAllCompetitions_shouldReturnList() throws Exception {
        CompetitionResponseDTO competition = new CompetitionResponseDTO();
        competition.setId(1L);
        competition.setName("Copa do Mundo");

        when(competitionService.findAll(true)).thenReturn(List.of(competition));

        mockMvc.perform(get("/api/v1/competitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Copa do Mundo"));
    }

    @Test
    void getCompetition_shouldReturnCompetition() throws Exception {
        CompetitionResponseDTO response = new CompetitionResponseDTO();
        response.setId(5L);
        response.setName("World Cup");

        when(competitionService.findById(5L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/competitions/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("World Cup"));
    }

    @Test
    void getCompetition_whenNotFound_shouldReturn404() throws Exception {
        when(competitionService.findById(999L)).thenThrow(new ResourceNotFoundException("Competition", "id", 999L));

        mockMvc.perform(get("/api/v1/competitions/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void toggleCompetitionActive_shouldReturnToggledCompetition() throws Exception {
        CompetitionResponseDTO response = new CompetitionResponseDTO();
        response.setId(1L);
        response.setActive(true);

        when(competitionService.toggleActive(1L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/competitions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.active").value(true));
    }
}
