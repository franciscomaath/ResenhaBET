package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.OddsProperties;
import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.CompletedEventRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.EventRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.EventUpdateRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.FinishEventRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.EventResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.entity.GroupTournament;
import com.franciscomaath.resenhaapi.domain.entity.Market;
import com.franciscomaath.resenhaapi.domain.entity.Outcome;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.entity.Tournament;
import com.franciscomaath.resenhaapi.domain.entity.TournamentRound;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.enums.GenerationMode;
import com.franciscomaath.resenhaapi.domain.enums.PhaseType;
import com.franciscomaath.resenhaapi.domain.enums.TournamentFormat;
import com.franciscomaath.resenhaapi.domain.enums.TournamentStatus;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import com.franciscomaath.resenhaapi.domain.event.EventChangeEvent;
import com.franciscomaath.resenhaapi.domain.event.EventCompletedEvent;
import com.franciscomaath.resenhaapi.domain.exception.BusinessException;
import com.franciscomaath.resenhaapi.domain.exception.InvalidStateException;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupTournamentRepository;
import com.franciscomaath.resenhaapi.domain.repository.MarketRepository;
import com.franciscomaath.resenhaapi.domain.repository.OutcomeRepository;
import com.franciscomaath.resenhaapi.domain.repository.PlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.RoundRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentPlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentRepository;
import com.franciscomaath.resenhaapi.mapper.EventMapper;
import com.franciscomaath.resenhaapi.service.BetService;
import com.franciscomaath.resenhaapi.service.EloService;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.service.MarketService;
import com.franciscomaath.resenhaapi.service.OddsCalculatorService;
import com.franciscomaath.resenhaapi.service.TournamentService;
import com.franciscomaath.resenhaapi.service.dto.H2HRecord;
import com.franciscomaath.resenhaapi.service.dto.OddsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private TournamentPlayerRepository tournamentPlayerRepository;

    @Mock
    private EloService eloService;

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
    private MarketService marketService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TournamentServiceImpl tournamentService;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    @Mock
    private GroupTournamentRepository groupTournamentRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    @BeforeEach
    void setUpMultiGroupDefaults() {
        lenient().when(currentUserContext.getRequiredGroupId()).thenReturn(1L);
        lenient().when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(anyLong(), eq(1L))).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return Optional.of(Player.builder()
                    .id(id)
                    .name("Player " + id)
                    .currentElo(BigDecimal.valueOf(1000))
                    .build());
        });
    }

    @Test
    void create_shouldPersistEventWhenDataIsValid() {
        Tournament tournament = Tournament.builder().id(1L).format(TournamentFormat.BRACKET).build();
        Player home = Player.builder().id(10L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Player away = Player.builder().id(20L).name("Away").currentElo(BigDecimal.valueOf(1100)).build();
        TournamentRound round = TournamentRound.builder().id(100L).name("Round 1").phaseType(PhaseType.KNOCKOUT).build();

        EventRequestDTO request = new EventRequestDTO();
        request.setTournamentId(1L);
        request.setRoundId(100L);
        request.setPlayerHomeId(10L);
        request.setPlayerAwayId(20L);

        Event saved = Event.builder()
                .id(5L)
                .tournament(tournament)
                .round(round)
                .playerHome(home)
                .playerAway(away)
                .status(EventStatus.CREATED)
                .homeScore(0)
                .awayScore(0)
                .isKnockout(true)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(5L);
        dto.setStatus("CREATED");

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(home));
        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(20L, 1L)).thenReturn(Optional.of(away));
        when(tournamentPlayerRepository.existsByTournamentIdAndPlayerId(1L, 10L)).thenReturn(true);
        when(tournamentPlayerRepository.existsByTournamentIdAndPlayerId(1L, 20L)).thenReturn(true);
        when(roundRepository.findById(100L)).thenReturn(Optional.of(round));
        when(oddsProperties.getH2hMatchLimit()).thenReturn(10);
        when(eventRepository.findDirectConfrontations(10L, 20L, EventStatus.COMPLETED, PageRequest.of(0, 10)))
                .thenReturn(List.of());
        when(oddsCalculatorService.calculate(any(), any(), any())).thenReturn(new OddsResult(BigDecimal.valueOf(2.0), BigDecimal.valueOf(0), BigDecimal.valueOf(2.5)));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);
        when(marketRepository.save(any(Market.class))).thenAnswer(invocation -> {
            Market m = invocation.getArgument(0);
            m.setId(1L);
            return m;
        });
        when(eventMapper.toResponse(saved)).thenReturn(dto);

        EventResponseDTO result = eventService.create(request);

        ArgumentCaptor<Event> captor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(captor.capture());
        Event persisted = captor.getValue();

        assertEquals(EventStatus.CREATED, persisted.getStatus());
        assertEquals(0, persisted.getHomeScore());
        assertEquals(0, persisted.getAwayScore());
        assertTrue(persisted.getIsKnockout());
        assertNotNull(persisted.getGameDatetime());
        assertEquals(BigDecimal.valueOf(1200), persisted.getHomeEloBefore());
        assertEquals(BigDecimal.valueOf(1100), persisted.getAwayEloBefore());
        assertEquals(5L, result.getId());

        verify(marketRepository).save(any(Market.class));
        verify(outcomeRepository, times(3)).save(any(Outcome.class));
    }

    @Test
    void create_shouldBuildH2HRecordFromPastEvents() {
        Tournament tournament = Tournament.builder().id(1L).format(TournamentFormat.LEAGUE).build();
        Player home = Player.builder().id(10L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Player away = Player.builder().id(20L).name("Away").currentElo(BigDecimal.valueOf(1100)).build();
        TournamentRound round = TournamentRound.builder().id(100L).name("Round 1").build();

        Event pastEvent1 = Event.builder()
                .id(1L)
                .playerHome(home)
                .playerAway(away)
                .homeScore(2)
                .awayScore(1)
                .status(EventStatus.COMPLETED)
                .build();

        Event pastEvent2 = Event.builder()
                .id(2L)
                .playerHome(away)
                .playerAway(home)
                .homeScore(3)
                .awayScore(1)
                .status(EventStatus.COMPLETED)
                .build();

        Event pastEvent3 = Event.builder()
                .id(3L)
                .playerHome(home)
                .playerAway(away)
                .homeScore(1)
                .awayScore(1)
                .status(EventStatus.COMPLETED)
                .build();

        EventRequestDTO request = new EventRequestDTO();
        request.setTournamentId(1L);
        request.setRoundId(100L);
        request.setPlayerHomeId(10L);
        request.setPlayerAwayId(20L);

        Event saved = Event.builder()
                .id(5L)
                .tournament(tournament)
                .round(round)
                .playerHome(home)
                .playerAway(away)
                .status(EventStatus.CREATED)
                .homeScore(0)
                .awayScore(0)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(5L);
        dto.setStatus("CREATED");

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(home));
        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(20L, 1L)).thenReturn(Optional.of(away));
        when(tournamentPlayerRepository.existsByTournamentIdAndPlayerId(1L, 10L)).thenReturn(true);
        when(tournamentPlayerRepository.existsByTournamentIdAndPlayerId(1L, 20L)).thenReturn(true);
        when(roundRepository.findById(100L)).thenReturn(Optional.of(round));
        when(oddsProperties.getH2hMatchLimit()).thenReturn(10);
        when(eventRepository.findDirectConfrontations(10L, 20L, EventStatus.COMPLETED, PageRequest.of(0, 10)))
                .thenReturn(List.of(pastEvent1, pastEvent2, pastEvent3));
        when(oddsCalculatorService.calculate(any(), any(), any())).thenReturn(new OddsResult(BigDecimal.valueOf(2.0), BigDecimal.valueOf(3.0), BigDecimal.valueOf(2.5)));
        when(eventRepository.save(any(Event.class))).thenReturn(saved);
        when(marketRepository.save(any(Market.class))).thenAnswer(invocation -> {
            Market m = invocation.getArgument(0);
            m.setId(1L);
            return m;
        });
        when(eventMapper.toResponse(saved)).thenReturn(dto);

        eventService.create(request);

        ArgumentCaptor<H2HRecord> h2hCaptor = ArgumentCaptor.forClass(H2HRecord.class);
        verify(oddsCalculatorService).calculate(any(), any(), h2hCaptor.capture());

        H2HRecord capturedH2H = h2hCaptor.getValue();
        assertEquals(1, capturedH2H.getHomeWins());
        assertEquals(1, capturedH2H.getAwayWins());
        assertEquals(1, capturedH2H.getDraws());
        assertEquals(3, capturedH2H.getTotalMatches());
    }

    @Test
    void create_whenPlayerIsNotRegistered_shouldThrowBusinessException() {
        Tournament tournament = Tournament.builder().id(1L).format(TournamentFormat.LEAGUE).build();
        Player home = Player.builder().id(10L).build();

        EventRequestDTO request = new EventRequestDTO();
        request.setTournamentId(1L);
        request.setRoundId(100L);
        request.setPlayerHomeId(10L);
        request.setPlayerAwayId(20L);

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(home));
        when(tournamentPlayerRepository.existsByTournamentIdAndPlayerId(1L, 10L)).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.create(request));

        assertTrue(ex.getMessage().contains("não está inscrito"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void create_whenRoundDoesNotExist_shouldThrowResourceNotFound() {
        Tournament tournament = Tournament.builder().id(1L).format(TournamentFormat.LEAGUE).build();
        Player home = Player.builder().id(10L).build();
        Player away = Player.builder().id(20L).build();

        EventRequestDTO request = new EventRequestDTO();
        request.setTournamentId(1L);
        request.setRoundId(999L);
        request.setPlayerHomeId(10L);
        request.setPlayerAwayId(20L);

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(home));
        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(20L, 1L)).thenReturn(Optional.of(away));
        when(tournamentPlayerRepository.existsByTournamentIdAndPlayerId(1L, 10L)).thenReturn(true);
        when(tournamentPlayerRepository.existsByTournamentIdAndPlayerId(1L, 20L)).thenReturn(true);
        when(roundRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> eventService.create(request));

        assertTrue(ex.getMessage().contains("Round not found"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void create_whenRealFootball_shouldThrowBusinessException() {
        Tournament tournament = Tournament.builder()
                .id(1L)
                .type(TournamentType.REAL_FOOTBALL)
                .build();

        EventRequestDTO request = new EventRequestDTO();
        request.setPlayerHomeId(1L);
        request.setPlayerAwayId(2L);
        request.setTournamentId(1L);
        request.setRoundId(1L);

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.create(request));

        assertTrue(ex.getMessage().contains("REAL_FOOTBALL"));
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void findByTournamentId_shouldReturnMappedEvents() {
        Tournament tournament = Tournament.builder().id(1L).build();
        Event event = Event.builder().id(10L).tournament(tournament).build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(10L);
        dto.setTournamentId(1L);

        when(eventRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class))).thenReturn(List.of(event));
        when(eventMapper.toResponse(event)).thenReturn(dto);

        List<EventResponseDTO> result = eventService.findAll(1L, null);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(1L, result.get(0).getTournamentId());
    }

    @Test
    void startEvent_shouldMoveStatusToInProgress() {
        Event event = Event.builder()
                .id(3L)
                .status(EventStatus.CREATED)
                .round(TournamentRound.builder().id(1L).build())
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(3L);
        dto.setStatus("IN_PROGRESS");

        when(eventRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(dto);

        EventResponseDTO result = eventService.startEvent(3L);

        assertEquals(EventStatus.IN_PROGRESS, event.getStatus());
        assertEquals("IN_PROGRESS", result.getStatus());
        verify(marketService).closeMarket(3L);
    }

    @Test
    void startEvent_whenStatusIsInvalid_shouldThrowBusinessException() {
        Event event = Event.builder()
                .id(3L)
                .status(EventStatus.COMPLETED)
                .round(TournamentRound.builder().id(1L).build())
                .build();

        when(eventRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(event));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.startEvent(3L));

        assertTrue(ex.getMessage().contains("already started or completed"));
    }

    @Test
    void updateScore_shouldPersistScoresWhenEventInProgress() {
        Event event = Event.builder()
                .id(7L)
                .status(EventStatus.IN_PROGRESS)
                .homeScore(0)
                .awayScore(0)
                .build();

        EventUpdateRequestDTO request = new EventUpdateRequestDTO();
        request.setHomeScore(2);
        request.setAwayScore(1);

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(7L);
        dto.setHomeScore(2);
        dto.setAwayScore(1);

        when(eventRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(dto);

        EventResponseDTO result = eventService.updateScore(7L, request);

        assertEquals(2, event.getHomeScore());
        assertEquals(1, event.getAwayScore());
        assertEquals(2, result.getHomeScore());
        assertEquals(1, result.getAwayScore());
    }

    @Test
    void updateScore_whenEventIsNotInProgress_shouldThrowInvalidStateException() {
        Event event = Event.builder()
                .id(7L)
                .status(EventStatus.CREATED)
                .build();

        EventUpdateRequestDTO request = new EventUpdateRequestDTO();
        request.setHomeScore(2);
        request.setAwayScore(1);

        when(eventRepository.findByIdAndDeletedAtIsNull(7L)).thenReturn(Optional.of(event));

        InvalidStateException ex = assertThrows(InvalidStateException.class,
                () -> eventService.updateScore(7L, request));

        assertTrue(ex.getMessage().contains("not in progress"));
    }

    @Test
    void finishEvent_shouldMoveStatusToCompletedWhenScoreExistsAndInProgress() {
        Player home = Player.builder().id(10L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Player away = Player.builder().id(20L).name("Away").currentElo(BigDecimal.valueOf(1100)).build();
        Tournament tournament = Tournament.builder().id(1L).format(TournamentFormat.BRACKET).build();
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.IN_PROGRESS)
                .playerHome(home)
                .playerAway(away)
                .homeScore(3)
                .awayScore(2)
                .tournament(tournament)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(9L);
        dto.setStatus("COMPLETED");

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(dto);

        EventResponseDTO result = eventService.finishEvent(9L);

        assertEquals(EventStatus.COMPLETED, event.getStatus());
        assertEquals(BigDecimal.valueOf(1200), event.getHomeEloBefore());
        assertEquals(BigDecimal.valueOf(1100), event.getAwayEloBefore());
        assertEquals("COMPLETED", result.getStatus());
        verify(eventPublisher).publishEvent(any(EventCompletedEvent.class));
        verify(eloService).applyEloForEvent(event);
    }

    @Test
    void resetEvent_shouldReturnToCreatedAndReopenMarkets() {
        Tournament tournament = Tournament.builder().id(1L).format(TournamentFormat.BRACKET).build();
        Event event = Event.builder()
                .id(3L)
                .status(EventStatus.IN_PROGRESS)
                .homeScore(2)
                .awayScore(1)
                .penaltiesHome(3)
                .penaltiesAway(2)
                .homeEloBefore(BigDecimal.valueOf(1200))
                .awayEloBefore(BigDecimal.valueOf(1100))
                .tournament(tournament)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(3L);
        dto.setStatus("CREATED");

        when(eventRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(dto);

        EventResponseDTO result = eventService.resetEvent(3L);

        assertEquals(EventStatus.CREATED, event.getStatus());
        assertEquals(0, event.getHomeScore());
        assertEquals(0, event.getAwayScore());
        assertNull(event.getPenaltiesHome());
        assertNull(event.getPenaltiesAway());
        assertNull(event.getHomeEloBefore());
        assertNull(event.getAwayEloBefore());
        assertEquals("CREATED", result.getStatus());
        verify(marketService).openMarket(3L);
        verify(eventPublisher).publishEvent(any(EventChangeEvent.class));
    }

    @Test
    void createCompleted_shouldPersistCompletedEventAndApplyElo() {
        Player home = Player.builder().id(10L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Player away = Player.builder().id(20L).name("Away").currentElo(BigDecimal.valueOf(1100)).build();
        Tournament tournament = Tournament.builder().id(1L).format(TournamentFormat.BRACKET).build();
        TournamentRound round = TournamentRound.builder().id(100L).name("Round 1").phaseType(PhaseType.KNOCKOUT).build();

        CompletedEventRequestDTO request = new CompletedEventRequestDTO();
        request.setTournamentId(1L);
        request.setRoundId(100L);
        request.setPlayerHomeId(10L);
        request.setPlayerAwayId(20L);
        request.setHomeScore(2);
        request.setAwayScore(0);

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(11L);
        dto.setStatus("COMPLETED");

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(roundRepository.findById(100L)).thenReturn(Optional.of(round));
        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(home));
        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(20L, 1L)).thenReturn(Optional.of(away));
        when(tournamentPlayerRepository.existsByTournamentIdAndPlayerId(1L, 10L)).thenReturn(true);
        when(tournamentPlayerRepository.existsByTournamentIdAndPlayerId(1L, 20L)).thenReturn(true);
        when(eventRepository.existsByPlayerHomeIdAndPlayerAwayIdAndRoundId(10L, 20L, 100L)).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(11L);
            }
            return saved;
        });
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of());
        when(eventMapper.toResponse(any(Event.class))).thenReturn(dto);

        EventResponseDTO result = eventService.createCompleted(request);

        assertEquals(EventStatus.COMPLETED, EventStatus.valueOf(result.getStatus()));
        verify(eventPublisher).publishEvent(any(EventCompletedEvent.class));
        verify(eloService).applyEloForEvent(any(Event.class));
    }

    @Test
    void createCompleted_byeShouldAdvanceWinnerWithoutElo() {
        Player home = Player.builder().id(10L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Tournament tournament = Tournament.builder().id(1L).format(TournamentFormat.BRACKET).build();
        TournamentRound round = TournamentRound.builder().id(100L).name("Quarterfinal").phaseType(PhaseType.KNOCKOUT).build();

        CompletedEventRequestDTO request = new CompletedEventRequestDTO();
        request.setTournamentId(1L);
        request.setRoundId(100L);
        request.setPlayerHomeId(10L);
        request.setHomeScore(1);
        request.setAwayScore(0);
        request.setIsBye(true);

        Event byeEvent = Event.builder()
                .id(11L)
                .tournament(tournament)
                .round(round)
                .playerHome(home)
                .homeScore(1)
                .awayScore(0)
                .isKnockout(true)
                .isBye(true)
                .status(EventStatus.COMPLETED)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(11L);
        dto.setStatus("COMPLETED");

        when(tournamentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(tournament));
        when(roundRepository.findById(100L)).thenReturn(Optional.of(round));
        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(home));
        when(tournamentPlayerRepository.existsByTournamentIdAndPlayerId(1L, 10L)).thenReturn(true);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(11L);
            }
            return saved;
        });
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of(byeEvent));
        when(eventMapper.toResponse(any(Event.class))).thenReturn(dto);

        EventResponseDTO result = eventService.createCompleted(request);

        assertEquals("COMPLETED", result.getStatus());
        verify(eloService, never()).applyEloForEvent(any());
        verify(eventPublisher).publishEvent(any(EventCompletedEvent.class));
    }

    @Test
    void reopenEvent_shouldRollbackResultAndCloseDownstreamMarkets() {
        Player home = Player.builder().id(10L).name("Home").currentElo(BigDecimal.valueOf(1210)).build();
        Player away = Player.builder().id(20L).name("Away").currentElo(BigDecimal.valueOf(1180)).build();
        Tournament tournament = Tournament.builder()
                .id(1L)
                .status(TournamentStatus.COMPLETED)
                .endDate(java.time.LocalDateTime.now())
                .build();
        TournamentRound round = TournamentRound.builder().id(100L).name("Semi-Finals").phaseType(PhaseType.KNOCKOUT).build();
        TournamentRound nextRound = TournamentRound.builder().id(101L).name("Final").phaseType(PhaseType.KNOCKOUT).build();

        Event nextEvent = Event.builder()
                .id(12L)
                .status(EventStatus.CREATED)
                .round(nextRound)
                .tournament(tournament)
                .homeSourceEvent(null)
                .awaySourceEvent(null)
                .playerHome(home)
                .playerAway(null)
                .build();

        Market nextMarket = new Market();
        nextMarket.setId(44L);
        nextMarket.setEvent(nextEvent);
        nextMarket.setMarketType(com.franciscomaath.resenhaapi.domain.enums.MarketType.MATCH_RESULT);
        nextMarket.setStatus(com.franciscomaath.resenhaapi.domain.enums.MarketStatus.OPEN);

        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.COMPLETED)
                .playerHome(home)
                .playerAway(away)
                .homeScore(2)
                .awayScore(1)
                .isKnockout(true)
                .tournament(tournament)
                .round(round)
                .nextRoundEvent(nextEvent)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(9L);
        dto.setStatus("IN_PROGRESS");

        GroupTournament groupTournament = GroupTournament.builder().id(100L).build();

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));
        when(groupTournamentRepository.findByTournamentIdAndGroupId(1L, 1L)).thenReturn(Optional.of(groupTournament));
        when(eventRepository.save(nextEvent)).thenReturn(nextEvent);
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(dto);
        when(marketRepository.findAllByEventId(12L)).thenReturn(List.of(nextMarket));

        EventResponseDTO result = eventService.reopenEvent(9L);

        assertEquals(EventStatus.IN_PROGRESS, event.getStatus());
        assertNull(event.getHomeScore());
        assertNull(event.getAwayScore());
        assertNull(event.getPenaltiesHome());
        assertNull(event.getPenaltiesAway());
        assertNull(event.getHomeEloBefore());
        assertNull(event.getAwayEloBefore());
        assertNull(nextEvent.getPlayerHome());
        verify(betService).reopenBetsForEvent(9L);
        verify(marketService).closeMarket(12L);
        verify(tournamentRepository).save(tournament);
        verify(eloService).recalculateGroupElos(1L);
        verify(eventPublisher).publishEvent(any(EventChangeEvent.class));
        assertEquals("IN_PROGRESS", result.getStatus());
    }

    @Test
    void finishEvent_whenScoreIsNull_shouldThrowBusinessException() {
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.IN_PROGRESS)
                .homeScore(null)
                .awayScore(1)
                .build();

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.finishEvent(9L));

        assertTrue(ex.getMessage().contains("Score not found"));
    }

    @Test
    void finishEvent_whenStatusIsInvalid_shouldThrowBusinessException() {
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.CREATED)
                .homeScore(1)
                .awayScore(1)
                .build();

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.finishEvent(9L));

        assertTrue(ex.getMessage().contains("already finished or not started"));
    }

    // B30 — Knockout event behavior tests

    @Test
    void finishEvent_knockoutWithDraw_shouldMoveToPenalties() {
        Player home = Player.builder().id(10L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Player away = Player.builder().id(20L).name("Away").currentElo(BigDecimal.valueOf(1100)).build();
        Tournament tournament = Tournament.builder().id(1L).format(TournamentFormat.BRACKET).build();
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.IN_PROGRESS)
                .playerHome(home)
                .playerAway(away)
                .homeScore(1)
                .awayScore(1)
                .isKnockout(true)
                .tournament(tournament)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(9L);
        dto.setStatus("PENALTIES");

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(dto);

        EventResponseDTO result = eventService.finishEvent(9L);

        assertEquals(EventStatus.PENALTIES, event.getStatus());
        assertEquals("PENALTIES", result.getStatus());
        verify(betService, never()).resolveBetsForEvent(any());
        verify(eloService, never()).applyEloForEvent(any());
    }

    @Test
    void finishEvent_knockoutWithClearWinner_shouldAdvanceWinner() {
        Player home = Player.builder().id(10L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Player away = Player.builder().id(20L).name("Away").currentElo(BigDecimal.valueOf(1100)).build();
        Tournament tournament = Tournament.builder().id(1L).format(TournamentFormat.BRACKET).build();
        TournamentRound round = TournamentRound.builder().id(1L).name("Semi-Finals").build();
        TournamentRound finalRound = TournamentRound.builder().id(2L).name("Final").build();
        Event nextEvent = Event.builder()
                .id(100L)
                .round(finalRound)
                .tournament(tournament)
                .build();
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.IN_PROGRESS)
                .playerHome(home)
                .playerAway(away)
                .homeScore(2)
                .awayScore(1)
                .isKnockout(true)
                .tournament(tournament)
                .round(round)
                .nextRoundEvent(nextEvent)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(9L);
        dto.setStatus("COMPLETED");

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventRepository.save(nextEvent)).thenReturn(nextEvent);
        when(eventMapper.toResponse(event)).thenReturn(dto);

        EventResponseDTO result = eventService.finishEvent(9L);

        assertEquals(EventStatus.COMPLETED, event.getStatus());
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(home, nextEvent.getPlayerHome());
        verify(eventPublisher).publishEvent(any(EventCompletedEvent.class));
        verify(eloService).applyEloForEvent(event);
    }

    @Test
    void finishEvent_whenOnlyFirstSemiFinalCompleted_shouldPopulateOnlyHomeSlotsFromMatchingSource() {
        Player lucas = Player.builder().id(8L).name("lucas").currentElo(BigDecimal.valueOf(1200)).build();
        Player luan = Player.builder().id(6L).name("luan").currentElo(BigDecimal.valueOf(1250)).build();
        Player tadeu = Player.builder().id(9L).name("tadeu").currentElo(BigDecimal.valueOf(1180)).build();
        Player francisco = Player.builder().id(10L).name("francisco").currentElo(BigDecimal.valueOf(1190)).build();
        Tournament tournament = Tournament.builder()
                .id(1L)
                .format(TournamentFormat.BRACKET)
                .hasThirdPlaceMatch(true)
                .build();
        TournamentRound semiFinalRound = TournamentRound.builder().id(1L).name("Semi-Finals").build();
        TournamentRound finalRound = TournamentRound.builder().id(2L).name("Final").build();
        TournamentRound thirdPlaceRound = TournamentRound.builder().id(3L).name("3rd Place").build();

        Event semiFinalOne = Event.builder()
                .id(13L)
                .status(EventStatus.IN_PROGRESS)
                .playerHome(lucas)
                .playerAway(luan)
                .homeScore(1)
                .awayScore(2)
                .isKnockout(true)
                .tournament(tournament)
                .round(semiFinalRound)
                .build();
        Event semiFinalTwo = Event.builder()
                .id(14L)
                .status(EventStatus.CREATED)
                .playerHome(tadeu)
                .playerAway(francisco)
                .homeScore(0)
                .awayScore(0)
                .isKnockout(true)
                .tournament(tournament)
                .round(semiFinalRound)
                .build();
        Event finalEvent = Event.builder()
                .id(15L)
                .status(EventStatus.CREATED)
                .isKnockout(true)
                .tournament(tournament)
                .round(finalRound)
                .homeSourceEvent(semiFinalOne)
                .awaySourceEvent(semiFinalTwo)
                .thirdPlaceMatch(false)
                .build();
        Event thirdPlaceEvent = Event.builder()
                .id(16L)
                .status(EventStatus.CREATED)
                .isKnockout(true)
                .tournament(tournament)
                .round(thirdPlaceRound)
                .homeSourceEvent(semiFinalOne)
                .awaySourceEvent(semiFinalTwo)
                .thirdPlaceMatch(true)
                .build();
        semiFinalOne.setNextRoundEvent(finalEvent);

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(13L);
        dto.setStatus("COMPLETED");

        when(eventRepository.findByIdAndDeletedAtIsNull(13L)).thenReturn(Optional.of(semiFinalOne));
        when(eventRepository.save(semiFinalOne)).thenReturn(semiFinalOne);
        when(eventRepository.save(finalEvent)).thenReturn(finalEvent);
        when(eventRepository.save(thirdPlaceEvent)).thenReturn(thirdPlaceEvent);
        when(eventRepository.findByTournamentIdAndRoundName(1L, "3rd Place")).thenReturn(Optional.of(thirdPlaceEvent));
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of(semiFinalOne, semiFinalTwo, finalEvent, thirdPlaceEvent));
        when(eventMapper.toResponse(semiFinalOne)).thenReturn(dto);

        EventResponseDTO result = eventService.finishEvent(13L);

        assertEquals("COMPLETED", result.getStatus());
        assertEquals(EventStatus.COMPLETED, semiFinalOne.getStatus());

        assertEquals(semiFinalOne, finalEvent.getHomeSourceEvent());
        assertEquals(semiFinalTwo, finalEvent.getAwaySourceEvent());
        assertEquals(luan, finalEvent.getPlayerHome());
        assertNull(finalEvent.getPlayerAway());

        assertEquals(semiFinalOne, thirdPlaceEvent.getHomeSourceEvent());
        assertEquals(semiFinalTwo, thirdPlaceEvent.getAwaySourceEvent());
        assertEquals(lucas, thirdPlaceEvent.getPlayerHome());
        assertNull(thirdPlaceEvent.getPlayerAway());
        assertTrue(thirdPlaceEvent.isThirdPlaceMatch());

        assertEquals(14L, semiFinalTwo.getId());
        assertEquals("tadeu", semiFinalTwo.getPlayerHome().getName());
        assertEquals("francisco", semiFinalTwo.getPlayerAway().getName());
        assertEquals(9L, semiFinalTwo.getPlayerHome().getId());
        assertEquals(10L, semiFinalTwo.getPlayerAway().getId());
        assertEquals(EventStatus.CREATED, semiFinalTwo.getStatus());
    }

    @Test
    void findAll_withTournamentId_shouldReturnEventsSortedByIdBeforeMapping() {
        Event finalEvent = Event.builder().id(15L).build();
        Event semiFinalTwo = Event.builder().id(14L).build();
        Event semiFinalOne = Event.builder().id(13L).build();

        EventResponseDTO finalDto = new EventResponseDTO();
        finalDto.setId(15L);
        EventResponseDTO semiFinalTwoDto = new EventResponseDTO();
        semiFinalTwoDto.setId(14L);
        EventResponseDTO semiFinalOneDto = new EventResponseDTO();
        semiFinalOneDto.setId(13L);

        when(eventRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Event>>any()))
                .thenReturn(List.of(finalEvent, semiFinalTwo, semiFinalOne));
        when(eventMapper.toResponse(finalEvent)).thenReturn(finalDto);
        when(eventMapper.toResponse(semiFinalTwo)).thenReturn(semiFinalTwoDto);
        when(eventMapper.toResponse(semiFinalOne)).thenReturn(semiFinalOneDto);

        List<EventResponseDTO> result = eventService.findAll(1L, null);

        assertEquals(List.of(13L, 14L, 15L), result.stream().map(EventResponseDTO::getId).toList());
    }

    @Test
    void recordPenalties_realTimeUpdate_shouldStayInPenalties() {
        Player home = Player.builder().id(10L).name("Home").build();
        Player away = Player.builder().id(20L).name("Away").build();
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.PENALTIES)
                .playerHome(home)
                .playerAway(away)
                .homeScore(1)
                .awayScore(1)
                .isKnockout(true)
                .build();

        FinishEventRequestDTO request = new FinishEventRequestDTO();
        request.setPenaltiesHome(3);
        request.setPenaltiesAway(2);

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(9L);
        dto.setStatus("PENALTIES");

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(dto);

        EventResponseDTO result = eventService.recordPenalties(9L, request);

        assertEquals(EventStatus.PENALTIES, event.getStatus());
        assertEquals(3, event.getPenaltiesHome());
        assertEquals(2, event.getPenaltiesAway());
        assertEquals("PENALTIES", result.getStatus());
        verify(betService, never()).resolveBetsForEvent(any());
    }

    @Test
    void recordPenalties_finalizationWithCompletedStatus_shouldResolveBetsAndAdvanceWinner() {
        Player home = Player.builder().id(10L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Player away = Player.builder().id(20L).name("Away").currentElo(BigDecimal.valueOf(1100)).build();
        Tournament tournament = Tournament.builder().id(1L).format(TournamentFormat.BRACKET).build();
        TournamentRound round = TournamentRound.builder().id(1L).name("Semi-Finals").build();
        TournamentRound finalRound = TournamentRound.builder().id(2L).name("Final").build();
        Event nextEvent = Event.builder()
                .id(100L)
                .round(finalRound)
                .tournament(tournament)
                .build();
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.PENALTIES)
                .playerHome(home)
                .playerAway(away)
                .homeScore(1)
                .awayScore(1)
                .isKnockout(true)
                .tournament(tournament)
                .round(round)
                .nextRoundEvent(nextEvent)
                .build();

        FinishEventRequestDTO request = new FinishEventRequestDTO();
        request.setPenaltiesHome(4);
        request.setPenaltiesAway(3);
        request.setStatus(EventStatus.COMPLETED);

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(9L);
        dto.setStatus("COMPLETED");

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventRepository.save(nextEvent)).thenReturn(nextEvent);
        when(eventMapper.toResponse(event)).thenReturn(dto);

        EventResponseDTO result = eventService.recordPenalties(9L, request);

        assertEquals(EventStatus.COMPLETED, event.getStatus());
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(4, event.getPenaltiesHome());
        assertEquals(3, event.getPenaltiesAway());
        assertEquals(home, nextEvent.getPlayerHome());
        verify(eventPublisher).publishEvent(any(EventCompletedEvent.class));
        verify(eloService).applyEloForEvent(event);
    }

    @Test
    void recordPenalties_whenEqualPenalties_shouldThrowBusinessException() {
        Player home = Player.builder().id(10L).name("Home").build();
        Player away = Player.builder().id(20L).name("Away").build();
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.PENALTIES)
                .playerHome(home)
                .playerAway(away)
                .homeScore(1)
                .awayScore(1)
                .isKnockout(true)
                .build();

        FinishEventRequestDTO request = new FinishEventRequestDTO();
        request.setPenaltiesHome(3);
        request.setPenaltiesAway(3);
        request.setStatus(EventStatus.COMPLETED);

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.recordPenalties(9L, request));
        assertTrue(ex.getMessage().contains("must be different"));
    }

    @Test
    void recordPenalties_whenNotPenaltiesStatus_shouldThrowBusinessException() {
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.IN_PROGRESS)
                .isKnockout(true)
                .build();

        FinishEventRequestDTO request = new FinishEventRequestDTO();
        request.setPenaltiesHome(3);
        request.setPenaltiesAway(2);

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.recordPenalties(9L, request));
        assertTrue(ex.getMessage().contains("not in PENALTIES"));
    }

    @Test
    void recordPenalties_whenNotKnockout_shouldThrowBusinessException() {
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.PENALTIES)
                .isKnockout(false)
                .build();

        FinishEventRequestDTO request = new FinishEventRequestDTO();
        request.setPenaltiesHome(3);
        request.setPenaltiesAway(2);

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.recordPenalties(9L, request));
        assertTrue(ex.getMessage().contains("not a knockout"));
    }

    @Test
    void finishEvent_shouldEndTournamentAutomaticallyWhenAllMatchesCompleted_league() {
        Player home = Player.builder().id(10L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Player away = Player.builder().id(20L).name("Away").currentElo(BigDecimal.valueOf(1100)).build();
        Tournament tournament = Tournament.builder()
                .id(1L)
                .format(TournamentFormat.LEAGUE)
                .status(TournamentStatus.IN_PROGRESS)
                .build();
        TournamentRound round = TournamentRound.builder().id(1L).name("Rodada 1").phaseType(PhaseType.GROUP_STAGE).build();

        Event completedEvent = Event.builder()
                .id(8L)
                .status(EventStatus.COMPLETED)
                .playerHome(Player.builder().id(30L).build())
                .playerAway(Player.builder().id(40L).build())
                .homeScore(2)
                .awayScore(1)
                .tournament(tournament)
                .round(round)
                .isKnockout(false)
                .build();

        Event lastEvent = Event.builder()
                .id(9L)
                .status(EventStatus.IN_PROGRESS)
                .playerHome(home)
                .playerAway(away)
                .homeScore(3)
                .awayScore(1)
                .tournament(tournament)
                .round(round)
                .isKnockout(false)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(9L);
        dto.setStatus("COMPLETED");

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(lastEvent));
        when(eventRepository.save(lastEvent)).thenReturn(lastEvent);
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of(completedEvent, lastEvent));
        when(eventMapper.toResponse(lastEvent)).thenReturn(dto);

        eventService.finishEvent(9L);

        assertEquals(TournamentStatus.COMPLETED, tournament.getStatus());
        assertNotNull(tournament.getEndDate());
        verify(tournamentRepository).save(tournament);
    }

    @Test
    void finishEvent_shouldEndTournamentAutomaticallyWhenAllMatchesCompleted_bracket() {
        Player home = Player.builder().id(10L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Player away = Player.builder().id(20L).name("Away").currentElo(BigDecimal.valueOf(1100)).build();
        Tournament tournament = Tournament.builder()
                .id(1L)
                .format(TournamentFormat.BRACKET)
                .status(TournamentStatus.IN_PROGRESS)
                .build();
        TournamentRound finalRound = TournamentRound.builder().id(2L).name("Final").phaseType(PhaseType.KNOCKOUT).build();

        Event semiFinal1 = Event.builder()
                .id(7L)
                .status(EventStatus.COMPLETED)
                .playerHome(Player.builder().id(30L).build())
                .playerAway(Player.builder().id(40L).build())
                .homeScore(2)
                .awayScore(0)
                .isKnockout(true)
                .tournament(tournament)
                .build();

        Event semiFinal2 = Event.builder()
                .id(8L)
                .status(EventStatus.COMPLETED)
                .playerHome(Player.builder().id(50L).build())
                .playerAway(Player.builder().id(60L).build())
                .homeScore(1)
                .awayScore(3)
                .isKnockout(true)
                .tournament(tournament)
                .build();

        Event finalEvent = Event.builder()
                .id(9L)
                .status(EventStatus.IN_PROGRESS)
                .playerHome(home)
                .playerAway(away)
                .homeScore(2)
                .awayScore(1)
                .isKnockout(true)
                .tournament(tournament)
                .round(finalRound)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(9L);
        dto.setStatus("COMPLETED");

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(finalEvent));
        when(eventRepository.save(finalEvent)).thenReturn(finalEvent);
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of(semiFinal1, semiFinal2, finalEvent));
        when(eventMapper.toResponse(finalEvent)).thenReturn(dto);

        eventService.finishEvent(9L);

        assertEquals(TournamentStatus.COMPLETED, tournament.getStatus());
        assertNotNull(tournament.getEndDate());
        verify(tournamentRepository).save(tournament);
    }

    @Test
    void finishEvent_shouldEndTournamentAutomaticallyWhenAllMatchesCompleted_leagueBracketWithKnockout() {
        Player home = Player.builder().id(10L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Player away = Player.builder().id(20L).name("Away").currentElo(BigDecimal.valueOf(1100)).build();
        TournamentRound groupRound = TournamentRound.builder().id(1L).name("Rodada 1 - Grupo A").phaseType(PhaseType.GROUP_STAGE).groupNumber(1).build();
        TournamentRound knockoutRound = TournamentRound.builder().id(2L).name("Semifinais").phaseType(PhaseType.KNOCKOUT).build();
        Tournament tournament = Tournament.builder()
                .id(1L)
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.IN_PROGRESS)
                .numberOfGroups(2)
                .playersAdvancingPerGroup(1)
                .rounds(List.of(groupRound, knockoutRound))
                .build();

        Event groupEvent = Event.builder()
                .id(7L)
                .status(EventStatus.COMPLETED)
                .playerHome(Player.builder().id(30L).build())
                .playerAway(Player.builder().id(40L).build())
                .homeScore(2)
                .awayScore(1)
                .isKnockout(false)
                .tournament(tournament)
                .round(groupRound)
                .build();

        Event knockoutEvent = Event.builder()
                .id(9L)
                .status(EventStatus.IN_PROGRESS)
                .playerHome(home)
                .playerAway(away)
                .homeScore(3)
                .awayScore(1)
                .isKnockout(true)
                .tournament(tournament)
                .round(knockoutRound)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(9L);
        dto.setStatus("COMPLETED");

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(knockoutEvent));
        when(eventRepository.save(knockoutEvent)).thenReturn(knockoutEvent);
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of(groupEvent, knockoutEvent));
        when(eventMapper.toResponse(knockoutEvent)).thenReturn(dto);

        eventService.finishEvent(9L);

        assertEquals(TournamentStatus.COMPLETED, tournament.getStatus());
        assertNotNull(tournament.getEndDate());
        verify(tournamentRepository).save(tournament);
    }

    @Test
    void finishEvent_shouldAutoAdvanceToBracketWhenLeagueBracketGroupStageCompleted_autoMode() {
        Player home = Player.builder().id(10L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Player away = Player.builder().id(20L).name("Away").currentElo(BigDecimal.valueOf(1100)).build();
        TournamentRound groupRound = TournamentRound.builder().id(1L).name("Rodada 1 - Grupo A").phaseType(PhaseType.GROUP_STAGE).groupNumber(1).build();
        Tournament tournament = Tournament.builder()
                .id(1L)
                .format(TournamentFormat.LEAGUE_BRACKET)
                .status(TournamentStatus.IN_PROGRESS)
                .generationMode(GenerationMode.AUTO)
                .numberOfGroups(1)
                .playersAdvancingPerGroup(2)
                .rounds(List.of(groupRound))
                .build();

        Event completedGroupEvent = Event.builder()
                .id(8L)
                .status(EventStatus.COMPLETED)
                .playerHome(Player.builder().id(30L).build())
                .playerAway(Player.builder().id(40L).build())
                .homeScore(1)
                .awayScore(0)
                .isKnockout(false)
                .tournament(tournament)
                .round(groupRound)
                .build();

        Event lastGroupEvent = Event.builder()
                .id(9L)
                .status(EventStatus.IN_PROGRESS)
                .playerHome(home)
                .playerAway(away)
                .homeScore(2)
                .awayScore(1)
                .isKnockout(false)
                .tournament(tournament)
                .round(groupRound)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(9L);
        dto.setStatus("COMPLETED");

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(lastGroupEvent));
        when(eventRepository.save(lastGroupEvent)).thenReturn(lastGroupEvent);
        when(eventRepository.findAllByTournamentId(1L)).thenReturn(List.of(completedGroupEvent, lastGroupEvent));
        when(eventMapper.toResponse(lastGroupEvent)).thenReturn(dto);

        eventService.finishEvent(9L);

        assertEquals(TournamentStatus.IN_PROGRESS, tournament.getStatus());
        verify(tournamentService).advanceToBracketInternal(1L);
        verify(tournamentRepository, never()).save(any(Tournament.class));
    }

    @Test
    void cancelEvent_whenStatusIsCreated_shouldCancelAndRefund() {
        Player home = Player.builder().id(10L).name("Home").build();
        Player away = Player.builder().id(20L).name("Away").build();
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.CREATED)
                .playerHome(home)
                .playerAway(away)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(9L);
        dto.setStatus("CANCELLED");

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(dto);

        EventResponseDTO result = eventService.cancelEvent(9L);

        assertEquals(EventStatus.CANCELLED, event.getStatus());
        assertEquals("CANCELLED", result.getStatus());
        verify(marketService).cancelMarket(9L);
        verify(eventPublisher).publishEvent(any(EventChangeEvent.class));
        verify(eventPublisher).publishEvent(any(EventCompletedEvent.class));
    }

    @Test
    void cancelEvent_whenStatusIsInProgress_shouldCancelAndRefund() {
        Player home = Player.builder().id(10L).name("Home").build();
        Player away = Player.builder().id(20L).name("Away").build();
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.IN_PROGRESS)
                .playerHome(home)
                .playerAway(away)
                .build();

        EventResponseDTO dto = new EventResponseDTO();
        dto.setId(9L);
        dto.setStatus("CANCELLED");

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));
        when(eventRepository.save(event)).thenReturn(event);
        when(eventMapper.toResponse(event)).thenReturn(dto);

        EventResponseDTO result = eventService.cancelEvent(9L);

        assertEquals(EventStatus.CANCELLED, event.getStatus());
        assertEquals("CANCELLED", result.getStatus());
        verify(marketService).cancelMarket(9L);
        verify(eventPublisher).publishEvent(any(EventCompletedEvent.class));
    }

    @Test
    void cancelEvent_whenStatusIsCompleted_shouldThrowBusinessException() {
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.COMPLETED)
                .build();

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.cancelEvent(9L));

        assertTrue(ex.getMessage().contains("CREATED"));
        assertTrue(ex.getMessage().contains("IN_PROGRESS"));
        verify(betService, never()).cancelBetsForEvent(any());
        verify(marketService, never()).cancelMarket(any());
    }

    @Test
    void cancelEvent_whenStatusIsCancelled_shouldThrowBusinessException() {
        Event event = Event.builder()
                .id(9L)
                .status(EventStatus.CANCELLED)
                .build();

        when(eventRepository.findByIdAndDeletedAtIsNull(9L)).thenReturn(Optional.of(event));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> eventService.cancelEvent(9L));

        assertTrue(ex.getMessage().contains("CREATED"));
        assertTrue(ex.getMessage().contains("IN_PROGRESS"));
        verify(betService, never()).cancelBetsForEvent(any());
        verify(marketService, never()).cancelMarket(any());
    }
}
