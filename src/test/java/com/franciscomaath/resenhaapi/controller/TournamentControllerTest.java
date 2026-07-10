package com.franciscomaath.resenhaapi.controller;

import com.franciscomaath.resenhaapi.controller.dto.request.StartTournamentRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.OddsImportResult;
import com.franciscomaath.resenhaapi.controller.dto.response.SyncResult;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentPlayerResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentPlayersResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentRoundResponseDTO;
import com.franciscomaath.resenhaapi.controller.exception.GlobalExceptionHandler;
import com.franciscomaath.resenhaapi.service.FixtureSyncService;
import com.franciscomaath.resenhaapi.service.OddsImportService;
import com.franciscomaath.resenhaapi.service.validator.TournamentGroupConfigValidator;
import com.franciscomaath.resenhaapi.service.TournamentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TournamentControllerTest {

    @Mock
    private TournamentService tournamentService;

    @Mock
    private TournamentGroupConfigValidator tournamentGroupConfigValidator;

    @Mock
    private FixtureSyncService fixtureSyncService;

    @Mock
    private OddsImportService oddsImportService;

    @InjectMocks
    private TournamentController tournamentController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(tournamentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void createTournament_shouldReturnTournament() throws Exception {
        TournamentResponseDTO response = new TournamentResponseDTO();
        response.setId(1L);
        response.setName("Championship");
        response.setStatus("PENDING");
        response.setFormat("LEAGUE");

        when(tournamentService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Championship",
                                  "format": "LEAGUE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Championship"));

        verify(tournamentService).create(any());
    }

    @Test
    void createTournament_withoutName_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Input"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }
    @Test
    void getAllTournaments_shouldReturnPage() throws Exception {
        TournamentResponseDTO first = new TournamentResponseDTO();
        first.setId(1L);
        first.setName("A");

        TournamentResponseDTO second = new TournamentResponseDTO();
        second.setId(2L);
        second.setName("B");

        // Add this:
        Page<TournamentResponseDTO> tournamentPage = new PageImpl<>(
                List.of(first, second),
                PageRequest.of(0, 10), // A real PageRequest instead of Unpaged
                2                      // Total elements
        );

        when(tournamentService.findAll(any(Pageable.class))).thenReturn(tournamentPage);

        mockMvc.perform(get("/api/v1/tournaments")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.content[0].name").value("A"))
                .andExpect(jsonPath("$.content[1].name").value("B"))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(tournamentService).findAll(any(Pageable.class));
    }

    @Test
    void getTournamentPlayers_shouldReturnList() throws Exception {
        TournamentPlayerResponseDTO player = new TournamentPlayerResponseDTO();
        player.setTournamentPlayerId(50L);
        player.setTournamentId(1L);
        player.setPlayerId(10L);
        player.setPlayerName("Francisco");
        player.setTeamId(3L);
        player.setTeamName("Falcons");

        TournamentPlayersResponseDTO response = new TournamentPlayersResponseDTO();
        response.setPlayerCount(1);
        response.setPlayers(List.of(player));

        when(tournamentService.findPlayersByTournamentId(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/tournaments/1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerCount").value(1))
                .andExpect(jsonPath("$.players[0].playerId").value(10))
                .andExpect(jsonPath("$.players[0].teamName").value("Falcons"));

        verify(tournamentService).findPlayersByTournamentId(1L);
    }

    @Test
    void patchTournamentPlayerTeam_shouldReturnUpdatedPlayerTeam() throws Exception {
        TournamentPlayerResponseDTO response = new TournamentPlayerResponseDTO();
        response.setTournamentId(1L);
        response.setPlayerId(10L);
        response.setTeamId(7L);
        response.setTeamName("Sharks");

        when(tournamentService.updateTournamentPlayerTeam(any(), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/tournaments/1/players/10/team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "teamId": 7
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value(10))
                .andExpect(jsonPath("$.teamId").value(7));

        verify(tournamentService).updateTournamentPlayerTeam(any(), any(), any());
    }

    @Test
    void patchTournamentPlayerTeam_withoutTeamId_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/tournaments/1/players/10/team")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Input"))
                .andExpect(jsonPath("$.fieldErrors.teamId").exists());
    }

    @Test
    void getTournamentRounds_shouldReturnList() throws Exception {
        TournamentRoundResponseDTO round = new TournamentRoundResponseDTO();
        round.setName("Round 1");
        round.setMultiplier(new BigDecimal("1.00"));
        round.setRoundOrder(1);

        when(tournamentService.findRoundsByTournamentId(1L)).thenReturn(List.of(round));

        mockMvc.perform(get("/api/v1/tournaments/1/rounds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Round 1"))
                .andExpect(jsonPath("$[0].roundOrder").value(1));

        verify(tournamentService).findRoundsByTournamentId(1L);
    }

    @Test
    void addPlayerToTournament_shouldReturnTournamentPlayer() throws Exception {
        TournamentPlayerResponseDTO response = new TournamentPlayerResponseDTO();
        response.setTournamentId(1L);
        response.setPlayerId(99L);
        response.setPlayerName("New Player");

        when(tournamentService.addPlayerToTournament(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/tournaments/1/players")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "playerId": 99
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tournamentId").value(1))
                .andExpect(jsonPath("$.playerId").value(99));

        verify(tournamentService).addPlayerToTournament(any(), any());
    }

    @Test
    void startTournament_shouldReturnStartedTournament() throws Exception {
        TournamentResponseDTO response = new TournamentResponseDTO();
        response.setId(1L);
        response.setStatus("IN_PROGRESS");

        when(tournamentService.startTournament(1L, null)).thenReturn(response);

        mockMvc.perform(post("/api/v1/tournaments/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        verify(tournamentService).startTournament(1L, null);
    }

    @Test
    void syncFixtures_shouldReturnSyncResult() throws Exception {
        SyncResult syncResult = new SyncResult();
        syncResult.setEventsCreated(10);

        when(fixtureSyncService.sync(1L)).thenReturn(syncResult);

        mockMvc.perform(post("/api/v1/tournaments/1/sync-fixtures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventsCreated").value(10));

        verify(fixtureSyncService).sync(1L);
    }

    @Test
    void syncOdds_shouldReturnOddsImportResult() throws Exception {
        OddsImportResult oddsResult = new OddsImportResult();
        oddsResult.setMarketsCreated(6);

        when(oddsImportService.importForTournament(1L)).thenReturn(oddsResult);

        mockMvc.perform(post("/api/v1/tournaments/1/sync-odds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketsCreated").value(6));
    }

    @Test
    void advanceToBracket_shouldReturnTournament() throws Exception {
        TournamentResponseDTO response = new TournamentResponseDTO();
        response.setId(1L);
        response.setStatus("IN_PROGRESS");

        when(tournamentService.advanceToBracket(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/tournaments/1/advance-to-bracket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void forceAdvanceToBracket_shouldReturnTournament() throws Exception {
        TournamentResponseDTO response = new TournamentResponseDTO();
        response.setId(1L);
        response.setStatus("IN_PROGRESS");

        when(tournamentService.forceAdvanceToBracket(1L)).thenReturn(response);

        mockMvc.perform(post("/api/v1/tournaments/1/force-advance-to-bracket"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}

