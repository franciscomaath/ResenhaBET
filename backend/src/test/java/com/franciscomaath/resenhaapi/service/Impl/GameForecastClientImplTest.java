package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.GameForecastProperties;
import com.franciscomaath.resenhaapi.controller.dto.response.GameForecastResponse;
import com.franciscomaath.resenhaapi.domain.entity.ExternalApiLog;
import com.franciscomaath.resenhaapi.domain.repository.ExternalApiLogRepository;
import com.franciscomaath.resenhaapi.service.dto.ForecastEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameForecastClientImplTest {

    @Mock
    private GameForecastProperties properties;

    @Mock
    private ExternalApiLogRepository apiLogRepository;

    @Mock
    private ObjectMapper objectMapper;

    private GameForecastClientImpl gameForecastClient;

    private RestClient.ResponseSpec responseSpec;

    @BeforeEach
    @SuppressWarnings({"unchecked", "rawtypes"})
    void setUp() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        gameForecastClient = new GameForecastClientImpl(properties, apiLogRepository, objectMapper, restClient);

        lenient().when(restClient.get()).thenReturn(uriSpec);
        lenient().when(uriSpec.uri(any(Function.class))).thenReturn(headersSpec);
        lenient().when(headersSpec.retrieve()).thenReturn(responseSpec);
    }

    @Test
    void fetchPredictions_singlePage_returnsEvents() {
        ForecastEventDto event = new ForecastEventDto();
        event.setId("1");

        GameForecastResponse response = new GameForecastResponse();
        response.setData(List.of(event));

        when(responseSpec.body(GameForecastResponse.class)).thenReturn(response);

        List<ForecastEventDto> result = gameForecastClient.fetchPredictions("149", 50);

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());

        ArgumentCaptor<ExternalApiLog> logCaptor = ArgumentCaptor.forClass(ExternalApiLog.class);
        verify(apiLogRepository).save(logCaptor.capture());
        assertEquals("GAME_FORECAST", logCaptor.getValue().getProvider());
        assertEquals("/events", logCaptor.getValue().getEndpoint());
    }

    @Test
    void fetchPredictions_paginatesWhenResponseEqualsPageSize() {
        GameForecastResponse page1 = new GameForecastResponse();
        page1.setData(List.of(new ForecastEventDto(), new ForecastEventDto()));

        GameForecastResponse page2 = new GameForecastResponse();
        page2.setData(List.of(new ForecastEventDto()));

        when(responseSpec.body(GameForecastResponse.class)).thenReturn(page1, page2);

        List<ForecastEventDto> result = gameForecastClient.fetchPredictions("149", 2);

        assertEquals(3, result.size());
        verify(apiLogRepository, times(2)).save(any(ExternalApiLog.class));
    }

    @Test
    void fetchPredictions_whenException_returnsPartialResults() {
        GameForecastResponse page1 = new GameForecastResponse();
        page1.setData(List.of(new ForecastEventDto()));

        when(responseSpec.body(GameForecastResponse.class))
                .thenReturn(page1)
                .thenThrow(new RuntimeException("API error"));

        List<ForecastEventDto> result = gameForecastClient.fetchPredictions("149", 1);

        assertEquals(1, result.size());
    }

    @Test
    void fetchPredictions_whenEmptyResponse_returnsEmptyList() {
        GameForecastResponse response = new GameForecastResponse();
        response.setData(List.of());

        when(responseSpec.body(GameForecastResponse.class)).thenReturn(response);

        List<ForecastEventDto> result = gameForecastClient.fetchPredictions("149", 50);

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchPredictions_replayMode_usesCachedResponse() throws Exception {
        ReflectionTestUtils.setField(gameForecastClient, "replayMode", true);

        String cachedJson = "{\"data\":[{\"id\":\"1\"}]}";
        GameForecastResponse cachedResponse = new GameForecastResponse();
        ForecastEventDto cachedEvent = new ForecastEventDto();
        cachedEvent.setId("1");
        cachedResponse.setData(List.of(cachedEvent));

        ExternalApiLog cachedLog = ExternalApiLog.builder()
                .responseBody(cachedJson)
                .build();

        when(apiLogRepository.findTopByProviderAndEndpointAndRequestKeyOrderByFetchedAtDesc(
                "GAME_FORECAST", "/events", "league_id=149,page_size=50,page=1"))
                .thenReturn(Optional.of(cachedLog));
        when(objectMapper.readValue(cachedJson, GameForecastResponse.class)).thenReturn(cachedResponse);

        List<ForecastEventDto> result = gameForecastClient.fetchPredictions("149", 50);

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getId());
        verify(responseSpec, never()).body(GameForecastResponse.class);
    }
}
