package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.response.TeamResponseDTO;
import com.franciscomaath.resenhaapi.controller.exception.GlobalExceptionHandler;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.service.TeamService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TeamControllerTest {

    @Mock
    private TeamService teamService;

    @InjectMocks
    private TeamController teamController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(teamController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createTeam_shouldReturnCreatedTeam() throws Exception {
        TeamResponseDTO response = new TeamResponseDTO();
        response.setId(1L);
        response.setName("Falcons");
        response.setAbbreviation("FLC");
        response.setBadgeUrl("http://badge");

        when(teamService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Falcons",
                                  "abbreviation": "FLC",
                                  "badgeUrl": "http://badge"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Falcons"))
                .andExpect(jsonPath("$.abbreviation").value("FLC"));

        verify(teamService).create(any());
    }

    @Test
    void createTeam_withInvalidBody_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "abbreviation": "ABCDE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Input"))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.abbreviation").exists());
    }

    @Test
    void getAllTeams_shouldReturnList() throws Exception {
        TeamResponseDTO first = new TeamResponseDTO();
        first.setId(1L);
        first.setName("Falcons");
        first.setAbbreviation("FLC");

        TeamResponseDTO second = new TeamResponseDTO();
        second.setId(2L);
        second.setName("Sharks");
        second.setAbbreviation("SHK");

        when(teamService.findAll()).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/v1/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].name").value("Sharks"));

        verify(teamService).findAll();
    }

    @Test
    void getTeamById_shouldReturnTeam() throws Exception {
        TeamResponseDTO response = new TeamResponseDTO();
        response.setId(10L);
        response.setName("Wolves");
        response.setAbbreviation("WLV");

        when(teamService.findById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/teams/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.name").value("Wolves"));

        verify(teamService).findById(10L);
    }

    @Test
    void getTeamById_whenNotFound_shouldReturn404() throws Exception {
        when(teamService.findById(999L)).thenThrow(new ResourceNotFoundException("Team not found"));

        mockMvc.perform(get("/api/v1/teams/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"))
                .andExpect(jsonPath("$.message").value("Team not found"));
    }

    @Test
    void updateGameForecastTeamId_shouldReturnUpdatedTeam() throws Exception {
        TeamResponseDTO response = new TeamResponseDTO();
        response.setId(1L);
        response.setName("Brazil");
        response.setGameForecastTeamId("gf-123");

        when(teamService.updateGameForecastTeamId(1L, "gf-123")).thenReturn(response);

        mockMvc.perform(patch("/api/v1/teams/1/game-forecast-id")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gameForecastTeamId": "gf-123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.gameForecastTeamId").value("gf-123"));
    }
}
