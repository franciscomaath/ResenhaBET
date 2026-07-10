package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.GameForecastProperties;
import com.franciscomaath.resenhaapi.controller.dto.response.GameForecastResponse;
import com.franciscomaath.resenhaapi.domain.entity.ExternalApiLog;
import com.franciscomaath.resenhaapi.domain.repository.ExternalApiLogRepository;
import com.franciscomaath.resenhaapi.service.GameForecastClient;
import com.franciscomaath.resenhaapi.service.dto.ForecastEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GameForecastClientImpl implements GameForecastClient {

    private final RestClient restClient;
    private final GameForecastProperties properties;
    private final ExternalApiLogRepository apiLogRepository;
    private final ObjectMapper objectMapper;

    @Value("${resenhabet.external-api.replay-mode:false}")
    private boolean replayMode;

    @Autowired
    public GameForecastClientImpl(GameForecastProperties properties, ExternalApiLogRepository apiLogRepository, ObjectMapper objectMapper) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("X-RapidAPI-Key", properties.getRapidapiKey())
                .defaultHeader("X-RapidAPI-Host", "game-forecast-api.p.rapidapi.com")
                .build();
        this.apiLogRepository = apiLogRepository;
        this.objectMapper = objectMapper;
    }

    GameForecastClientImpl(GameForecastProperties properties, ExternalApiLogRepository apiLogRepository,
                           ObjectMapper objectMapper, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
        this.apiLogRepository = apiLogRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<ForecastEventDto> fetchPredictions(String leagueId, int pageSize) {
        List<ForecastEventDto> allEvents = new ArrayList<>();
        int page = 1;
        boolean hasMore = true;

        while (hasMore) {
            final int currentPage = page;
            try {
                GameForecastResponse response = fetchPage(leagueId, pageSize, currentPage);

                log.info("Fetched page {} of predictions for league {}: {}", currentPage, leagueId, response);

                if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                    allEvents.addAll(response.getData());
                    hasMore = response.getData().size() == pageSize;
                    page++;
                } else {
                    hasMore = false;
                }
            } catch (Exception e) {
                log.error("Failed to fetch predictions from GameForecastAPI for league {}: {}", leagueId, e.getMessage());
                hasMore = false;
            }
        }

        return allEvents;
    }

    private GameForecastResponse fetchPage(String leagueId, int pageSize, int page) {
        String requestKey = "league_id=%s,page_size=%d,page=%d".formatted(leagueId, pageSize, page);

        if (replayMode) {
            Optional<ExternalApiLog> cached = apiLogRepository
                    .findTopByProviderAndEndpointAndRequestKeyOrderByFetchedAtDesc(
                            "GAME_FORECAST", "/events", requestKey);

            if (cached.isPresent()) {
                log.info("Replay mode: usando resposta cacheada para {}", requestKey);
                return objectMapper.readValue(cached.get().getResponseBody(), GameForecastResponse.class);
            }
            log.warn("Replay mode ON mas sem cache pra {} — chamando API real", requestKey);
        }

        GameForecastResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/events")
                        .queryParam("league_id", leagueId)
                        .queryParam("page_size", pageSize)
                        .queryParam("page", page)
                        .queryParam("include_all_history", "false")
                        .build())
                .retrieve()
                .body(GameForecastResponse.class);

        apiLogRepository.save(ExternalApiLog.builder()
                .provider("GAME_FORECAST")
                .endpoint("/events")
                .requestKey(requestKey)
                .responseBody(objectMapper.writeValueAsString(response))
                .statusCode(200)
                .fetchedAt(LocalDateTime.now())
                .build());

        return response;
    }
}
