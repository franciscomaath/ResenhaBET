package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.ApiFootballProperties;
import com.franciscomaath.resenhaapi.domain.entity.ExternalApiLog;
import com.franciscomaath.resenhaapi.domain.repository.ExternalApiLogRepository;
import com.franciscomaath.resenhaapi.service.dto.MatchDto;
import com.franciscomaath.resenhaapi.service.dto.StandingEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiFootballClientImplTest {

    @Mock
    private ApiFootballProperties properties;

    @Mock
    private ExternalApiLogRepository apiLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    private ApiFootballClientImpl apiFootballClient;
    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        apiFootballClient = new ApiFootballClientImpl(properties, apiLogRepository, objectMapper, restClient);

        lenient().when(restClient.get()).thenReturn(uriSpec);
        lenient().when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
        lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);
        lenient().when(responseSpec.body(String.class)).thenReturn("{}");
    }

    @Test
    void fetchEventsByLeague_shouldReturnParsedMatches() throws Exception {
        String rawJson = "[{\"match_id\":\"1\",\"match_hometeam_name\":\"Brazil\"}]";
        when(responseSpec.body(String.class)).thenReturn(rawJson);

        MatchDto match = new MatchDto();
        match.setMatchId("1");
        match.setHomeTeamName("Brazil");

        when(objectMapper.readValue(rawJson, MatchDto[].class)).thenReturn(new MatchDto[]{match});

        List<MatchDto> result = apiFootballClient.fetchEventsByLeague("28", "8",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 20));

        assertEquals(1, result.size());
        assertEquals("Brazil", result.get(0).getHomeTeamName());

        ArgumentCaptor<ExternalApiLog> logCaptor = ArgumentCaptor.forClass(ExternalApiLog.class);
        verify(apiLogRepository).save(logCaptor.capture());
        assertEquals("API_FOOTBALL", logCaptor.getValue().getProvider());
        assertEquals("get_events", logCaptor.getValue().getEndpoint());
    }

    @Test
    void fetchEventsByLeague_whenException_shouldReturnEmptyList() {
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("API error"));

        List<MatchDto> result = apiFootballClient.fetchEventsByLeague("28", "8",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 20));

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchEventsByLeague_whenResponseIsNull_shouldReturnEmptyList() throws Exception {
        when(responseSpec.body(String.class)).thenReturn(null);

        List<MatchDto> result = apiFootballClient.fetchEventsByLeague("28", "8",
                LocalDate.of(2026, 6, 1), LocalDate.of(2026, 7, 20));

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchLiveEvents_shouldReturnParsedMatches() throws Exception {
        String rawJson = "[{\"match_id\":\"2\",\"match_hometeam_name\":\"Brazil\"}]";
        when(responseSpec.body(String.class)).thenReturn(rawJson);

        MatchDto match = new MatchDto();
        match.setMatchId("2");
        when(objectMapper.readValue(rawJson, MatchDto[].class)).thenReturn(new MatchDto[]{match});

        List<MatchDto> result = apiFootballClient.fetchLiveEvents("28", "8");

        assertEquals(1, result.size());

        ArgumentCaptor<ExternalApiLog> logCaptor = ArgumentCaptor.forClass(ExternalApiLog.class);
        verify(apiLogRepository).save(logCaptor.capture());
        assertEquals("get_events_live", logCaptor.getValue().getEndpoint());
    }

    @Test
    void fetchLiveEvents_whenException_shouldReturnEmptyList() {
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("API error"));

        List<MatchDto> result = apiFootballClient.fetchLiveEvents("28", "8");

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchEventsByMatchId_shouldReturnParsedMatches() throws Exception {
        String rawJson = "[{\"match_id\":\"710313\",\"match_hometeam_name\":\"Norway\"}]";
        when(responseSpec.body(String.class)).thenReturn(rawJson);

        MatchDto match = new MatchDto();
        match.setMatchId("710313");
        match.setHomeTeamName("Norway");
        when(objectMapper.readValue(rawJson, MatchDto[].class)).thenReturn(new MatchDto[]{match});

        List<MatchDto> result = apiFootballClient.fetchEventsByMatchId("710313");

        assertEquals(1, result.size());
        assertEquals("Norway", result.get(0).getHomeTeamName());

        ArgumentCaptor<ExternalApiLog> logCaptor = ArgumentCaptor.forClass(ExternalApiLog.class);
        verify(apiLogRepository).save(logCaptor.capture());
        assertEquals("get_events_match_id", logCaptor.getValue().getEndpoint());
        assertEquals("match_id=710313", logCaptor.getValue().getRequestKey());
    }

    @Test
    void fetchEventsByMatchId_whenException_shouldReturnEmptyList() {
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("API error"));

        List<MatchDto> result = apiFootballClient.fetchEventsByMatchId("710313");

        assertTrue(result.isEmpty());
    }

    @Test
    void getStandings_shouldReturnParsedEntries() throws Exception {
        String rawJson = "[{\"team_id\":\"3\",\"league_round\":\"Group A\"}]";
        when(responseSpec.body(String.class)).thenReturn(rawJson);

        StandingEntry entry = new StandingEntry();
        entry.setTeamId("3");
        entry.setLeagueRound("Group A");
        when(objectMapper.readValue(rawJson, StandingEntry[].class)).thenReturn(new StandingEntry[]{entry});

        List<StandingEntry> result = apiFootballClient.getStandings("28");

        assertEquals(1, result.size());
        assertEquals("Group A", result.get(0).getLeagueRound());

        ArgumentCaptor<ExternalApiLog> logCaptor = ArgumentCaptor.forClass(ExternalApiLog.class);
        verify(apiLogRepository).save(logCaptor.capture());
        assertEquals("get_standings", logCaptor.getValue().getEndpoint());
    }

    @Test
    void getStandings_whenException_shouldReturnEmptyList() {
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("API error"));

        List<StandingEntry> result = apiFootballClient.getStandings("28");

        assertTrue(result.isEmpty());
    }
}
