package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.response.PlayerResponseDTO;
import com.franciscomaath.resenhaapi.controller.exception.GlobalExceptionHandler;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.service.PlayerService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlayerControllerTest {

    @Mock
    private PlayerService playerService;

    @InjectMocks
    private PlayerController playerController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(playerController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void createPlayer_shouldReturnPlayer() throws Exception {
        PlayerResponseDTO response = new PlayerResponseDTO();
        response.setId(1L);
        response.setName("Francisco");
        response.setActive(true);

        when(playerService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Francisco"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Francisco"))
                .andExpect(jsonPath("$.active").value(true));

        verify(playerService).create(any());
    }

    @Test
    void createPlayer_withoutName_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Input"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void getAllPlayers_shouldReturnList() throws Exception {
        PlayerResponseDTO first = new PlayerResponseDTO();
        first.setId(1L);
        first.setName("One");
        first.setActive(true);

        PlayerResponseDTO second = new PlayerResponseDTO();
        second.setId(2L);
        second.setName("Two");
        second.setActive(false);

        when(playerService.findAll()).thenReturn(List.of(first, second));

        mockMvc.perform(get("/api/v1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("One"))
                .andExpect(jsonPath("$[1].active").value(false));

        verify(playerService).findAll();
    }

    @Test
    void getPlayer_shouldReturnPlayerById() throws Exception {
        PlayerResponseDTO response = new PlayerResponseDTO();
        response.setId(15L);
        response.setName("Target");
        response.setActive(true);

        when(playerService.findPlayerById(15L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/players/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.name").value("Target"));

        verify(playerService).findPlayerById(15L);
    }

    @Test
    void getPlayer_whenNotFound_shouldReturn404() throws Exception {
        when(playerService.findPlayerById(404L)).thenThrow(new ResourceNotFoundException("Player not found"));

        mockMvc.perform(get("/api/v1/players/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource Not Found"))
                .andExpect(jsonPath("$.message").value("Player not found"));
    }

    @Test
    void updatePlayer_shouldReturnUpdatedPlayer() throws Exception {
        PlayerResponseDTO response = new PlayerResponseDTO();
        response.setId(2L);
        response.setName("Updated Name");
        response.setActive(false);

        when(playerService.update(any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/players/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Name",
                                  "active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Updated Name"))
                .andExpect(jsonPath("$.active").value(false));

        verify(playerService).update(any(), any());
    }
}

