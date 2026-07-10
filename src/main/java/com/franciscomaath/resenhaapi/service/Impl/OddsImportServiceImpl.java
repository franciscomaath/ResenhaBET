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
import com.franciscomaath.resenhaapi.service.OddsImportService;
import com.franciscomaath.resenhaapi.service.dto.ForecastEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OddsImportServiceImpl implements OddsImportService {

    private final TournamentRepository tournamentRepository;
    private final EventRepository eventRepository;
    private final MarketRepository marketRepository;
    private final OutcomeRepository outcomeRepository;
    private final GameForecastClient gameForecastClient;
    private final GameForecastProperties gameForecastProperties;
    private final OddsProperties oddsProperties;
    private final TeamRepository teamRepository;
    private final CurrentUserContext currentUserContext;
    private final GroupAuthorizationService groupAuthorizationService;

    @Override
    @Transactional
    public OddsImportResult importForTournament(Long tournamentId) {
        currentUserContext.requireAdmin();
        groupAuthorizationService.requireTournamentAccess(tournamentId);

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));

        if (tournament.getType() != TournamentType.REAL_FOOTBALL) {
            throw new BusinessException("Only REAL_FOOTBALL tournaments support odds import.");
        }

        Competition competition = tournament.getCompetition();

        String leagueId = competition.getGameForecastLeagueId();
        List<ForecastEventDto> forecasts = gameForecastClient.fetchPredictions(leagueId, 50);

        OddsImportResult result = new OddsImportResult();

        List<Event> events = eventRepository.findAllByTournamentId(tournamentId);

        log.info("Candidate events for tournament {}: {}", tournamentId,
                events.stream()
                        .map(e -> e.getId() + ":" + e.getStatus() + ":" + e.getTeamHome().getName() + " vs " + e.getTeamAway().getName())
                        .toList());

        for (ForecastEventDto forecast : forecasts) {
            Event matchedEvent = findMatchingEvent(events, forecast);
            if (matchedEvent == null) {
                log.warn("No matching event found for forecast: {} vs {}",
                        forecast.getTeamHome() != null ? forecast.getTeamHome().getName() : "unknown",
                        forecast.getTeamAway() != null ? forecast.getTeamAway().getName() : "unknown");
                continue;
            }

            if (matchedEvent.getStatus() == EventStatus.COMPLETED) {
                continue;
            }

            if (forecast.getPredictions() == null || forecast.getPredictions().length == 0) {
                log.warn("No predictions available for event: {}", matchedEvent.getId());
                continue;
            }

            ForecastEventDto.Prediction prediction = forecast.getPredictions()[0];
            importMarketsForEvent(matchedEvent, prediction, result);
        }

        log.info("Odds import completed for tournament {}: {} markets created, {} markets found, {} outcomes created, {} odds updated",
                tournamentId, result.getMarketsCreated(), result.getMarketsUpdated(), result.getOutcomesCreated(), result.getOddsUpdated());

        return result;
    }

    private Event findMatchingEvent(List<Event> events, ForecastEventDto forecast) {
        String homeTeamName = forecast.getTeamHome() != null ? forecast.getTeamHome().getName() : null;
        String awayTeamName = forecast.getTeamAway() != null ? forecast.getTeamAway().getName() : null;
        String homeForecastId = forecast.getTeamHome() != null ? forecast.getTeamHome().getId() : null;
        String awayForecastId = forecast.getTeamAway() != null ? forecast.getTeamAway().getId() : null;
        LocalDateTime forecastDate = parseStartDate(forecast.getStartAt());

        Event dateMismatchCandidate = null;

        for (Event event : events) {
            if (event.getTeamHome() == null || event.getTeamAway() == null) {
                continue;
            }

            boolean homeMatch = matchAndCacheTeam(event.getTeamHome(), homeForecastId, homeTeamName);
            boolean awayMatch = matchAndCacheTeam(event.getTeamAway(), awayForecastId, awayTeamName);

            if (homeMatch && awayMatch) {
                if (forecastDate != null && event.getGameDatetime() != null) {
                    LocalDate eventDate = event.getGameDatetime().toLocalDate();
                    LocalDate forecastDateOnly = forecastDate.toLocalDate();
                    if (eventDate.equals(forecastDateOnly)) {
                        return event;
                    }
                    dateMismatchCandidate = event; // times bateram, só a data não bateu
                } else {
                    return event;
                }
            }
        }


        if (dateMismatchCandidate != null) {
            log.warn("Team match found for '{} vs {}' but DATE mismatch — event {} date={}, forecast date={}",
                    homeTeamName, awayTeamName, dateMismatchCandidate.getId(),
                    dateMismatchCandidate.getGameDatetime(), forecastDate);
        } else {
            log.warn("No TEAM match found for forecast: {} vs {}", homeTeamName, awayTeamName);
        }

        return null;
    }

    private boolean matchAndCacheTeam(Team team, String forecastTeamId, String forecastTeamName) {
        String cachedId = team.getGameForecastTeamId(); // agora String, não Long

        if (cachedId != null) {
            return cachedId.equals(forecastTeamId);
        }

        boolean nameMatch = forecastTeamName != null && team.getName().equalsIgnoreCase(forecastTeamName);

        if (nameMatch && forecastTeamId != null) {
            team.setGameForecastTeamId(forecastTeamId);
            teamRepository.save(team);
        } else if (!nameMatch) {
            log.warn("No GameForecastAPI match for team '{}' (id={}). "
                            + "Set manually via PATCH /api/v1/teams/{}/game-forecast-id",
                    team.getName(), team.getId(), team.getId());
        }

        return nameMatch;
    }

    private LocalDateTime parseStartDate(String startAt) {
        if (startAt == null) return null;
        try {
            return OffsetDateTime.parse(startAt).atZoneSameInstant(ZoneId.of("America/Fortaleza")).toLocalDateTime();
        } catch (Exception e) {
            log.warn("Could not parse start_at: {}", startAt);
            return null;
        }
    }

    private void importMarketsForEvent(Event event, ForecastEventDto.Prediction prediction, OddsImportResult result) {
        importMatchResultMarket(event, prediction, result);
        importOverUnder25Market(event, prediction, result);
        importOverUnder35Market(event, prediction, result);
        importBttsMarket(event, prediction, result);
        importExactScoreMarket(event, prediction, result);
    }

    private void importMatchResultMarket(Event event, ForecastEventDto.Prediction prediction, OddsImportResult result) {
        if (prediction.getMatchResult() == null) return;

        Market market = findOrCreateMarket(event, MarketType.MATCH_RESULT, "Resultado Final", result);

        createOrUpdateOutcome(market, event.getTeamHome().getName(), prediction.getMatchResult().getHome(), result);
        createOrUpdateOutcome(market, "Empate", prediction.getMatchResult().getDraw(), result);
        createOrUpdateOutcome(market, event.getTeamAway().getName(), prediction.getMatchResult().getAway(), result);
    }

    private void importOverUnder25Market(Event event, ForecastEventDto.Prediction prediction, OddsImportResult result) {
        if (prediction.getTotalGoals() == null) return;

        Market market = findOrCreateMarket(event, MarketType.OVER_UNDER_25, "Over/Under 2.5", result);

        createOrUpdateOutcome(market, "Over 2.5", prediction.getTotalGoals().getOver25(), result);
        createOrUpdateOutcome(market, "Under 2.5", prediction.getTotalGoals().getUnder25(), result);
    }

    private void importOverUnder35Market(Event event, ForecastEventDto.Prediction prediction, OddsImportResult result) {
        if (prediction.getTotalGoals() == null) return;

        Market market = findOrCreateMarket(event, MarketType.OVER_UNDER_35, "Over/Under 3.5", result);

        createOrUpdateOutcome(market, "Over 3.5", prediction.getTotalGoals().getOver35(), result);
        createOrUpdateOutcome(market, "Under 3.5", prediction.getTotalGoals().getUnder35(), result);
    }

    private void importBttsMarket(Event event, ForecastEventDto.Prediction prediction, OddsImportResult result) {
        if (prediction.getBothTeamsScore() == null) return;

        Market market = findOrCreateMarket(event, MarketType.BTTS, "Ambas Marcam", result);

        createOrUpdateOutcome(market, "Sim", prediction.getBothTeamsScore().getYes(), result);
        createOrUpdateOutcome(market, "Não", prediction.getBothTeamsScore().getNo(), result);
    }

    private void importExactScoreMarket(Event event, ForecastEventDto.Prediction prediction, OddsImportResult result) {
        if (prediction.getExactScore() == null) return;

        Market market = findOrCreateMarket(event, MarketType.EXACT_SCORE, "Placar Exato", result);

        int minProbability = gameForecastProperties.getMinExactScoreProbability();

        for (Map.Entry<String, Integer> entry : prediction.getExactScore().entrySet()) {
            String scoreKey = entry.getKey();
            Integer probability = entry.getValue();

            if ("other".equalsIgnoreCase(scoreKey)) {
                continue;
            }

            if (probability != null && probability >= minProbability) {
                String scoreName = scoreKey.replace("_", "-");
                createOrUpdateOutcome(market, scoreName, probability, result);
            }
        }
    }

    private Market findOrCreateMarket(Event event, MarketType marketType, String name, OddsImportResult result) {
        Optional<Market> existing = marketRepository.findByEventIdAndMarketType(event.getId(), marketType);
        if (existing.isPresent()) {
            result.setMarketsUpdated(result.getMarketsUpdated() + 1);
            return existing.get();
        }
        Market market = new Market();
        market.setEvent(event);
        market.setName(name);
        market.setStatus(MarketStatus.OPEN);
        market.setMarketType(marketType);
        result.setMarketsCreated(result.getMarketsCreated() + 1);
        return marketRepository.save(market);
    }

    private void createOrUpdateOutcome(Market market, String name, Integer probability, OddsImportResult result) {
        if (probability == null || probability <= 0) return;

        BigDecimal odd = calculateOdd(probability);

        List<Outcome> existingOutcomes = outcomeRepository.findByMarketId(market.getId());
        Outcome existingOutcome = existingOutcomes.stream()
                .filter(o -> o.getName().equals(name))
                .findFirst()
                .orElse(null);

        if (existingOutcome != null) {
            existingOutcome.setOdd(odd);
            outcomeRepository.save(existingOutcome);
            result.setOddsUpdated(result.getOddsUpdated() + 1);
        } else {
            Outcome outcome = new Outcome();
            outcome.setMarket(market);
            outcome.setName(name);
            outcome.setOdd(odd);
            outcomeRepository.save(outcome);
            result.setOutcomesCreated(result.getOutcomesCreated() + 1);
        }
    }

    private BigDecimal calculateOdd(Integer probability) {
        if (probability == null || probability <= 0) {
            return oddsProperties.getMinOdd();
        }

        BigDecimal prob = BigDecimal.valueOf(probability);
        BigDecimal odd = BigDecimal.valueOf(100).divide(prob, 2, RoundingMode.HALF_UP);

        BigDecimal minOdd = oddsProperties.getMinOdd();
        if (odd.compareTo(minOdd) < 0) {
            return minOdd;
        }

        return odd;
    }
}
