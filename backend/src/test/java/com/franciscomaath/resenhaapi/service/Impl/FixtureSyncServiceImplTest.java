package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.ApiFootballProperties;
import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.response.OddsImportResult;
import com.franciscomaath.resenhaapi.controller.dto.response.SyncResult;
import com.franciscomaath.resenhaapi.domain.entity.*;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.enums.PhaseType;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import com.franciscomaath.resenhaapi.domain.event.EventCompletedEvent;
import com.franciscomaath.resenhaapi.domain.event.EventMarketsCloseRequestedEvent;
import com.franciscomaath.resenhaapi.domain.exception.BusinessException;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.RoundRepository;
import com.franciscomaath.resenhaapi.domain.repository.TeamRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentRepository;
import com.franciscomaath.resenhaapi.service.ApiFootballClient;
import com.franciscomaath.resenhaapi.service.BetService;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.service.OddsImportService;
import com.franciscomaath.resenhaapi.service.dto.MatchDto;
import com.franciscomaath.resenhaapi.service.dto.StandingEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FixtureSyncServiceImplTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private RoundRepository roundRepository;

    @Mock
    private ApiFootballClient apiFootballClient;

    @Mock
    private OddsImportService oddsImportService;

    @Mock
    private ApiFootballProperties properties;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    @InjectMocks
    private FixtureSyncServiceImpl fixtureSyncService;

    private Competition createCompetition() {
        Competition competition = new Competition();
        competition.setId(1L);
        competition.setName("World Cup");
        competition.setApiFootballLeagueId("28");
        competition.setApiFootballCountryId("8");
        competition.setStartDate(LocalDateTime.of(2026, 6, 1, 0, 0));
        competition.setEndDate(LocalDateTime.of(2026, 7, 20, 23, 59));
        return competition;
    }

    private Tournament createRealFootballTournament() {
        Tournament tournament = new Tournament();
        tournament.setId(100L);
        tournament.setType(TournamentType.REAL_FOOTBALL);
        tournament.setCompetition(createCompetition());
        return tournament;
    }

    private MatchDto createMatchDto(String matchId, String homeTeam, String homeId,
                                     String awayTeam, String awayId, String status,
                                     String stageName, String matchRound) {
        MatchDto dto = new MatchDto();
        dto.setMatchId(matchId);
        dto.setHomeTeamName(homeTeam);
        dto.setHomeTeamId(homeId);
        dto.setAwayTeamName(awayTeam);
        dto.setAwayTeamId(awayId);
        dto.setMatchStatus(status);
        dto.setStageName(stageName);
        dto.setMatchRound(matchRound);
        dto.setMatchDate("2026-06-15");
        dto.setMatchTime("21:00");
        dto.setHomeScore("0");
        dto.setAwayScore("0");
        return dto;
    }

    @Test
    void sync_whenNotRealFootball_throwsBusinessException() {
        Tournament tournament = new Tournament();
        tournament.setId(100L);
        tournament.setType(TournamentType.FIFA_MATCH);

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> fixtureSyncService.sync(100L));
        assertTrue(ex.getMessage().contains("REAL_FOOTBALL"));
    }

    @Test
    void sync_whenTournamentNotFound_throwsResourceNotFoundException() {
        when(tournamentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> fixtureSyncService.sync(999L));
    }

    @Test
    void sync_withNewFixtures_createsEventsAndTeamsAndRounds() {
        Tournament tournament = createRealFootballTournament();

        MatchDto match = createMatchDto("1", "Brazil", "10", "Argentina", "20",
                "", "Group Stage", null);

        StandingEntry standing = new StandingEntry();
        standing.setTeamId("10");
        standing.setLeagueRound("Group A");

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(apiFootballClient.fetchEventsByLeague(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(match));
        when(apiFootballClient.getStandings(anyString()))
                .thenReturn(List.of(standing));
        when(eventRepository.findByTournamentIdAndExternalMatchIdIn(eq(100L), anyList())).thenReturn(List.of());
        when(teamRepository.findByApiFootballTeamId(anyString())).thenReturn(Optional.empty());
        when(teamRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(1L);
            return t;
        });
        when(roundRepository.findByTournamentIdAndName(eq(100L), anyString()))
                .thenReturn(Optional.empty());
        when(roundRepository.save(any(TournamentRound.class))).thenAnswer(invocation -> {
            TournamentRound r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(1L);
            return r;
        });
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(1L);
            return e;
        });
        OddsImportResult oddsResult = new OddsImportResult();
        oddsResult.setMarketsCreated(6);
        when(oddsImportService.importForTournament(100L)).thenReturn(oddsResult);

        SyncResult result = fixtureSyncService.sync(100L);

        assertEquals(1, result.getEventsCreated());
        assertEquals(0, result.getEventsUpdated());
        assertEquals(2, result.getTeamsLinked());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, atLeastOnce()).save(eventCaptor.capture());
        Event savedEvent = eventCaptor.getValue();
        assertNotNull(savedEvent.getExternalMatchId());
        assertEquals("1", savedEvent.getExternalMatchId());
        assertEquals("Brazil", savedEvent.getTeamHome().getName());
        assertEquals("Argentina", savedEvent.getTeamAway().getName());
    }

    @Test
    void sync_withKnockoutFixture_usesStageNameWhenMatchRoundIsBlank() {
        Tournament tournament = createRealFootballTournament();

        MatchDto match = createMatchDto("769878", "South Africa", "726", "Canada", "512",
                "Not Started", "1/16-finals", "");

        StandingEntry homeStanding = new StandingEntry();
        homeStanding.setTeamId("726");
        homeStanding.setLeagueRound("Group A");

        StandingEntry awayStanding = new StandingEntry();
        awayStanding.setTeamId("512");
        awayStanding.setLeagueRound("Group B");

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(apiFootballClient.fetchEventsByLeague(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(match));
        when(apiFootballClient.getStandings(anyString()))
                .thenReturn(List.of(homeStanding, awayStanding));
        when(eventRepository.findByTournamentIdAndExternalMatchIdIn(eq(100L), anyList())).thenReturn(List.of());
        when(teamRepository.findByApiFootballTeamId(anyString())).thenReturn(Optional.empty());
        when(teamRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(1L);
            return t;
        });
        when(roundRepository.findByTournamentIdAndName(eq(100L), anyString()))
                .thenReturn(Optional.empty());
        when(roundRepository.save(any(TournamentRound.class))).thenAnswer(invocation -> {
            TournamentRound r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(1L);
            return r;
        });
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OddsImportResult oddsResult = new OddsImportResult();
        oddsResult.setMarketsCreated(0);
        when(oddsImportService.importForTournament(100L)).thenReturn(oddsResult);

        SyncResult result = fixtureSyncService.sync(100L);

        assertEquals(1, result.getEventsCreated());
        assertEquals(1, result.getRoundsCreated());

        ArgumentCaptor<TournamentRound> roundCaptor = ArgumentCaptor.forClass(TournamentRound.class);
        verify(roundRepository).save(roundCaptor.capture());
        assertEquals("1/16-finals", roundCaptor.getValue().getName());
        assertEquals(PhaseType.KNOCKOUT, roundCaptor.getValue().getPhaseType());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());
        assertNotNull(eventCaptor.getValue().getRound());
        assertEquals("1/16-finals", eventCaptor.getValue().getRound().getName());
    }

    @Test
    void sync_withExistingFixtures_updatesEvents() {
        Tournament tournament = createRealFootballTournament();

        Team homeTeam = Team.builder().id(1L).name("Brazil").abbreviation("BRA").build();
        Team awayTeam = Team.builder().id(2L).name("Argentina").abbreviation("ARG").build();

        Event existingEvent = Event.builder()
                .id(10L)
                .tournament(tournament)
                .externalMatchId("1")
                .teamHome(homeTeam)
                .teamAway(awayTeam)
                .status(EventStatus.CREATED)
                .build();

        MatchDto match = createMatchDto("1", "Brazil", "10", "Argentina", "20",
                "Finished", "Group Stage", null);
        match.setHomeScore("2");
        match.setAwayScore("1");

        StandingEntry standing = new StandingEntry();
        standing.setTeamId("10");
        standing.setLeagueRound("Group A");

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(apiFootballClient.fetchEventsByLeague(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(match));
        when(apiFootballClient.getStandings(anyString()))
                .thenReturn(List.of(standing));
        when(eventRepository.findByTournamentIdAndExternalMatchIdIn(100L, List.of("1")))
                .thenReturn(List.of(existingEvent));
        when(teamRepository.findByApiFootballTeamId(anyString())).thenReturn(Optional.empty());
        when(teamRepository.findByName("Brazil")).thenReturn(Optional.of(homeTeam));
        when(teamRepository.findByName("Argentina")).thenReturn(Optional.of(awayTeam));
        when(roundRepository.findByTournamentIdAndName(eq(100L), anyString()))
                .thenReturn(Optional.empty());
        when(roundRepository.save(any(TournamentRound.class))).thenAnswer(invocation -> {
            TournamentRound r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(1L);
            return r;
        });
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SyncResult result = fixtureSyncService.sync(100L);

        assertEquals(0, result.getEventsCreated());
        assertEquals(1, result.getEventsUpdated());
        assertEquals(EventStatus.COMPLETED, existingEvent.getStatus());
        assertEquals(2, existingEvent.getHomeScore());
        assertEquals(1, existingEvent.getAwayScore());
    }

    @Test
    void sync_newlyCompletedEvent_publishesEventCompletedEvent() {
        Tournament tournament = createRealFootballTournament();

        Team homeTeam = Team.builder().id(1L).name("Brazil").abbreviation("BRA").build();
        Team awayTeam = Team.builder().id(2L).name("Argentina").abbreviation("ARG").build();

        Event existingEvent = Event.builder()
                .id(10L)
                .tournament(tournament)
                .externalMatchId("1")
                .teamHome(homeTeam)
                .teamAway(awayTeam)
                .status(EventStatus.IN_PROGRESS)
                .build();

        MatchDto match = createMatchDto("1", "Brazil", "10", "Argentina", "20",
                "Finished", "Group Stage", null);
        match.setHomeScore("1");
        match.setAwayScore("0");

        StandingEntry standing = new StandingEntry();
        standing.setTeamId("10");
        standing.setLeagueRound("Group A");

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(apiFootballClient.fetchEventsByLeague(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(match));
        when(apiFootballClient.getStandings(anyString()))
                .thenReturn(List.of(standing));
        when(eventRepository.findByTournamentIdAndExternalMatchIdIn(100L, List.of("1")))
                .thenReturn(List.of(existingEvent));
        when(teamRepository.findByApiFootballTeamId(anyString())).thenReturn(Optional.empty());
        when(teamRepository.findByName("Brazil")).thenReturn(Optional.of(homeTeam));
        when(teamRepository.findByName("Argentina")).thenReturn(Optional.of(awayTeam));
        when(roundRepository.findByTournamentIdAndName(eq(100L), anyString()))
                .thenReturn(Optional.empty());
        when(roundRepository.save(any(TournamentRound.class))).thenAnswer(invocation -> {
            TournamentRound r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(1L);
            return r;
        });
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        fixtureSyncService.sync(100L);

        verify(applicationEventPublisher).publishEvent(any(EventCompletedEvent.class));
    }

    @Test
    void sync_existingCreatedEventBecomesInProgress_publishesEventMarketsCloseRequestedEvent() {
        Tournament tournament = createRealFootballTournament();

        Team homeTeam = Team.builder().id(1L).name("Brazil").abbreviation("BRA").build();
        Team awayTeam = Team.builder().id(2L).name("Argentina").abbreviation("ARG").build();

        Event existingEvent = Event.builder()
                .id(10L)
                .tournament(tournament)
                .externalMatchId("1")
                .teamHome(homeTeam)
                .teamAway(awayTeam)
                .status(EventStatus.CREATED)
                .build();

        MatchDto match = createMatchDto("1", "Brazil", "10", "Argentina", "20",
                "45'", "Group Stage", null);
        match.setHomeScore("1");
        match.setAwayScore("0");

        StandingEntry standing = new StandingEntry();
        standing.setTeamId("10");
        standing.setLeagueRound("Group A");

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(apiFootballClient.fetchEventsByLeague(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(match));
        when(apiFootballClient.getStandings(anyString()))
                .thenReturn(List.of(standing));
        when(eventRepository.findByTournamentIdAndExternalMatchIdIn(100L, List.of("1")))
                .thenReturn(List.of(existingEvent));
        when(teamRepository.findByApiFootballTeamId(anyString())).thenReturn(Optional.empty());
        when(teamRepository.findByName("Brazil")).thenReturn(Optional.of(homeTeam));
        when(teamRepository.findByName("Argentina")).thenReturn(Optional.of(awayTeam));
        when(roundRepository.findByTournamentIdAndName(eq(100L), anyString()))
                .thenReturn(Optional.empty());
        when(roundRepository.save(any(TournamentRound.class))).thenAnswer(invocation -> {
            TournamentRound r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(1L);
            return r;
        });
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        fixtureSyncService.sync(100L);

        ArgumentCaptor<EventMarketsCloseRequestedEvent> eventCaptor =
                ArgumentCaptor.forClass(EventMarketsCloseRequestedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(10L, eventCaptor.getValue().getEventId());
    }

    @Test
    void sync_existingInProgressEventRemainsInProgress_doesNotPublishEventMarketsCloseRequestedEvent() {
        Tournament tournament = createRealFootballTournament();

        Team homeTeam = Team.builder().id(1L).name("Brazil").abbreviation("BRA").build();
        Team awayTeam = Team.builder().id(2L).name("Argentina").abbreviation("ARG").build();

        Event existingEvent = Event.builder()
                .id(10L)
                .tournament(tournament)
                .externalMatchId("1")
                .teamHome(homeTeam)
                .teamAway(awayTeam)
                .status(EventStatus.IN_PROGRESS)
                .build();

        MatchDto match = createMatchDto("1", "Brazil", "10", "Argentina", "20",
                "45'", "Group Stage", null);

        StandingEntry standing = new StandingEntry();
        standing.setTeamId("10");
        standing.setLeagueRound("Group A");

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(apiFootballClient.fetchEventsByLeague(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(match));
        when(apiFootballClient.getStandings(anyString()))
                .thenReturn(List.of(standing));
        when(eventRepository.findByTournamentIdAndExternalMatchIdIn(100L, List.of("1")))
                .thenReturn(List.of(existingEvent));
        when(teamRepository.findByApiFootballTeamId(anyString())).thenReturn(Optional.empty());
        when(teamRepository.findByName("Brazil")).thenReturn(Optional.of(homeTeam));
        when(teamRepository.findByName("Argentina")).thenReturn(Optional.of(awayTeam));
        when(roundRepository.findByTournamentIdAndName(eq(100L), anyString()))
                .thenReturn(Optional.empty());
        when(roundRepository.save(any(TournamentRound.class))).thenAnswer(invocation -> {
            TournamentRound r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(1L);
            return r;
        });
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        fixtureSyncService.sync(100L);

        verify(applicationEventPublisher, never()).publishEvent(isA(EventMarketsCloseRequestedEvent.class));
    }

    @Test
    void sync_sameGroupMatch_createsGroupStageRound() {
        Tournament tournament = createRealFootballTournament();

        MatchDto match = createMatchDto("1", "Brazil", "10", "Argentina", "20",
                "", "Group Stage", null);

        StandingEntry homeStanding = new StandingEntry();
        homeStanding.setTeamId("10");
        homeStanding.setLeagueRound("Group A");

        StandingEntry awayStanding = new StandingEntry();
        awayStanding.setTeamId("20");
        awayStanding.setLeagueRound("Group A");

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(apiFootballClient.fetchEventsByLeague(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(match));
        when(apiFootballClient.getStandings(anyString()))
                .thenReturn(List.of(homeStanding, awayStanding));
        when(eventRepository.findByTournamentIdAndExternalMatchIdIn(eq(100L), anyList())).thenReturn(List.of());
        when(teamRepository.findByApiFootballTeamId(anyString())).thenReturn(Optional.empty());
        when(teamRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(1L);
            return t;
        });
        when(roundRepository.findByTournamentIdAndName(eq(100L), anyString()))
                .thenReturn(Optional.empty());
        when(roundRepository.save(any(TournamentRound.class))).thenAnswer(invocation -> {
            TournamentRound r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(1L);
            return r;
        });
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(1L);
            return e;
        });
        OddsImportResult oddsResult = new OddsImportResult();
        when(oddsImportService.importForTournament(100L)).thenReturn(oddsResult);

        fixtureSyncService.sync(100L);

        ArgumentCaptor<TournamentRound> roundCaptor = ArgumentCaptor.forClass(TournamentRound.class);
        verify(roundRepository).save(roundCaptor.capture());
        assertEquals("Group A", roundCaptor.getValue().getName());
        assertEquals(com.franciscomaath.resenhaapi.domain.enums.PhaseType.GROUP_STAGE,
                roundCaptor.getValue().getPhaseType());

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, atLeastOnce()).save(eventCaptor.capture());
        assertFalse(eventCaptor.getValue().getIsKnockout());
    }

    @Test
    void sync_crossGroupMatch_marksEventAsKnockout() {
        Tournament tournament = createRealFootballTournament();

        MatchDto match = createMatchDto("1", "Brazil", "10", "Argentina", "20",
                "", "Playoffs", "Round of 16");

        StandingEntry homeStanding = new StandingEntry();
        homeStanding.setTeamId("10");
        homeStanding.setLeagueRound("Group A");

        StandingEntry awayStanding = new StandingEntry();
        awayStanding.setTeamId("20");
        awayStanding.setLeagueRound("Group B");

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(apiFootballClient.fetchEventsByLeague(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(match));
        when(apiFootballClient.getStandings(anyString()))
                .thenReturn(List.of(homeStanding, awayStanding));
        when(eventRepository.findByTournamentIdAndExternalMatchIdIn(eq(100L), anyList())).thenReturn(List.of());
        when(teamRepository.findByApiFootballTeamId(anyString())).thenReturn(Optional.empty());
        when(teamRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(1L);
            return t;
        });
        when(roundRepository.findByTournamentIdAndName(eq(100L), anyString()))
                .thenReturn(Optional.empty());
        when(roundRepository.save(any(TournamentRound.class))).thenAnswer(invocation -> {
            TournamentRound r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(1L);
            return r;
        });
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(1L);
            return e;
        });
        when(oddsImportService.importForTournament(100L)).thenReturn(new OddsImportResult());

        fixtureSyncService.sync(100L);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, atLeastOnce()).save(eventCaptor.capture());
        assertTrue(eventCaptor.getValue().getIsKnockout());
    }

    @Test
    void sync_withNonNumericApiFootballTeamIds_usesIdsAsOpaqueStrings() {
        Tournament tournament = createRealFootballTournament();

        MatchDto match = createMatchDto("1", "Brazil", "api-home", "Argentina", "api-away",
                "", "Group Stage", null);

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(apiFootballClient.fetchEventsByLeague(anyString(), anyString(), any(), any()))
                .thenReturn(List.of(match));
        when(apiFootballClient.getStandings(anyString())).thenReturn(List.of());
        when(eventRepository.findByTournamentIdAndExternalMatchIdIn(eq(100L), anyList())).thenReturn(List.of());
        when(teamRepository.findByApiFootballTeamId(anyString())).thenReturn(Optional.empty());
        when(teamRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team t = invocation.getArgument(0);
            if (t.getId() == null) t.setId(1L);
            return t;
        });
        when(roundRepository.findByTournamentIdAndName(eq(100L), anyString()))
                .thenReturn(Optional.empty());
        when(roundRepository.save(any(TournamentRound.class))).thenAnswer(invocation -> {
            TournamentRound r = invocation.getArgument(0);
            if (r.getId() == null) r.setId(1L);
            return r;
        });
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            if (e.getId() == null) e.setId(1L);
            return e;
        });
        when(oddsImportService.importForTournament(100L)).thenReturn(new OddsImportResult());

        fixtureSyncService.sync(100L);

        verify(teamRepository).findByApiFootballTeamId("api-home");
        verify(teamRepository).findByApiFootballTeamId("api-away");
    }
}
