package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.config.GameForecastProperties;
import com.franciscomaath.resenhaapi.config.OddsProperties;
import com.franciscomaath.resenhaapi.controller.dto.response.OddsImportResult;
import com.franciscomaath.resenhaapi.domain.entity.*;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.enums.MarketStatus;
import com.franciscomaath.resenhaapi.domain.enums.MarketType;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import com.franciscomaath.resenhaapi.domain.exception.BusinessException;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.repository.*;
import com.franciscomaath.resenhaapi.service.GameForecastClient;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.service.dto.ForecastEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OddsImportServiceImplTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private MarketRepository marketRepository;

    @Mock
    private OutcomeRepository outcomeRepository;

    @Mock
    private GameForecastClient gameForecastClient;

    @Mock
    private GameForecastProperties gameForecastProperties;

    @Mock
    private OddsProperties oddsProperties;

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    @InjectMocks
    private OddsImportServiceImpl oddsImportService;

    private Tournament tournament;
    private Competition competition;
    private Team homeTeam;
    private Team awayTeam;
    private Event event;

    @BeforeEach
    void setUp() {
        competition = new Competition();
        competition.setId(1L);
        competition.setGameForecastLeagueId("149");

        tournament = new Tournament();
        tournament.setId(100L);
        tournament.setType(TournamentType.REAL_FOOTBALL);
        tournament.setCompetition(competition);

        homeTeam = Team.builder().id(1L).name("Brazil").abbreviation("BRA").build();
        awayTeam = Team.builder().id(2L).name("Argentina").abbreviation("ARG").build();

        event = new Event();
        event.setId(10L);
        event.setTournament(tournament);
        event.setTeamHome(homeTeam);
        event.setTeamAway(awayTeam);
        event.setStatus(EventStatus.CREATED);
    }

    private ForecastEventDto createForecast(String homeName, String awayName,
                                             Integer homeProb, Integer drawProb, Integer awayProb) {
        ForecastEventDto dto = new ForecastEventDto();
        dto.setId("f1");

        ForecastEventDto.TeamInfo homeInfo = new ForecastEventDto.TeamInfo();
        homeInfo.setName(homeName);
        homeInfo.setId("gf1");
        dto.setTeamHome(homeInfo);

        ForecastEventDto.TeamInfo awayInfo = new ForecastEventDto.TeamInfo();
        awayInfo.setName(awayName);
        awayInfo.setId("gf2");
        dto.setTeamAway(awayInfo);

        dto.setStartAt("2026-06-15T21:00:00Z");

        ForecastEventDto.Prediction prediction = new ForecastEventDto.Prediction();

        ForecastEventDto.MatchResult matchResult = new ForecastEventDto.MatchResult();
        matchResult.setHome(homeProb);
        matchResult.setDraw(drawProb);
        matchResult.setAway(awayProb);
        prediction.setMatchResult(matchResult);

        dto.setPredictions(new ForecastEventDto.Prediction[]{prediction});
        return dto;
    }

    @Test
    void importForTournament_whenNotRealFootball_throwsBusinessException() {
        tournament.setType(TournamentType.FIFA_MATCH);
        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> oddsImportService.importForTournament(100L));
        assertTrue(ex.getMessage().contains("REAL_FOOTBALL"));
    }

    @Test
    void importForTournament_whenTournamentNotFound_throwsResourceNotFoundException() {
        when(tournamentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> oddsImportService.importForTournament(999L));
    }

    @Test
    void importForTournament_withValidForecast_createsMatchResultMarket() {
        ForecastEventDto forecast = createForecast("Brazil", "Argentina", 60, 25, 15);

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(gameForecastClient.fetchPredictions("149", 50)).thenReturn(List.of(forecast));
        when(eventRepository.findAllByTournamentId(100L)).thenReturn(List.of(event));
        when(marketRepository.findByEventIdAndMarketType(eq(10L), any())).thenReturn(Optional.empty());
        when(marketRepository.save(any(Market.class))).thenAnswer(inv -> {
            Market m = inv.getArgument(0);
            if (m.getId() == null) m.setId(1L);
            return m;
        });
        when(outcomeRepository.findByMarketId(anyLong())).thenReturn(List.of());
        when(oddsProperties.getMinOdd()).thenReturn(new BigDecimal("1.05"));
        lenient().when(gameForecastProperties.getMinExactScoreProbability()).thenReturn(2);

        OddsImportResult result = oddsImportService.importForTournament(100L);

        assertTrue(result.getMarketsCreated() > 0);
        assertTrue(result.getOutcomesCreated() > 0);

        verify(marketRepository, atLeastOnce()).save(argThat(m ->
                m.getMarketType() == MarketType.MATCH_RESULT));
    }

    @Test
    void importForTournament_createsAllMarketTypes() {
        ForecastEventDto forecast = createForecast("Brazil", "Argentina", 60, 25, 15);
        ForecastEventDto.Prediction prediction = forecast.getPredictions()[0];

        ForecastEventDto.TotalGoals totalGoals = new ForecastEventDto.TotalGoals();
        totalGoals.setOver25(55);
        totalGoals.setUnder25(45);
        totalGoals.setOver35(30);
        totalGoals.setUnder35(70);
        prediction.setTotalGoals(totalGoals);

        ForecastEventDto.BothTeamsScore btts = new ForecastEventDto.BothTeamsScore();
        btts.setYes(45);
        btts.setNo(55);
        prediction.setBothTeamsScore(btts);

        prediction.setExactScore(Map.of(
                "2_0", 15,
                "1_0", 10,
                "other", 5,
                "0_1", 1
        ));

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(gameForecastClient.fetchPredictions("149", 50)).thenReturn(List.of(forecast));
        when(eventRepository.findAllByTournamentId(100L)).thenReturn(List.of(event));
        when(marketRepository.findByEventIdAndMarketType(anyLong(), any())).thenReturn(Optional.empty());
        when(marketRepository.save(any(Market.class))).thenAnswer(inv -> {
            Market m = inv.getArgument(0);
            if (m.getId() == null) m.setId(1L);
            return m;
        });
        when(outcomeRepository.findByMarketId(anyLong())).thenReturn(List.of());
        when(oddsProperties.getMinOdd()).thenReturn(new BigDecimal("1.05"));
        when(gameForecastProperties.getMinExactScoreProbability()).thenReturn(2);

        OddsImportResult result = oddsImportService.importForTournament(100L);

        assertEquals(5, result.getMarketsCreated());
        assertTrue(result.getOutcomesCreated() >= 10);
    }

    @Test
    void importForTournament_skipsCompletedEvents() {
        event.setStatus(EventStatus.COMPLETED);

        ForecastEventDto forecast = createForecast("Brazil", "Argentina", 60, 25, 15);

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(gameForecastClient.fetchPredictions("149", 50)).thenReturn(List.of(forecast));
        when(eventRepository.findAllByTournamentId(100L)).thenReturn(List.of(event));

        OddsImportResult result = oddsImportService.importForTournament(100L);

        assertEquals(0, result.getMarketsCreated());
        verify(marketRepository, never()).save(any());
    }

    @Test
    void importForTournament_skipsForecastsWithoutPredictions() {
        ForecastEventDto forecast = createForecast("Brazil", "Argentina", 60, 25, 15);
        forecast.setPredictions(new ForecastEventDto.Prediction[]{});

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(gameForecastClient.fetchPredictions("149", 50)).thenReturn(List.of(forecast));
        when(eventRepository.findAllByTournamentId(100L)).thenReturn(List.of(event));

        OddsImportResult result = oddsImportService.importForTournament(100L);

        assertEquals(0, result.getMarketsCreated());
    }

    @Test
    void importForTournament_cachesGameForecastTeamId() {
        Team teamWithoutId = Team.builder().id(1L).name("Brazil").abbreviation("BRA").build();
        event.setTeamHome(teamWithoutId);

        ForecastEventDto forecast = createForecast("Brazil", "Argentina", 60, 25, 15);
        forecast.getTeamHome().setId("gf-brazil");

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(gameForecastClient.fetchPredictions("149", 50)).thenReturn(List.of(forecast));
        when(eventRepository.findAllByTournamentId(100L)).thenReturn(List.of(event));
        lenient().when(marketRepository.findByEventIdAndMarketType(anyLong(), any())).thenReturn(Optional.empty());
        lenient().when(marketRepository.save(any(Market.class))).thenAnswer(inv -> {
            Market m = inv.getArgument(0);
            if (m.getId() == null) m.setId(1L);
            return m;
        });
        lenient().when(outcomeRepository.findByMarketId(anyLong())).thenReturn(List.of());
        lenient().when(oddsProperties.getMinOdd()).thenReturn(new BigDecimal("1.05"));
        lenient().when(gameForecastProperties.getMinExactScoreProbability()).thenReturn(2);

        oddsImportService.importForTournament(100L);

        verify(teamRepository).save(argThat(t ->
                "gf-brazil".equals(t.getGameForecastTeamId())));
    }

    @Test
    void importForTournament_calculatesOddWithMinGuard() {
        ForecastEventDto forecast = createForecast("Brazil", "Argentina", 99, 1, 1);

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(gameForecastClient.fetchPredictions("149", 50)).thenReturn(List.of(forecast));
        when(eventRepository.findAllByTournamentId(100L)).thenReturn(List.of(event));
        when(marketRepository.findByEventIdAndMarketType(anyLong(), any())).thenReturn(Optional.empty());
        when(marketRepository.save(any(Market.class))).thenAnswer(inv -> {
            Market m = inv.getArgument(0);
            if (m.getId() == null) m.setId(1L);
            return m;
        });
        when(outcomeRepository.findByMarketId(anyLong())).thenReturn(List.of());
        when(oddsProperties.getMinOdd()).thenReturn(new BigDecimal("1.05"));
        lenient().when(gameForecastProperties.getMinExactScoreProbability()).thenReturn(2);

        OddsImportResult result = oddsImportService.importForTournament(100L);

        ArgumentCaptor<Outcome> outcomeCaptor = ArgumentCaptor.forClass(Outcome.class);
        verify(outcomeRepository, atLeast(2)).save(outcomeCaptor.capture());

        List<Outcome> savedOutcomes = outcomeCaptor.getAllValues();
        boolean allMeetMinOdd = savedOutcomes.stream()
                .allMatch(o -> o.getOdd().compareTo(new BigDecimal("1.05")) >= 0);
        assertTrue(allMeetMinOdd);
    }

    @Test
    void importForTournament_skipsNoMatchForecast() {
        ForecastEventDto forecast = createForecast("NonExistent", "Team", 60, 25, 15);

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(gameForecastClient.fetchPredictions("149", 50)).thenReturn(List.of(forecast));
        when(eventRepository.findAllByTournamentId(100L)).thenReturn(List.of(event));

        OddsImportResult result = oddsImportService.importForTournament(100L);

        assertEquals(0, result.getMarketsCreated());
        verify(marketRepository, never()).save(any());
    }

    @Test
    void importForTournament_updatesExistingOutcome() {
        ForecastEventDto forecast = createForecast("Brazil", "Argentina", 65, 20, 15);

        Market existingMarket = new Market();
        existingMarket.setId(1L);
        existingMarket.setEvent(event);
        existingMarket.setMarketType(MarketType.MATCH_RESULT);
        existingMarket.setStatus(MarketStatus.OPEN);

        Outcome existingOutcome = new Outcome();
        existingOutcome.setId(1L);
        existingOutcome.setMarket(existingMarket);
        existingOutcome.setName("Brazil");
        existingOutcome.setOdd(new BigDecimal("1.50"));

        when(tournamentRepository.findById(100L)).thenReturn(Optional.of(tournament));
        when(gameForecastClient.fetchPredictions("149", 50)).thenReturn(List.of(forecast));
        when(eventRepository.findAllByTournamentId(100L)).thenReturn(List.of(event));
        when(marketRepository.findByEventIdAndMarketType(anyLong(), any())).thenReturn(Optional.empty());
        when(marketRepository.findByEventIdAndMarketType(eq(10L), eq(MarketType.MATCH_RESULT)))
                .thenReturn(Optional.of(existingMarket));
        lenient().when(marketRepository.save(any(Market.class))).thenAnswer(inv -> {
            Market m = inv.getArgument(0);
            if (m.getId() == null) m.setId(1L);
            return m;
        });
        when(outcomeRepository.findByMarketId(anyLong())).thenAnswer(inv -> {
            Long marketId = inv.getArgument(0);
            if (marketId.equals(1L)) return List.of(existingOutcome);
            return List.of();
        });
        when(oddsProperties.getMinOdd()).thenReturn(new BigDecimal("1.05"));

        OddsImportResult result = oddsImportService.importForTournament(100L);

        assertEquals(1, result.getOddsUpdated());
        // "Empate" and "Argentina" outcomes are newly created for the MATCH_RESULT market
        assertEquals(2, result.getOutcomesCreated());
    }
}
