package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.ApiFootballProperties;
import com.franciscomaath.resenhaapi.domain.entity.ExternalApiLog;
import com.franciscomaath.resenhaapi.domain.repository.ExternalApiLogRepository;
import com.franciscomaath.resenhaapi.service.ApiFootballClient;
import com.franciscomaath.resenhaapi.service.dto.MatchDto;
import com.franciscomaath.resenhaapi.service.dto.StandingEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
public class ApiFootballClientImpl implements ApiFootballClient {

    private final RestClient restClient;
    private final ApiFootballProperties properties;
    private final ExternalApiLogRepository apiLogRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public ApiFootballClientImpl(ApiFootballProperties properties, ExternalApiLogRepository apiLogRepository, ObjectMapper objectMapper) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
        this.apiLogRepository = apiLogRepository;
        this.objectMapper = objectMapper;
    }

    ApiFootballClientImpl(ApiFootballProperties properties, ExternalApiLogRepository apiLogRepository,
                          ObjectMapper objectMapper, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
        this.apiLogRepository = apiLogRepository;
        this.objectMapper = objectMapper;
    }

    private String fetchRawJson(Function<UriBuilder, URI> uriFunction, String endpoint, String requestKey) {
        String rawJson = restClient.get()
                .uri(uriFunction)
                .retrieve()
                .body(String.class);

        if (rawJson != null && !rawJson.isBlank()) {
            apiLogRepository.save(ExternalApiLog.builder()
                    .provider("API_FOOTBALL")
                    .endpoint(endpoint)
                    .requestKey(requestKey)
                    .responseBody(rawJson)
                    .statusCode(200)
                    .fetchedAt(LocalDateTime.now())
                    .build());
        }

        return rawJson;
    }

    @Override
    public List<MatchDto> fetchEventsByLeague(String leagueId, String countryId, LocalDate from, LocalDate to) {
        String requestKey = "league_id=%s,country_id=%s,from=%s,to=%s".formatted(leagueId, countryId, from, to);
        try {
            String rawJson = fetchRawJson(uriBuilder -> uriBuilder
                    .queryParam("action", "get_events")
                    .queryParam("timezone", "America/Fortaleza")
                    .queryParam("league_id", leagueId)
                    .queryParam("country_id", countryId)
                    .queryParam("from", from.toString())
                    .queryParam("to", to.toString())
                    .queryParam("APIkey", properties.getKey())
                    .build(), "get_events", requestKey);

            MatchDto[] response = rawJson != null ? objectMapper.readValue(rawJson, MatchDto[].class) : null;
            return response != null ? Arrays.asList(response) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch events from APIfootball for league {}: {}", leagueId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<MatchDto> fetchLiveEvents(String leagueId, String countryId) {
        String requestKey = "league_id=%s,country_id=%s,match_live=1".formatted(leagueId, countryId);
        try {
            String rawJson = fetchRawJson(uriBuilder -> uriBuilder
                    .queryParam("action", "get_events")
                    .queryParam("league_id", leagueId)
                    .queryParam("country_id", countryId)
                    .queryParam("match_live", "1")
                    .queryParam("timezone", "America/Fortaleza")
                    .queryParam("APIkey", properties.getKey())
                    .build(), "get_events_live", requestKey);

            MatchDto[] response = rawJson != null ? objectMapper.readValue(rawJson, MatchDto[].class) : null;
            return response != null ? Arrays.asList(response) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch live events from APIfootball for league {}: {}", leagueId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<MatchDto> fetchEventsByMatchId(String matchId) {
        String requestKey = "match_id=%s".formatted(matchId);
        try {
            String rawJson = fetchRawJson(uriBuilder -> uriBuilder
                    .queryParam("action", "get_events")
                    .queryParam("match_id", matchId)
                    .queryParam("timezone", "America/Fortaleza")
                    .queryParam("APIkey", properties.getKey())
                    .build(), "get_events_match_id", requestKey);

            MatchDto[] response = rawJson != null ? objectMapper.readValue(rawJson, MatchDto[].class) : null;
            return response != null ? Arrays.asList(response) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch event from APIfootball for match {}: {}", matchId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public List<StandingEntry> getStandings(String leagueId) {
        String requestKey = "league_id=%s".formatted(leagueId);
        try {
            String rawJson = fetchRawJson(uriBuilder -> uriBuilder
                    .queryParam("action", "get_standings")
                    .queryParam("league_id", leagueId)
                    .queryParam("APIkey", properties.getKey())
                    .build(), "get_standings", requestKey);

            StandingEntry[] response = rawJson != null ? objectMapper.readValue(rawJson, StandingEntry[].class) : null;
            return response != null ? Arrays.asList(response) : Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to fetch standings from APIfootball for league {}: {}", leagueId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
