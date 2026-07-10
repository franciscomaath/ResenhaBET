package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.SchedulerProperties;
import com.franciscomaath.resenhaapi.controller.dto.response.EventResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Competition;
import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.entity.Team;
import com.franciscomaath.resenhaapi.domain.entity.Tournament;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import com.franciscomaath.resenhaapi.domain.event.EventChangeEvent;
import com.franciscomaath.resenhaapi.domain.event.EventCompletedEvent;
import com.franciscomaath.resenhaapi.domain.event.EventMarketsCloseRequestedEvent;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.mapper.EventMapper;
import com.franciscomaath.resenhaapi.service.ApiFootballClient;
import com.franciscomaath.resenhaapi.service.EventService;
import com.franciscomaath.resenhaapi.service.dto.MatchDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RealFootballLiveServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ApiFootballClient apiFootballClient;

    @Mock
    private EventService eventService;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SchedulerProperties schedulerProperties;
    private RealFootballLiveServiceImpl service;

    @BeforeEach
    void setUp() {
        schedulerProperties = new SchedulerProperties();
        schedulerProperties.setAutoCloseGraceMinutes(0);
        schedulerProperties.setFinishFallbackAfterMinutes(180);
        service = new RealFootballLiveServiceImpl(
                eventRepository,
                apiFootballClient,
                eventService,
                eventMapper,
                new RealFootballMatchStatusMapper(),
                schedulerProperties,
                eventPublisher
        );

        lenient().when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(eventMapper.toResponse(any(Event.class))).thenReturn(new EventResponseDTO());
    }

    @Test
    void tick_whenCreatedEventIsDue_marksInProgressAndRequestsMarketClose() {
        Event event = createEvent(EventStatus.CREATED, LocalDateTime.now().minusMinutes(1));
        when(eventRepository.findDueRealFootballEvents(any())).thenReturn(List.of(event));
        when(eventRepository.findInProgressRealFootballEvents()).thenReturn(List.of());

        service.tick();

        assertEquals(EventStatus.IN_PROGRESS, event.getStatus());
        verify(eventRepository).save(event);

        ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(eventPublisher, atLeast(2)).publishEvent(eventCaptor.capture());
        assertInstanceOf(EventMarketsCloseRequestedEvent.class, eventCaptor.getAllValues().get(0));
        assertInstanceOf(EventChangeEvent.class, eventCaptor.getAllValues().get(1));
    }

    @Test
    void tick_whenLiveMatchExists_updatesScoreAndPublishesChange() {
        Event event = createEvent(EventStatus.IN_PROGRESS, LocalDateTime.now());
        MatchDto match = createMatch("api-1", "45'", "2", "1");

        when(eventRepository.findDueRealFootballEvents(any())).thenReturn(List.of());
        when(eventRepository.findInProgressRealFootballEvents()).thenReturn(List.of(event));
        when(apiFootballClient.fetchLiveEvents("28", "8")).thenReturn(List.of(match));

        service.tick();

        assertEquals(2, event.getHomeScore());
        assertEquals(1, event.getAwayScore());
        assertEquals(EventStatus.IN_PROGRESS, event.getStatus());
        verify(eventRepository).save(event);
        verify(eventPublisher).publishEvent(any(EventChangeEvent.class));
    }

    @Test
    void tick_whenLiveMatchHasNumericStatus_updatesScoreAndPublishesChange() {
        Event event = createEvent(EventStatus.IN_PROGRESS, LocalDateTime.now());
        MatchDto match = createMatch("api-1", "59", "3", "1");
        match.setMatchLive("1");

        when(eventRepository.findDueRealFootballEvents(any())).thenReturn(List.of());
        when(eventRepository.findInProgressRealFootballEvents()).thenReturn(List.of(event));
        when(apiFootballClient.fetchLiveEvents("28", "8")).thenReturn(List.of(match));

        service.tick();

        assertEquals(3, event.getHomeScore());
        assertEquals(1, event.getAwayScore());
        assertEquals(EventStatus.IN_PROGRESS, event.getStatus());
        verify(eventRepository).save(event);
        verify(eventPublisher).publishEvent(any(EventChangeEvent.class));
    }

    @Test
    void tick_whenMatchLiveFlagIsOne_updatesScoreEvenForUnknownStatus() {
        Event event = createEvent(EventStatus.IN_PROGRESS, LocalDateTime.now());
        MatchDto match = createMatch("api-1", "In Play", "3", "1");
        match.setMatchLive("1");

        when(eventRepository.findDueRealFootballEvents(any())).thenReturn(List.of());
        when(eventRepository.findInProgressRealFootballEvents()).thenReturn(List.of(event));
        when(apiFootballClient.fetchLiveEvents("28", "8")).thenReturn(List.of(match));

        service.tick();

        assertEquals(3, event.getHomeScore());
        assertEquals(1, event.getAwayScore());
        assertEquals(EventStatus.IN_PROGRESS, event.getStatus());
        verify(eventRepository).save(event);
        verify(eventPublisher).publishEvent(any(EventChangeEvent.class));
    }

    @Test
    void tick_whenLiveMatchFinished_completesEventAndPublishesCompletion() {
        Event event = createEvent(EventStatus.IN_PROGRESS, LocalDateTime.now());
        MatchDto match = createMatch("api-1", "Finished", "3", "2");

        when(eventRepository.findDueRealFootballEvents(any())).thenReturn(List.of());
        when(eventRepository.findInProgressRealFootballEvents()).thenReturn(List.of(event), List.of());
        when(apiFootballClient.fetchLiveEvents("28", "8")).thenReturn(List.of(match));

        service.tick();

        assertEquals(EventStatus.COMPLETED, event.getStatus());
        assertEquals(3, event.getHomeScore());
        assertEquals(2, event.getAwayScore());
        verify(eventPublisher).publishEvent(any(EventCompletedEvent.class));
        verify(eventPublisher).publishEvent(any(EventChangeEvent.class));
    }

    @Test
    void tick_whenLiveMatchCancelled_delegatesToEventServiceCancel() {
        Event event = createEvent(EventStatus.IN_PROGRESS, LocalDateTime.now());
        MatchDto match = createMatch("api-1", "Postponed", "0", "0");

        when(eventRepository.findDueRealFootballEvents(any())).thenReturn(List.of());
        when(eventRepository.findInProgressRealFootballEvents()).thenReturn(List.of(event), List.of());
        when(apiFootballClient.fetchLiveEvents("28", "8")).thenReturn(List.of(match));

        service.tick();

        verify(eventService).cancelEvent(10L);
    }

    @Test
    void tick_whenInProgressEventVanishesFromLiveEndpoint_fetchesByMatchIdAndCompletes() {
        Event event = createEvent(EventStatus.IN_PROGRESS, LocalDateTime.now());
        MatchDto finishedMatch = createMatch("api-1", "Finished", "3", "1");

        when(eventRepository.findDueRealFootballEvents(any())).thenReturn(List.of());
        when(eventRepository.findInProgressRealFootballEvents()).thenReturn(List.of(event));
        when(apiFootballClient.fetchLiveEvents("28", "8")).thenReturn(List.of());
        when(apiFootballClient.fetchEventsByMatchId("api-1")).thenReturn(List.of(finishedMatch));

        service.tick();

        assertEquals(EventStatus.COMPLETED, event.getStatus());
        assertEquals(3, event.getHomeScore());
        assertEquals(1, event.getAwayScore());
        verify(apiFootballClient).fetchEventsByMatchId("api-1");
        verify(eventPublisher).publishEvent(any(EventCompletedEvent.class));
        verify(eventPublisher).publishEvent(any(EventChangeEvent.class));
    }

    @Test
    void tick_whenMatchIdFallbackStillLooksLive_doesNotUpdateScore() {
        Event event = createEvent(EventStatus.IN_PROGRESS, LocalDateTime.now());
        MatchDto liveFallbackMatch = createMatch("api-1", "59", "3", "1");
        liveFallbackMatch.setMatchLive("1");

        when(eventRepository.findDueRealFootballEvents(any())).thenReturn(List.of());
        when(eventRepository.findInProgressRealFootballEvents()).thenReturn(List.of(event));
        when(apiFootballClient.fetchLiveEvents("28", "8")).thenReturn(List.of());
        when(apiFootballClient.fetchEventsByMatchId("api-1")).thenReturn(List.of(liveFallbackMatch));

        service.tick();

        assertEquals(EventStatus.IN_PROGRESS, event.getStatus());
        assertEquals(0, event.getHomeScore());
        assertEquals(0, event.getAwayScore());
        verify(apiFootballClient).fetchEventsByMatchId("api-1");
        verify(eventRepository, never()).save(event);
        verify(eventPublisher, never()).publishEvent(any(EventChangeEvent.class));
    }

    @Test
    void tick_whenMatchIdFallbackReturnsEmpty_keepsEventInProgress() {
        Event event = createEvent(EventStatus.IN_PROGRESS, LocalDateTime.now());

        when(eventRepository.findDueRealFootballEvents(any())).thenReturn(List.of());
        when(eventRepository.findInProgressRealFootballEvents()).thenReturn(List.of(event));
        when(apiFootballClient.fetchLiveEvents("28", "8")).thenReturn(List.of());
        when(apiFootballClient.fetchEventsByMatchId("api-1")).thenReturn(List.of());

        service.tick();

        assertEquals(EventStatus.IN_PROGRESS, event.getStatus());
        assertEquals(0, event.getHomeScore());
        assertEquals(0, event.getAwayScore());
        verify(apiFootballClient).fetchEventsByMatchId("api-1");
        verify(eventRepository, never()).save(event);
    }

    @Test
    void tick_whenLiveEndpointNoLongerReturnsStaleEvent_fetchesFixtureAndCompletes() {
        Event event = createEvent(EventStatus.IN_PROGRESS, LocalDateTime.now().minusHours(4));
        MatchDto finishedMatch = createMatch("api-1", "Finished", "1", "0");

        when(eventRepository.findDueRealFootballEvents(any())).thenReturn(List.of());
        when(eventRepository.findInProgressRealFootballEvents()).thenReturn(List.of(event));
        when(apiFootballClient.fetchLiveEvents("28", "8")).thenReturn(List.of());
        when(apiFootballClient.fetchEventsByLeague(eq("28"), eq("8"), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(finishedMatch));

        service.tick();

        assertEquals(EventStatus.COMPLETED, event.getStatus());
        assertEquals(1, event.getHomeScore());
        assertEquals(0, event.getAwayScore());
        verify(apiFootballClient).fetchEventsByLeague(eq("28"), eq("8"), any(LocalDate.class), any(LocalDate.class));
        verify(eventPublisher).publishEvent(any(EventCompletedEvent.class));
    }

    private Event createEvent(EventStatus status, LocalDateTime gameDatetime) {
        Competition competition = new Competition();
        competition.setId(1L);
        competition.setApiFootballLeagueId("28");
        competition.setApiFootballCountryId("8");

        Tournament tournament = new Tournament();
        tournament.setId(100L);
        tournament.setType(TournamentType.REAL_FOOTBALL);
        tournament.setCompetition(competition);

        Team home = Team.builder().id(1L).name("Brazil").abbreviation("BRA").build();
        Team away = Team.builder().id(2L).name("Argentina").abbreviation("ARG").build();

        return Event.builder()
                .id(10L)
                .tournament(tournament)
                .teamHome(home)
                .teamAway(away)
                .externalMatchId("api-1")
                .gameDatetime(gameDatetime)
                .status(status)
                .homeScore(0)
                .awayScore(0)
                .isKnockout(false)
                .build();
    }

    private MatchDto createMatch(String id, String status, String homeScore, String awayScore) {
        MatchDto match = new MatchDto();
        match.setMatchId(id);
        match.setMatchStatus(status);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        return match;
    }
}
