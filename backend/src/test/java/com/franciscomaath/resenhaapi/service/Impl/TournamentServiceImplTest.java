package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.controller.dto.request.PatchTournamentPlayerTeamRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.StartTournamentRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.TournamentPlayerRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.TournamentRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentPlayerResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentPlayersResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentRoundResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentScoreboardResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.GroupStandingsDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TeamStatsResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Competition;
import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.entity.Team;
import com.franciscomaath.resenhaapi.domain.entity.Tournament;
import com.franciscomaath.resenhaapi.domain.entity.TournamentPlayer;
import com.franciscomaath.resenhaapi.domain.entity.TournamentRound;
import com.franciscomaath.resenhaapi.domain.entity.BetSlip;
import com.franciscomaath.resenhaapi.domain.entity.Transaction;
import com.franciscomaath.resenhaapi.domain.entity.GroupTournament;
import com.franciscomaath.resenhaapi.domain.repository.GroupTournamentRepository;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.enums.GenerationMode;
import com.franciscomaath.resenhaapi.domain.enums.PhaseType;
import com.franciscomaath.resenhaapi.domain.enums.TournamentFormat;
import com.franciscomaath.resenhaapi.domain.enums.TournamentStatus;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import com.franciscomaath.resenhaapi.domain.exception.BusinessException;
import com.franciscomaath.resenhaapi.domain.exception.InvalidStateException;
import com.franciscomaath.resenhaapi.domain.repository.CompetitionRepository;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.RoundRepository;
import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.config.OddsProperties;
import com.franciscomaath.resenhaapi.service.BetService;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.service.OddsCalculatorService;
import com.franciscomaath.resenhaapi.service.TournamentWalletProvisioningService;
import com.franciscomaath.resenhaapi.service.validator.TournamentGroupConfigValidator;
import com.franciscomaath.resenhaapi.domain.repository.MarketRepository;
import com.franciscomaath.resenhaapi.domain.repository.OutcomeRepository;
import com.franciscomaath.resenhaapi.service.dto.OddsResult;
import com.franciscomaath.resenhaapi.testsupport.MultiGroupFixtures;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.franciscomaath.resenhaapi.domain.repository.BetSlipRepository;
import com.franciscomaath.resenhaapi.domain.repository.TransactionRepository;
import com.franciscomaath.resenhaapi.mapper.TournamentMapper;
import com.franciscomaath.resenhaapi.domain.repository.PlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.TeamRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentPlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentServiceImplTest {
    @Mock
    private RoundRepository roundRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private OddsCalculatorService oddsCalculatorService;

    @Mock
    private MarketRepository marketRepository;

    @Mock
    private OutcomeRepository outcomeRepository;

    @Mock
    private OddsProperties oddsProperties;

    @Mock
    private BetService betService;

    @Mock
    private TournamentGroupConfigValidator tournamentGroupConfigValidator;

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private GroupTournamentRepository groupTournamentRepository;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    @Mock
    private TournamentWalletProvisioningService tournamentWalletProvisioningService;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TournamentPlayerRepository tournamentPlayerRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentMapper tournamentMapper;

    @Mock
    private BetSlipRepository betSlipRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TournamentServiceImpl tournamentService;

    @Test
    void create_shouldPersistTournamentWithDefaultTypeFormatAndStatus() {
        TournamentRequestDTO request = new TournamentRequestDTO();
        request.setName("Championship");
        request.setFormat(TournamentFormat.LEAGUE);

        Tournament saved = Tournament.builder()
                .id(1L)
                .name("Championship")
                .type(TournamentType.FIFA_MATCH)
                .format(TournamentFormat.LEAGUE)
                .status(TournamentStatus.CREATED)
                .build();

        TournamentResponseDTO response = new TournamentResponseDTO();
        response.setId(1L);
        response.setName("Championship");

        when(tournamentRepository.save(any(Tournament.class))).thenReturn(saved);
        when(tournamentMapper.toResponse(any(Tournament.class), any(), any())).thenReturn(response);

        TournamentResponseDTO result = tournamentService.create(request);

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());
        Tournament persisted = captor.getValue();

        assertEquals(TournamentType.FIFA_MATCH, persisted.getType());
        assertEquals(TournamentFormat.LEAGUE, persisted.getFormat());
        assertEquals(TournamentStatus.CREATED, persisted.getStatus());
        assertEquals(1L, result.getId());
    }

    @Test
    void create_realFootballWithoutCompetitionId_throwsBusinessException() {
        TournamentRequestDTO request = new TournamentRequestDTO();
        request.setName("World Cup");
        request.setType(TournamentType.REAL_FOOTBALL);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tournamentService.create(request));
        assertTrue(ex.getMessage().contains("competitionId"));
        verify(tournamentRepository, never()).save(any());
    }

    @Test
    void create_realFootballWithCompetitionId_persistsWithTypeAndCompetition() {
        Competition competition = new Competition();
        competition.setId(1L);
        competition.setName("Copa do Mundo");

        TournamentRequestDTO request = new TournamentRequestDTO();
        request.setName("World Cup");
        request.setType(TournamentType.REAL_FOOTBALL);
        request.setCompetitionId(1L);

        Tournament saved = Tournament.builder()
                .id(1L)
                .name("Copa do Mundo")
                .type(TournamentType.REAL_FOOTBALL)
                .competition(competition)
                .build();

        TournamentResponseDTO response = new TournamentResponseDTO();
        response.setId(1L);
        response.setName("Copa do Mundo");

        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(tournamentRepository.save(any(Tournament.class))).thenReturn(saved);
        when(tournamentMapper.toResponse(any(Tournament.class), any(), any())).thenReturn(response);

        TournamentResponseDTO result = tournamentService.create(request);

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());
        Tournament persisted = captor.getValue();

        assertEquals(TournamentType.REAL_FOOTBALL, persisted.getType());
        assertEquals(competition, persisted.getCompetition());
        assertEquals("Copa do Mundo", result.getName());
    }

    @Test
    void create_fifaMatchWithCompetitionId_throwsBusinessException() {
        TournamentRequestDTO request = new TournamentRequestDTO();
        request.setName("League");
        request.setType(TournamentType.FIFA_MATCH);
        request.setCompetitionId(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tournamentService.create(request));
        assertTrue(ex.getMessage().contains("competitionId"));
        verify(tournamentRepository, never()).save(any());
    }

    @Test
    void create_nullType_defaultsToFifaMatch() {
        TournamentRequestDTO request = new TournamentRequestDTO();
        request.setName("Default");

        Tournament saved = Tournament.builder()
                .id(1L)
                .name("Default")
                .type(TournamentType.FIFA_MATCH)
                .build();

        TournamentResponseDTO response = new TournamentResponseDTO();
        response.setId(1L);
        response.setName("Default");

        when(tournamentRepository.save(any(Tournament.class))).thenReturn(saved);
        when(tournamentMapper.toResponse(any(Tournament.class), any(), any())).thenReturn(response);

        TournamentResponseDTO result = tournamentService.create(request);

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());
        assertEquals(TournamentType.FIFA_MATCH, captor.getValue().getType());
        assertEquals(1L, result.getId());
    }

    @Test
    void findAll_setsActiveGroupTournamentIdOnResponses() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("Cup")
                .type(TournamentType.FIFA_MATCH)
                .build();
        var group = MultiGroupFixtures.group(10L, "Group");
        var groupTournament = MultiGroupFixtures.groupTournament(100L, group, tournament);
        TournamentResponseDTO response = new TournamentResponseDTO();
        response.setId(1L);

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(tournamentRepository.findAllByGroupId(10L, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(tournament), PageRequest.of(0, 20), 1));
        when(tournamentMapper.toResponse(any(Tournament.class), any())).thenAnswer(invocation -> {
            TournamentResponseDTO dto = new TournamentResponseDTO();
            dto.setId(((Tournament) invocation.getArgument(0)).getId());
            dto.setGroupTournamentId((Long) invocation.getArgument(1));
            return dto;
        });
        when(groupTournamentRepository.findByTournamentIdAndGroupId(1L, 10L)).thenReturn(Optional.of(groupTournament));

        var result = tournamentService.findAll(PageRequest.of(0, 20));

        assertEquals(1, result.getTotalElements());
        assertEquals(100L, result.getContent().get(0).getGroupTournamentId());
    }

    @Test
    void addPlayerToTournament_shouldAddWhenPlayerNotYetRegistered() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("Cup")
                .status(TournamentStatus.CREATED)
                .build();
        Player player = Player.builder().id(9L).name("Player 9").build();

        TournamentPlayerRequestDTO request = new TournamentPlayerRequestDTO();
        request.setPlayerId(9L);

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(playerRepository.findByIdAndGroupId(9L, 10L)).thenReturn(Optional.of(player));
        when(tournamentPlayerRepository.existsByTournamentIdAndPlayerId(1L, 9L)).thenReturn(false);

        TournamentPlayerResponseDTO result = tournamentService.addPlayerToTournament(1L, request);

        ArgumentCaptor<TournamentPlayer> captor = ArgumentCaptor.forClass(TournamentPlayer.class);
        verify(tournamentPlayerRepository).save(captor.capture());
        TournamentPlayer persisted = captor.getValue();

        assertEquals(1L, persisted.getTournament().getId());
        assertEquals(9L, persisted.getPlayer().getId());
        assertEquals(9L, result.getPlayerId());
        assertEquals("Player 9", result.getPlayerName());
    }

    @Test
    void addPlayerToTournament_whenAlreadyRegistered_shouldThrowBusinessException() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("Cup")
                .status(TournamentStatus.CREATED)
                .build();
        Player player = Player.builder().id(9L).name("Player 9").build();

        TournamentPlayerRequestDTO request = new TournamentPlayerRequestDTO();
        request.setPlayerId(9L);

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(playerRepository.findByIdAndGroupId(9L, 10L)).thenReturn(Optional.of(player));
        when(tournamentPlayerRepository.existsByTournamentIdAndPlayerId(1L, 9L)).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tournamentService.addPlayerToTournament(1L, request));

        assertTrue(ex.getMessage().contains("already in this Tournament"));
        verify(tournamentPlayerRepository, never()).save(any(TournamentPlayer.class));
    }

    @Test
    void startTournament_shouldGenerateRoundsAndChangeStatusWhenValid() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("League")
                .format(TournamentFormat.LEAGUE)
                .status(TournamentStatus.CREATED)
                .build();

        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).build(),
                TournamentPlayer.builder().id(2L).build(),
                TournamentPlayer.builder().id(3L).build()
        );

        TournamentResponseDTO dto = new TournamentResponseDTO();
        dto.setId(1L);
        dto.setStatus("IN_PROGRESS");

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentMapper.toResponse(any(Tournament.class), any())).thenReturn(dto);

        TournamentResponseDTO result = tournamentService.startTournament(1L, new StartTournamentRequestDTO());

        assertEquals("IN_PROGRESS", result.getStatus());

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());
        Tournament saved = captor.getValue();

        assertEquals(TournamentStatus.IN_PROGRESS, saved.getStatus());
        assertEquals(6, saved.getRounds().size());
        assertEquals("Rodada 1", saved.getRounds().get(0).getName());
        assertEquals(BigDecimal.ONE, saved.getRounds().get(0).getMultiplier());
    }

    @Test
    void startTournament_leagueWithAutoGeneration_shouldGenerateRoundRobinEvents() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("League Auto")
                .format(TournamentFormat.LEAGUE)
                .status(TournamentStatus.CREATED)
                .generationMode(GenerationMode.AUTO)
                .build();

        Player p1 = Player.builder().id(1L).name("A").currentElo(BigDecimal.valueOf(1000)).build();
        Player p2 = Player.builder().id(2L).name("B").currentElo(BigDecimal.valueOf(1000)).build();
        Player p3 = Player.builder().id(3L).name("C").currentElo(BigDecimal.valueOf(1000)).build();
        Player p4 = Player.builder().id(4L).name("D").currentElo(BigDecimal.valueOf(1000)).build();

        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).player(p1).tournament(tournament).build(),
                TournamentPlayer.builder().id(2L).player(p2).tournament(tournament).build(),
                TournamentPlayer.builder().id(3L).player(p3).tournament(tournament).build(),
                TournamentPlayer.builder().id(4L).player(p4).tournament(tournament).build()
        );

        TournamentResponseDTO dto = new TournamentResponseDTO();
        dto.setId(1L);
        dto.setStatus("IN_PROGRESS");

        when(oddsProperties.getH2hMatchLimit()).thenReturn(5);
        when(oddsCalculatorService.calculate(any(), any(), any())).thenReturn(
                new OddsResult(BigDecimal.valueOf(2.0), BigDecimal.valueOf(3.0), BigDecimal.valueOf(2.0)));
        when(marketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outcomeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(100L);
            return e;
        });
        when(tournamentMapper.toResponse(any(Tournament.class), any())).thenReturn(dto);

        TournamentResponseDTO result = tournamentService.startTournament(1L, new StartTournamentRequestDTO());

        assertEquals("IN_PROGRESS", result.getStatus());
        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository, times(1)).save(captor.capture());
        Tournament saved = captor.getValue();
        assertEquals(6, saved.getRounds().size());
        verify(eventRepository, atLeast(12)).save(any(Event.class));
    }

    @Test
    void startTournament_leagueWithOddNumberOfPlayers_shouldCalculateEffectiveN() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("League Odd")
                .format(TournamentFormat.LEAGUE)
                .status(TournamentStatus.CREATED)
                .generationMode(GenerationMode.MANUAL)
                .build();

        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).build(),
                TournamentPlayer.builder().id(2L).build(),
                TournamentPlayer.builder().id(3L).build()
        );

        TournamentResponseDTO dto = new TournamentResponseDTO();
        dto.setId(1L);
        dto.setStatus("IN_PROGRESS");

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentMapper.toResponse(any(Tournament.class), any())).thenReturn(dto);

        TournamentResponseDTO result = tournamentService.startTournament(1L, new StartTournamentRequestDTO());

        assertEquals("IN_PROGRESS", result.getStatus());
        ArgumentCaptor<Tournament> captor =ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());
        Tournament saved = captor.getValue();
        // 3 players -> effectiveN = 4 -> (4-1)*2 = 6 rounds
        assertEquals(6, saved.getRounds().size());
        assertEquals("Rodada 1", saved.getRounds().get(0).getName());
        assertEquals("Rodada 6", saved.getRounds().get(5).getName());
    }

    @Test
    void startTournament_withLessThanTwoPlayers_shouldThrowBusinessException() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .format(TournamentFormat.LEAGUE)
                .status(TournamentStatus.CREATED)
                .build();

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(List.of(TournamentPlayer.builder().id(1L).build()));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tournamentService.startTournament(1L, new StartTournamentRequestDTO()));

        assertTrue(ex.getMessage().contains("Mínimo de 2 participantes"));
        verify(tournamentRepository, never()).save(any(Tournament.class));
    }

    @Test
    void startTournament_whenAlreadyStarted_shouldThrowBusinessException() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .status(TournamentStatus.IN_PROGRESS)
                .build();

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tournamentService.startTournament(1L, new StartTournamentRequestDTO()));

        assertTrue(ex.getMessage().contains("already started"));
    }

    @Test
    void findPlayersByTournamentId_shouldReturnMappedPlayersWithTeamData() {
        Tournament tournament = Tournament.builder().id(1L).build();
        Player player = Player.builder().id(11L).name("Player 11").build();
        Team team = Team.builder().id(5L).name("Falcons").abbreviation("FLC").build();

        TournamentPlayer tp = TournamentPlayer.builder()
                .id(90L)
                .tournament(tournament)
                .player(player)
                .team(team)
                .build();

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(List.of(tp));

        TournamentPlayersResponseDTO result = tournamentService.findPlayersByTournamentId(1L);

        assertEquals(1, result.getPlayerCount());
        assertEquals(1, result.getPlayers().size());
        assertEquals(11L, result.getPlayers().get(0).getPlayerId());
        assertEquals(5L, result.getPlayers().get(0).getTeamId());
        assertEquals("Falcons", result.getPlayers().get(0).getTeamName());
    }

    @Test
    void updateTournamentPlayerTeam_shouldSetTeamAndReturnUpdatedResponse() {
        Tournament tournament = Tournament.builder().id(1L).build();
        Player player = Player.builder().id(10L).name("Player 10").build();
        Team team = Team.builder().id(7L).name("Sharks").abbreviation("SHK").build();

        TournamentPlayer tournamentPlayer = TournamentPlayer.builder()
                .id(200L)
                .tournament(tournament)
                .player(player)
                .team(null)
                .build();

        PatchTournamentPlayerTeamRequestDTO request = new PatchTournamentPlayerTeamRequestDTO();
        request.setTeamId(7L);

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(teamRepository.findById(7L)).thenReturn(Optional.of(team));
        when(tournamentPlayerRepository.findByTournamentIdAndPlayerId(1L, 10L)).thenReturn(Optional.of(tournamentPlayer));
        when(tournamentPlayerRepository.save(tournamentPlayer)).thenReturn(tournamentPlayer);

        TournamentPlayerResponseDTO result = tournamentService.updateTournamentPlayerTeam(1L, 10L, request);

        assertEquals(7L, tournamentPlayer.getTeam().getId());
        assertEquals(10L, result.getPlayerId());
        assertEquals(7L, result.getTeamId());
        assertEquals("Sharks", result.getTeamName());
    }

    @Test
    void findRoundsByTournamentId_shouldReturnMappedRoundDtos() {
        Tournament tournament = Tournament.builder().id(1L).build();
        TournamentRound round = TournamentRound.builder()
                .id(1L)
                .name("Rodada 1")
                .multiplier(BigDecimal.ONE)
                .roundOrder(1)
                .tournament(tournament)
                .build();

        TournamentRoundResponseDTO roundDto = new TournamentRoundResponseDTO();
        roundDto.setRoundId(1L);
        roundDto.setName("Rodada 1");
        roundDto.setMultiplier(BigDecimal.ONE);
        roundDto.setRoundOrder(1);

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(roundRepository.findByTournamentIdOrderByRoundOrderAsc(1L)).thenReturn(List.of(round));
        when(tournamentMapper.toRoundResponse(round)).thenReturn(roundDto);

        List<TournamentRoundResponseDTO> result = tournamentService.findRoundsByTournamentId(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getRoundId());
        assertEquals("Rodada 1", result.get(0).getName());
        assertFalse(result.isEmpty());
    }

    // B29 — Bracket generation tests

    @Test
    void startTournament_bracket2Players_shouldCreateFinalOnly() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("Bracket")
                .format(TournamentFormat.BRACKET)
                .status(TournamentStatus.CREATED)
                .generationMode(GenerationMode.MANUAL)
                .build();

        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).build(),
                TournamentPlayer.builder().id(2L).build()
        );

        TournamentResponseDTO dto = new TournamentResponseDTO();
        dto.setId(1L);
        dto.setStatus("IN_PROGRESS");

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(100L);
            return e;
        });
        when(tournamentMapper.toResponse(any(Tournament.class), any())).thenReturn(dto);

        TournamentResponseDTO result = tournamentService.startTournament(1L, new StartTournamentRequestDTO());

        assertEquals("IN_PROGRESS", result.getStatus());
        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository, times(1)).save(captor.capture());
        Tournament saved = captor.getValue();
        assertEquals(1, saved.getRounds().size());
        assertEquals("Final", saved.getRounds().get(0).getName());
    }

    @Test
    void startTournament_bracket4Players_shouldCreateSemiAndFinal() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("Bracket")
                .format(TournamentFormat.BRACKET)
                .status(TournamentStatus.CREATED)
                .generationMode(GenerationMode.MANUAL)
                .build();

        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).build(),
                TournamentPlayer.builder().id(2L).build(),
                TournamentPlayer.builder().id(3L).build(),
                TournamentPlayer.builder().id(4L).build()
        );

        TournamentResponseDTO dto = new TournamentResponseDTO();
        dto.setId(1L);
        dto.setStatus("IN_PROGRESS");

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(100L);
            return e;
        });
        when(tournamentMapper.toResponse(any(Tournament.class), any())).thenReturn(dto);

        TournamentResponseDTO result = tournamentService.startTournament(1L, new StartTournamentRequestDTO());

        assertEquals("IN_PROGRESS", result.getStatus());
        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository, times(1)).save(captor.capture());
        Tournament saved = captor.getValue();
        assertEquals(2, saved.getRounds().size());
        assertEquals("Semifinais", saved.getRounds().get(0).getName());
        assertEquals("Final", saved.getRounds().get(1).getName());
    }

    @Test
    void startTournament_bracket4PlayersWithThirdPlace_shouldInsertThirdPlaceBeforeFinal() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("Bracket")
                .format(TournamentFormat.BRACKET)
                .status(TournamentStatus.CREATED)
                .generationMode(GenerationMode.MANUAL)
                .hasThirdPlaceMatch(true)
                .build();

        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).build(),
                TournamentPlayer.builder().id(2L).build(),
                TournamentPlayer.builder().id(3L).build(),
                TournamentPlayer.builder().id(4L).build()
        );

        TournamentResponseDTO dto = new TournamentResponseDTO();
        dto.setId(1L);
        dto.setStatus("IN_PROGRESS");

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(100L);
            return e;
        });
        when(tournamentMapper.toResponse(any(Tournament.class), any())).thenReturn(dto);

        TournamentResponseDTO result = tournamentService.startTournament(1L, new StartTournamentRequestDTO());

        assertEquals("IN_PROGRESS", result.getStatus());
        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository, times(1)).save(captor.capture());
        Tournament saved = captor.getValue();
        assertEquals(3, saved.getRounds().size());
        assertEquals("Semifinais", saved.getRounds().get(0).getName());
        assertEquals("3rd Place", saved.getRounds().get(1).getName());
        assertEquals("Final", saved.getRounds().get(2).getName());
    }

    @Test
    void startTournament_bracketWithAutoGeneration_shouldGenerateSeededEvents() {
        Player p1 = Player.builder().id(1L).name("A").currentElo(BigDecimal.valueOf(1600)).build();
        Player p2 = Player.builder().id(2L).name("B").currentElo(BigDecimal.valueOf(1500)).build();
        Player p3 = Player.builder().id(3L).name("C").currentElo(BigDecimal.valueOf(1400)).build();
        Player p4 = Player.builder().id(4L).name("D").currentElo(BigDecimal.valueOf(1300)).build();

        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("Bracket Auto")
                .format(TournamentFormat.BRACKET)
                .status(TournamentStatus.CREATED)
                .generationMode(GenerationMode.AUTO)
                .build();

        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).player(p1).build(),
                TournamentPlayer.builder().id(2L).player(p2).build(),
                TournamentPlayer.builder().id(3L).player(p3).build(),
                TournamentPlayer.builder().id(4L).player(p4).build()
        );

        TournamentResponseDTO responseDTO = new TournamentResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setStatus("IN_PROGRESS");

        when(oddsProperties.getH2hMatchLimit()).thenReturn(5);
        when(oddsCalculatorService.calculate(any(), any(), any())).thenReturn(
                new OddsResult(BigDecimal.valueOf(1.5), BigDecimal.valueOf(0), BigDecimal.valueOf(2.5)));
        when(marketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outcomeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(100L);
            return e;
        });
        when(tournamentMapper.toResponse(any(Tournament.class), any())).thenReturn(responseDTO);

        TournamentResponseDTO result = tournamentService.startTournament(1L, new StartTournamentRequestDTO());

        assertEquals("IN_PROGRESS", result.getStatus());
        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository, times(1)).save(captor.capture());
        Tournament savedWithRounds = captor.getValue();
        assertEquals(2, savedWithRounds.getRounds().size());
        assertEquals("Semifinais", savedWithRounds.getRounds().get(0).getName());
        assertEquals("Final", savedWithRounds.getRounds().get(1).getName());

        // Verify seeded matchups: highest elo vs lowest elo
        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, atLeast(3)).save(eventCaptor.capture());
        List<Event> allEvents = eventCaptor.getAllValues();

        boolean hasSemi1 = allEvents.stream().anyMatch(e ->
                e.getPlayerHome() != null && e.getPlayerHome().getId().equals(1L)
                        && e.getPlayerAway() != null && e.getPlayerAway().getId().equals(4L));
        boolean hasSemi2 = allEvents.stream().anyMatch(e ->
                e.getPlayerHome() != null && e.getPlayerHome().getId().equals(2L)
                        && e.getPlayerAway() != null && e.getPlayerAway().getId().equals(3L));
        assertTrue(hasSemi1, "Expected seeding: #1 vs #4");
        assertTrue(hasSemi2, "Expected seeding: #2 vs #3");
    }

    @Test
    void startTournament_bracketWithNonPowerOf2Players_shouldPadToNextPowerOf2() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("Bracket 3")
                .format(TournamentFormat.BRACKET)
                .status(TournamentStatus.CREATED)
                .generationMode(GenerationMode.MANUAL)
                .build();

        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).build(),
                TournamentPlayer.builder().id(2L).build(),
                TournamentPlayer.builder().id(3L).build()
        );

        TournamentResponseDTO dto = new TournamentResponseDTO();
        dto.setId(1L);
        dto.setStatus("IN_PROGRESS");

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(100L);
            return e;
        });
        when(tournamentMapper.toResponse(any(Tournament.class), any())).thenReturn(dto);

        TournamentResponseDTO result = tournamentService.startTournament(1L, new StartTournamentRequestDTO());

        assertEquals("IN_PROGRESS", result.getStatus());
        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository, times(1)).save(captor.capture());
        Tournament saved = captor.getValue();
        // 3 players -> nextPowerOf2 = 4 -> 2 rounds (Semifinais + Final)
        assertEquals(2, saved.getRounds().size());
        assertEquals("Semifinais", saved.getRounds().get(0).getName());
        assertEquals("Final", saved.getRounds().get(1).getName());
    }

    // B31 — advance-to-bracket tests

    @Test
    void advanceToBracket_whenNotAllGroupCompleted_shouldThrowInvalidStateException() {
        TournamentRound groupRound = TournamentRound.builder().id(1L).name("Rodada 1 - Grupo 1").phaseType(PhaseType.GROUP_STAGE).groupNumber(1).build();
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("LB")
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.IN_PROGRESS)
                .numberOfGroups(1)
                .playersAdvancingPerGroup(2)
                .generationMode(GenerationMode.MANUAL)
                .rounds(List.of(groupRound))
                .build();

        Player p1 = Player.builder().id(1L).name("A").currentElo(BigDecimal.valueOf(1000)).build();
        Player p2 = Player.builder().id(2L).name("B").currentElo(BigDecimal.valueOf(1000)).build();
        Player p3 = Player.builder().id(3L).name("C").currentElo(BigDecimal.valueOf(1000)).build();
        Player p4 = Player.builder().id(4L).name("D").currentElo(BigDecimal.valueOf(1000)).build();

        TournamentPlayer tp1 = TournamentPlayer.builder().id(1L).player(p1).tournament(tournament).groupNumber(1).build();
        TournamentPlayer tp2 = TournamentPlayer.builder().id(2L).player(p2).tournament(tournament).groupNumber(1).build();
        TournamentPlayer tp3 = TournamentPlayer.builder().id(3L).player(p3).tournament(tournament).groupNumber(1).build();
        TournamentPlayer tp4 = TournamentPlayer.builder().id(4L).player(p4).tournament(tournament).groupNumber(1).build();

        TournamentRound round = TournamentRound.builder().id(1L).name("Rodada 1 - Grupo 1").phaseType(PhaseType.GROUP_STAGE).groupNumber(1).build();

        Event completedEvent = Event.builder()
                .id(1L).tournament(tournament).round(round).playerHome(p1).playerAway(p2)
                .homeScore(1).awayScore(0).status(EventStatus.COMPLETED).build();
        Event incompleteEvent = Event.builder()
                .id(2L).tournament(tournament).round(round).playerHome(p3).playerAway(p4)
                .homeScore(0).awayScore(0).status(EventStatus.CREATED).build();

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of(completedEvent, incompleteEvent));

        assertThrows(InvalidStateException.class,
                () -> tournamentService.advanceToBracket(1L));
    }

    @Test
    void advanceToBracket_whenKnockoutAlreadyExists_shouldThrowBusinessException() {
        TournamentRound groupRound = TournamentRound.builder().id(1L).name("Rodada 1 - Grupo 1").phaseType(PhaseType.GROUP_STAGE).groupNumber(1).build();
        TournamentRound knockoutRound = TournamentRound.builder().id(10L).name("Final").phaseType(PhaseType.KNOCKOUT).build();
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("LB")
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.IN_PROGRESS)
                .rounds(List.of(groupRound, knockoutRound))
                .build();

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tournamentService.advanceToBracket(1L));
        assertTrue(ex.getMessage().contains("Knockout phase already exists"));
    }

    @Test
    void advanceToBracket_whenNotLeagueBracket_shouldThrowBusinessException() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .format(TournamentFormat.LEAGUE)
                .status(TournamentStatus.IN_PROGRESS)
                .build();

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tournamentService.advanceToBracket(1L));
        assertTrue(ex.getMessage().contains("not LEAGUE_BRACKET"));
    }

    @Test
    void advanceToBracket_whenNotInProgress_shouldThrowBusinessException() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.CREATED)
                .build();

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tournamentService.advanceToBracket(1L));
        assertTrue(ex.getMessage().contains("not in progress"));
    }

    // LEAGUE_BRACKET — startTournament

    @Test
    void startTournament_leagueBracket_shouldCreateGroupStageRounds() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("LB Auto")
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.CREATED)
                .generationMode(GenerationMode.AUTO)
                .build();

        Player p1 = Player.builder().id(1L).name("A").currentElo(BigDecimal.valueOf(1000)).build();
        Player p2 = Player.builder().id(2L).name("B").currentElo(BigDecimal.valueOf(1000)).build();
        Player p3 = Player.builder().id(3L).name("C").currentElo(BigDecimal.valueOf(1000)).build();
        Player p4 = Player.builder().id(4L).name("D").currentElo(BigDecimal.valueOf(1000)).build();

        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).player(p1).tournament(tournament).build(),
                TournamentPlayer.builder().id(2L).player(p2).tournament(tournament).build(),
                TournamentPlayer.builder().id(3L).player(p3).tournament(tournament).build(),
                TournamentPlayer.builder().id(4L).player(p4).tournament(tournament).build()
        );

        StartTournamentRequestDTO dto = new StartTournamentRequestDTO();
        dto.setNumberOfGroups(2);
        dto.setPlayersAdvancingPerGroup(1);

        TournamentResponseDTO responseDTO = new TournamentResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setStatus("IN_PROGRESS");

        when(oddsProperties.getH2hMatchLimit()).thenReturn(5);
        when(oddsCalculatorService.calculate(any(), any(), any())).thenReturn(
                new OddsResult(BigDecimal.valueOf(2.0), BigDecimal.valueOf(3.0), BigDecimal.valueOf(2.0)));
        when(marketRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outcomeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(100L);
            return e;
        });
        when(tournamentMapper.toResponse(any(Tournament.class), any())).thenReturn(responseDTO);

        TournamentResponseDTO result = tournamentService.startTournament(1L, dto);

        assertEquals("IN_PROGRESS", result.getStatus());
        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository, atLeast(1)).save(captor.capture());
        Tournament saved = captor.getAllValues().get(0);
        // 4 players, 2 groups, 2 per group -> effective 2 -> (2-1)*2 = 2 rounds per group -> 4 total
        assertEquals(4, saved.getRounds().size());
        assertTrue(saved.getRounds().stream().allMatch(r -> r.getPhaseType() == PhaseType.GROUP_STAGE));
        assertEquals(2, saved.getNumberOfGroups());
        assertEquals(1, saved.getPlayersAdvancingPerGroup());
        verify(tournamentPlayerRepository).saveAll(any());
    }

    @Test
    void startTournament_leagueBracketWithNullDto_shouldThrowBusinessException() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("LB")
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.CREATED)
                .generationMode(GenerationMode.MANUAL)
                .build();

        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).build(),
                TournamentPlayer.builder().id(2L).build()
        );

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tournamentService.startTournament(1L, null));
        assertTrue(ex.getMessage().contains("group configuration"));
    }

    @Test
    void startTournament_leagueBracketWithInvalidNumberOfGroups_shouldThrowBusinessException() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("LB")
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.CREATED)
                .generationMode(GenerationMode.MANUAL)
                .build();

        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).build(),
                TournamentPlayer.builder().id(2L).build()
        );

        StartTournamentRequestDTO dto = new StartTournamentRequestDTO();
        dto.setNumberOfGroups(0);
        dto.setPlayersAdvancingPerGroup(1);

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tournamentService.startTournament(1L, dto));
        assertTrue(ex.getMessage().contains("numberOfGroups"));
    }

    @Test
    void startTournament_leagueBracketWithInvalidPlayersAdvancingPerGroup_shouldThrowBusinessException() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("LB")
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.CREATED)
                .generationMode(GenerationMode.MANUAL)
                .build();

        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).build(),
                TournamentPlayer.builder().id(2L).build()
        );

        StartTournamentRequestDTO dto = new StartTournamentRequestDTO();
        dto.setNumberOfGroups(1);
        dto.setPlayersAdvancingPerGroup(0);

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tournamentService.startTournament(1L, dto));
        assertTrue(ex.getMessage().contains("playersAdvancingPerGroup"));
    }

    @Test
    void startTournament_leagueBracketWithNotEnoughPlayersPerGroup_shouldThrowBusinessException() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("LB")
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.CREATED)
                .generationMode(GenerationMode.MANUAL)
                .build();

        // 2 players in 2 groups = 1 per group -> not enough
        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).build(),
                TournamentPlayer.builder().id(2L).build()
        );

        StartTournamentRequestDTO dto = new StartTournamentRequestDTO();
        dto.setNumberOfGroups(2);
        dto.setPlayersAdvancingPerGroup(1);

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tournamentService.startTournament(1L, dto));
        assertTrue(ex.getMessage().contains("Not enough players per group"));
    }

    @Test
    void startTournament_leagueBracketWithTotalAdvancingNotPowerOf2_shouldThrowBusinessException() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("LB")
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.CREATED)
                .generationMode(GenerationMode.MANUAL)
                .build();

        // 6 players, 3 groups, 1 per group = 3 total advancing -> not power of 2
        List<TournamentPlayer> players = List.of(
                TournamentPlayer.builder().id(1L).build(),
                TournamentPlayer.builder().id(2L).build(),
                TournamentPlayer.builder().id(3L).build(),
                TournamentPlayer.builder().id(4L).build(),
                TournamentPlayer.builder().id(5L).build(),
                TournamentPlayer.builder().id(6L).build()
        );

        StartTournamentRequestDTO dto = new StartTournamentRequestDTO();
        dto.setNumberOfGroups(3);
        dto.setPlayersAdvancingPerGroup(1);

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(tournamentPlayerRepository.findByTournamentId(1L)).thenReturn(players);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> tournamentService.startTournament(1L, dto));
        assertTrue(ex.getMessage().contains("power of 2"));
    }

    // B31 — advance-to-bracket happy path

    @Test
    void advanceToBracket_whenAllConditionsMet_shouldCreateKnockoutPhase() {
        Player p1 = Player.builder().id(1L).name("A").currentElo(BigDecimal.valueOf(1000)).build();
        Player p2 = Player.builder().id(2L).name("B").currentElo(BigDecimal.valueOf(1000)).build();
        Player p3 = Player.builder().id(3L).name("C").currentElo(BigDecimal.valueOf(1000)).build();
        Player p4 = Player.builder().id(4L).name("D").currentElo(BigDecimal.valueOf(1000)).build();

        TournamentRound groupRound = TournamentRound.builder()
                .id(1L).name("Rodada 1 - Grupo A").phaseType(PhaseType.GROUP_STAGE).groupNumber(1).roundOrder(1).build();

        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("LB")
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.IN_PROGRESS)
                .numberOfGroups(1)
                .playersAdvancingPerGroup(2)
                .generationMode(GenerationMode.MANUAL)
                .rounds(List.of(groupRound))
                .build();

        TournamentPlayer tp1 = TournamentPlayer.builder().id(1L).player(p1).tournament(tournament).groupNumber(1).build();
        TournamentPlayer tp2 = TournamentPlayer.builder().id(2L).player(p2).tournament(tournament).groupNumber(1).build();
        TournamentPlayer tp3 = TournamentPlayer.builder().id(3L).player(p3).tournament(tournament).groupNumber(1).build();
        TournamentPlayer tp4 = TournamentPlayer.builder().id(4L).player(p4).tournament(tournament).groupNumber(1).build();
        tournament.setTournamentPlayers(List.of(tp1, tp2, tp3, tp4));

        TournamentRound round = TournamentRound.builder()
                .id(1L).name("Rodada 1 - Grupo A").phaseType(PhaseType.GROUP_STAGE).groupNumber(1).build();

        Event e1 = Event.builder().id(1L).tournament(tournament).round(round).playerHome(p1).playerAway(p2)
                .homeScore(1).awayScore(0).status(EventStatus.COMPLETED).build();
        Event e2 = Event.builder().id(2L).tournament(tournament).round(round).playerHome(p3).playerAway(p4)
                .homeScore(1).awayScore(0).status(EventStatus.COMPLETED).build();

        TournamentResponseDTO responseDTO = new TournamentResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setStatus("IN_PROGRESS");

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of(e1, e2));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(100L);
            return e;
        });
        when(tournamentMapper.toResponse(any(Tournament.class), any())).thenReturn(responseDTO);

        TournamentResponseDTO result = tournamentService.advanceToBracket(1L);

        assertEquals("IN_PROGRESS", result.getStatus());
        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository, atLeast(1)).save(captor.capture());
        Tournament saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        // Original round (1) + new knockout rounds (1 for 2 players -> Final)
        assertEquals(2, saved.getRounds().size());
        assertEquals(PhaseType.KNOCKOUT, saved.getRounds().get(1).getPhaseType());
        assertEquals("Final", saved.getRounds().get(1).getName());
    }

    // B31 — force-advance-to-bracket

    @Test
    void forceAdvanceToBracket_shouldCancelPendingEventsAndAdvance() {
        Player p1 = Player.builder().id(1L).name("A").currentElo(BigDecimal.valueOf(1000)).build();
        Player p2 = Player.builder().id(2L).name("B").currentElo(BigDecimal.valueOf(1000)).build();
        Player p3 = Player.builder().id(3L).name("C").currentElo(BigDecimal.valueOf(1000)).build();
        Player p4 = Player.builder().id(4L).name("D").currentElo(BigDecimal.valueOf(1000)).build();

        TournamentRound groupRound = TournamentRound.builder()
                .id(1L).name("Rodada 1 - Grupo A").phaseType(PhaseType.GROUP_STAGE).groupNumber(1).roundOrder(1).build();

        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("LB")
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.IN_PROGRESS)
                .numberOfGroups(1)
                .playersAdvancingPerGroup(2)
                .generationMode(GenerationMode.MANUAL)
                .rounds(List.of(groupRound))
                .build();

        TournamentPlayer tp1 = TournamentPlayer.builder().id(1L).player(p1).tournament(tournament).groupNumber(1).build();
        TournamentPlayer tp2 = TournamentPlayer.builder().id(2L).player(p2).tournament(tournament).groupNumber(1).build();
        TournamentPlayer tp3 = TournamentPlayer.builder().id(3L).player(p3).tournament(tournament).groupNumber(1).build();
        TournamentPlayer tp4 = TournamentPlayer.builder().id(4L).player(p4).tournament(tournament).groupNumber(1).build();
        tournament.setTournamentPlayers(List.of(tp1, tp2, tp3, tp4));

        TournamentRound round = TournamentRound.builder()
                .id(1L).name("Rodada 1 - Grupo A").phaseType(PhaseType.GROUP_STAGE).groupNumber(1).build();

        Event completedEvent = Event.builder().id(1L).tournament(tournament).round(round).playerHome(p1).playerAway(p2)
                .homeScore(1).awayScore(0).status(EventStatus.COMPLETED).build();
        Event createdEvent = Event.builder().id(2L).tournament(tournament).round(round).playerHome(p3).playerAway(p4)
                .homeScore(0).awayScore(0).status(EventStatus.CREATED).build();

        TournamentResponseDTO responseDTO = new TournamentResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setStatus("IN_PROGRESS");

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of(completedEvent, createdEvent));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(100L);
            return e;
        });
        when(tournamentMapper.toResponse(any(Tournament.class), any())).thenReturn(responseDTO);

        TournamentResponseDTO result = tournamentService.forceAdvanceToBracket(1L);

        assertEquals("IN_PROGRESS", result.getStatus());
        // Verify the created event was cancelled
        assertEquals(EventStatus.CANCELLED, createdEvent.getStatus());
        verify(eventRepository, atLeast(1)).save(createdEvent);
        // Verify betService.cancelBetsForEvent was called for the cancelled event
        verify(betService).cancelBetsForEvent(createdEvent.getId());
        verify(betService, never()).cancelBetsForEvent(completedEvent.getId());
        // Verify knockout rounds were added
        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository, atLeast(1)).save(captor.capture());
        Tournament saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertTrue(saved.getRounds().stream().anyMatch(r -> r.getPhaseType() == PhaseType.KNOCKOUT));
    }

    @Test
    void forceAdvanceToBracket_whenInProgressEventsExist_shouldThrowInvalidStateException() {
        Player p1 = Player.builder().id(1L).name("A").currentElo(BigDecimal.valueOf(1000)).build();
        Player p2 = Player.builder().id(2L).name("B").currentElo(BigDecimal.valueOf(1000)).build();

        TournamentRound groupRound = TournamentRound.builder()
                .id(1L).name("Rodada 1 - Grupo A").phaseType(PhaseType.GROUP_STAGE).groupNumber(1).build();

        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("LB")
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.IN_PROGRESS)
                .numberOfGroups(1)
                .playersAdvancingPerGroup(1)
                .generationMode(GenerationMode.MANUAL)
                .rounds(List.of(groupRound))
                .build();

        TournamentRound round = TournamentRound.builder()
                .id(1L).name("Rodada 1 - Grupo A").phaseType(PhaseType.GROUP_STAGE).groupNumber(1).build();

        Event inProgressEvent = Event.builder().id(1L).tournament(tournament).round(round).playerHome(p1).playerAway(p2)
                .homeScore(0).awayScore(0).status(EventStatus.IN_PROGRESS).build();

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of(inProgressEvent));

        InvalidStateException ex = assertThrows(InvalidStateException.class,
                () -> tournamentService.forceAdvanceToBracket(1L));
        assertTrue(ex.getMessage().contains("in-progress"));
    }

    @Test
    void getScoreboard_realFootballLeagueBracket_shouldReturnTeamStandings() {
        Team teamA = Team.builder().id(1L).name("Brazil").abbreviation("BRA").build();
        Team teamB = Team.builder().id(2L).name("Argentina").abbreviation("ARG").build();
        Team teamC = Team.builder().id(3L).name("Uruguay").abbreviation("URU").build();
        Team teamD = Team.builder().id(4L).name("Colombia").abbreviation("COL").build();

        TournamentRound groupARound = TournamentRound.builder()
                .id(1L).name("Group A").phaseType(PhaseType.GROUP_STAGE).roundOrder(1).build();

        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("World Cup")
                .type(TournamentType.REAL_FOOTBALL)
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.IN_PROGRESS)
                .numberOfGroups(1)
                .rounds(List.of(groupARound))
                .build();

        Event e1 = Event.builder().id(1L).tournament(tournament).round(groupARound)
                .teamHome(teamA).teamAway(teamB)
                .homeScore(2).awayScore(1).status(EventStatus.COMPLETED).build();
        Event e2 = Event.builder().id(2L).tournament(tournament).round(groupARound)
                .teamHome(teamC).teamAway(teamD)
                .homeScore(0).awayScore(0).status(EventStatus.COMPLETED).build();
        Event e3 = Event.builder().id(3L).tournament(tournament).round(groupARound)
                .teamHome(teamA).teamAway(teamC)
                .homeScore(1).awayScore(1).status(EventStatus.COMPLETED).build();
        Event e4 = Event.builder().id(4L).tournament(tournament).round(groupARound)
                .teamHome(teamB).teamAway(teamD)
                .homeScore(3).awayScore(0).status(EventStatus.COMPLETED).build();

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of(e1, e2, e3, e4));
        when(teamRepository.findAllById(any())).thenAnswer(invocation -> {
            Iterable<Long> ids = invocation.getArgument(0);
            List<Team> teams = new java.util.ArrayList<>();
            for (Long id : ids) {
                if (id.equals(1L)) teams.add(teamA);
                else if (id.equals(2L)) teams.add(teamB);
                else if (id.equals(3L)) teams.add(teamC);
                else if (id.equals(4L)) teams.add(teamD);
            }
            return teams;
        });

        TournamentScoreboardResponseDTO result = tournamentService.getScoreboard(1L);

        assertEquals(1L, result.getTournamentId());
        assertEquals("World Cup", result.getTournamentName());
        assertEquals("LEAGUE_BRACKET", result.getFormat());
        assertEquals(1, result.getGroups().size());

        GroupStandingsDTO groupDTO = result.getGroups().get(0);
        assertEquals("Group A", groupDTO.getGroupName());
        assertEquals(4, groupDTO.getTeamStandings().size());

        List<TeamStatsResponseDTO> standings = groupDTO.getTeamStandings();

        TeamStatsResponseDTO brazil = standings.stream()
                .filter(s -> s.getTeamName().equals("Brazil")).findFirst().orElseThrow();
        assertEquals(2, brazil.getMatchesPlayed());
        assertEquals(1, brazil.getWins());
        assertEquals(1, brazil.getDraws());
        assertEquals(0, brazil.getLosses());
        assertEquals(3, brazil.getGoalsScored());
        assertEquals(2, brazil.getGoalsConceded());
        assertEquals(1, brazil.getGoalDifference());
        assertEquals(4, brazil.getPoints());

        TeamStatsResponseDTO argentina = standings.stream()
                .filter(s -> s.getTeamName().equals("Argentina")).findFirst().orElseThrow();
        assertEquals(2, argentina.getMatchesPlayed());
        assertEquals(1, argentina.getWins());
        assertEquals(0, argentina.getDraws());
        assertEquals(1, argentina.getLosses());
        assertEquals(4, argentina.getGoalsScored());
        assertEquals(2, argentina.getGoalsConceded());
        assertEquals(2, argentina.getGoalDifference());
        assertEquals(3, argentina.getPoints());

        TeamStatsResponseDTO uruguay = standings.stream()
                .filter(s -> s.getTeamName().equals("Uruguay")).findFirst().orElseThrow();
        assertEquals(2, uruguay.getMatchesPlayed());
        assertEquals(0, uruguay.getWins());
        assertEquals(2, uruguay.getDraws());
        assertEquals(0, uruguay.getLosses());
        assertEquals(1, uruguay.getGoalsScored());
        assertEquals(1, uruguay.getGoalsConceded());
        assertEquals(0, uruguay.getGoalDifference());
        assertEquals(2, uruguay.getPoints());

        TeamStatsResponseDTO colombia = standings.stream()
                .filter(s -> s.getTeamName().equals("Colombia")).findFirst().orElseThrow();
        assertEquals(2, colombia.getMatchesPlayed());
        assertEquals(0, colombia.getWins());
        assertEquals(1, colombia.getDraws());
        assertEquals(1, colombia.getLosses());
        assertEquals(0, colombia.getGoalsScored());
        assertEquals(3, colombia.getGoalsConceded());
        assertEquals(-3, colombia.getGoalDifference());
        assertEquals(1, colombia.getPoints());

        assertEquals("Brazil", standings.get(0).getTeamName());
        assertEquals("Argentina", standings.get(1).getTeamName());
        assertEquals("Uruguay", standings.get(2).getTeamName());
        assertEquals("Colombia", standings.get(3).getTeamName());
    }

    @Test
    void getScoreboard_realFootballMultipleGroups_shouldReturnStandingsPerGroup() {
        Team teamA = Team.builder().id(1L).name("Brazil").abbreviation("BRA").build();
        Team teamB = Team.builder().id(2L).name("Argentina").abbreviation("ARG").build();
        Team teamC = Team.builder().id(3L).name("France").abbreviation("FRA").build();
        Team teamD = Team.builder().id(4L).name("Germany").abbreviation("GER").build();

        TournamentRound groupARound = TournamentRound.builder()
                .id(1L).name("Group A").phaseType(PhaseType.GROUP_STAGE).roundOrder(1).build();
        TournamentRound groupBRound = TournamentRound.builder()
                .id(2L).name("Group B").phaseType(PhaseType.GROUP_STAGE).roundOrder(2).build();

        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("World Cup")
                .type(TournamentType.REAL_FOOTBALL)
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.IN_PROGRESS)
                .numberOfGroups(2)
                .rounds(List.of(groupARound, groupBRound))
                .build();

        Event e1 = Event.builder().id(1L).tournament(tournament).round(groupARound)
                .teamHome(teamA).teamAway(teamB)
                .homeScore(1).awayScore(0).status(EventStatus.COMPLETED).build();
        Event e2 = Event.builder().id(2L).tournament(tournament).round(groupBRound)
                .teamHome(teamC).teamAway(teamD)
                .homeScore(2).awayScore(2).status(EventStatus.COMPLETED).build();

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of(e1, e2));
        when(teamRepository.findAllById(any())).thenAnswer(invocation -> {
            Iterable<Long> ids = invocation.getArgument(0);
            List<Team> teams = new java.util.ArrayList<>();
            for (Long id : ids) {
                if (id.equals(1L)) teams.add(teamA);
                else if (id.equals(2L)) teams.add(teamB);
                else if (id.equals(3L)) teams.add(teamC);
                else if (id.equals(4L)) teams.add(teamD);
            }
            return teams;
        });

        TournamentScoreboardResponseDTO result = tournamentService.getScoreboard(1L);

        assertEquals(2, result.getGroups().size());

        GroupStandingsDTO groupA = result.getGroups().stream()
                .filter(g -> "Group A".equals(g.getGroupName())).findFirst().orElseThrow();
        assertEquals(2, groupA.getTeamStandings().size());
        assertEquals("Brazil", groupA.getTeamStandings().get(0).getTeamName());
        assertEquals(3, groupA.getTeamStandings().get(0).getPoints());
        assertEquals("Argentina", groupA.getTeamStandings().get(1).getTeamName());
        assertEquals(0, groupA.getTeamStandings().get(1).getPoints());

        GroupStandingsDTO groupB = result.getGroups().stream()
                .filter(g -> "Group B".equals(g.getGroupName())).findFirst().orElseThrow();
        assertEquals(2, groupB.getTeamStandings().size());
        assertEquals("France", groupB.getTeamStandings().get(0).getTeamName());
        assertEquals(1, groupB.getTeamStandings().get(0).getPoints());
        assertEquals("Germany", groupB.getTeamStandings().get(1).getTeamName());
        assertEquals(1, groupB.getTeamStandings().get(1).getPoints());
    }

    @Test
    void getScoreboard_realFootballTeamStandingsSort_shouldNotUseEloAsTiebreaker() {
        Team teamA = Team.builder().id(1L).name("Team A").abbreviation("TA").build();
        Team teamB = Team.builder().id(2L).name("Team B").abbreviation("TB").build();

        TournamentRound groupRound = TournamentRound.builder()
                .id(1L).name("Group A").phaseType(PhaseType.GROUP_STAGE).roundOrder(1).build();

        Tournament tournament = Tournament.builder()
                .id(1L)
                .name("Cup")
                .type(TournamentType.REAL_FOOTBALL)
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.IN_PROGRESS)
                .numberOfGroups(1)
                .rounds(List.of(groupRound))
                .build();

        Event e1 = Event.builder().id(1L).tournament(tournament).round(groupRound)
                .teamHome(teamA).teamAway(teamB)
                .homeScore(1).awayScore(1).status(EventStatus.COMPLETED).build();

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of(e1));
        when(teamRepository.findAllById(any())).thenReturn(List.of(teamA, teamB));

        TournamentScoreboardResponseDTO result = tournamentService.getScoreboard(1L);

        GroupStandingsDTO group = result.getGroups().get(0);
        List<TeamStatsResponseDTO> standings = group.getTeamStandings();

        assertEquals(2, standings.size());
        assertEquals(1, standings.get(0).getPoints());
        assertEquals(1, standings.get(1).getPoints());
        assertEquals(0, standings.get(0).getGoalDifference());
        assertEquals(0, standings.get(1).getGoalDifference());
        assertEquals(1, standings.get(0).getGoalsScored());
        assertEquals(1, standings.get(1).getGoalsScored());
    }

    @Test
    void softDeleteTournament_withRealFootball_softDeletesBetsAndTransactions() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .type(TournamentType.REAL_FOOTBALL)
                .status(TournamentStatus.CREATED)
                .build();

        GroupTournament groupTournament = new GroupTournament();
        groupTournament.setId(100L);
        groupTournament.setTournament(tournament);

        BetSlip betSlip = new BetSlip();
        betSlip.setId(10L);

        Transaction transaction = new Transaction();
        transaction.setId(20L);

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(currentUserContext.getRequiredGroupId()).thenReturn(5L);
        when(groupTournamentRepository.findByTournamentIdAndGroupId(1L, 5L)).thenReturn(Optional.of(groupTournament));
        when(betSlipRepository.findByGroupTournamentId(100L)).thenReturn(List.of(betSlip));
        when(transactionRepository.findByTournamentWalletGroupTournamentId(100L)).thenReturn(List.of(transaction));

        tournamentService.softDeleteTournament(1L);

        verify(groupAuthorizationService).requireCurrentGroupOwner();
        verify(betSlipRepository).saveAll(List.of(betSlip));
        verify(transactionRepository).saveAll(List.of(transaction));
        verify(groupTournamentRepository).save(groupTournament);
        assertTrue(betSlip.getDeletedAt() != null);
        assertTrue(transaction.getDeletedAt() != null);
        assertTrue(groupTournament.getDeletedAt() != null);
    }

    @Test
    void softDeleteTournament_withFifaMatch_softDeletesTournamentAndBetsAndTransactions() {
        Tournament tournament = Tournament.builder()
                .id(2L)
                .type(TournamentType.FIFA_MATCH)
                .status(TournamentStatus.CREATED)
                .build();

        GroupTournament groupTournament = new GroupTournament();
        groupTournament.setId(200L);
        groupTournament.setTournament(tournament);

        BetSlip betSlip = new BetSlip();
        betSlip.setId(11L);

        Transaction transaction = new Transaction();
        transaction.setId(21L);

        when(tournamentRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(tournament));
        when(currentUserContext.getRequiredGroupId()).thenReturn(5L);
        when(groupTournamentRepository.findByTournamentIdAndGroupId(2L, 5L)).thenReturn(Optional.of(groupTournament));
        when(betSlipRepository.findByGroupTournamentId(200L)).thenReturn(List.of(betSlip));
        when(transactionRepository.findByTournamentWalletGroupTournamentId(200L)).thenReturn(List.of(transaction));

        tournamentService.softDeleteTournament(2L);

        verify(groupAuthorizationService).requireTournamentAdmin(2L);
        verify(betSlipRepository).saveAll(List.of(betSlip));
        verify(transactionRepository).saveAll(List.of(transaction));
        verify(tournamentRepository).save(tournament);
        assertTrue(betSlip.getDeletedAt() != null);
        assertTrue(transaction.getDeletedAt() != null);
        assertTrue(tournament.getDeletedAt() != null);
    }
}
