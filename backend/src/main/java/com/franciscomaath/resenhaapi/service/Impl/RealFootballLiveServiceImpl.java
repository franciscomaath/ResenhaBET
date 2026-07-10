package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.SchedulerProperties;
import com.franciscomaath.resenhaapi.domain.entity.Competition;
import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.event.EventChangeEvent;
import com.franciscomaath.resenhaapi.domain.event.EventCompletedEvent;
import com.franciscomaath.resenhaapi.domain.event.EventMarketsCloseRequestedEvent;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.mapper.EventMapper;
import com.franciscomaath.resenhaapi.service.ApiFootballClient;
import com.franciscomaath.resenhaapi.service.EventService;
import com.franciscomaath.resenhaapi.service.RealFootballLiveService;
import com.franciscomaath.resenhaapi.service.dto.MatchDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealFootballLiveServiceImpl implements RealFootballLiveService {

    private final EventRepository eventRepository;
    private final ApiFootballClient apiFootballClient;
    private final EventService eventService;
    private final EventMapper eventMapper;
    private final RealFootballMatchStatusMapper statusMapper;
    private final SchedulerProperties schedulerProperties;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void tick() {
        log.info("Running REAL_FOOTBALL live polling tick at {}", LocalDateTime.now());
        autoStartDueEvents();
        pollLiveScores();
        finalizeStaleInProgressEvents();
    }

    private void autoStartDueEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(schedulerProperties.getAutoCloseGraceMinutes());
        List<Event> dueEvents = eventRepository.findDueRealFootballEvents(cutoff);

        for (Event event : dueEvents) {
            event.setStatus(EventStatus.IN_PROGRESS);
            event = eventRepository.save(event);
            eventPublisher.publishEvent(new EventMarketsCloseRequestedEvent(this, event.getId()));
            publishEventChange(event);
            log.info("Auto-started REAL_FOOTBALL event {} for live polling", event.getId());
        }
    }

    private void pollLiveScores() {
        List<Event> inProgressEvents = eventRepository.findInProgressRealFootballEvents();
        if (inProgressEvents.isEmpty()) {
            return;
        }

        Map<Long, List<Event>> eventsByCompetition = inProgressEvents.stream()
                .collect(Collectors.groupingBy(e -> e.getTournament().getCompetition().getId()));

        for (List<Event> competitionEvents : eventsByCompetition.values()) {
            Competition competition = competitionEvents.get(0).getTournament().getCompetition();
            List<MatchDto> liveMatches = apiFootballClient.fetchLiveEvents(
                    competition.getApiFootballLeagueId(), competition.getApiFootballCountryId());
            Map<String, MatchDto> liveByMatchId = liveMatches.stream()
                    .filter(m -> m.getMatchId() != null)
                    .collect(Collectors.toMap(MatchDto::getMatchId, m -> m, (existing, replacement) -> existing));
            Map<String, List<MatchDto>> missingLiveFallbackCache = new HashMap<>();

            for (Event event : competitionEvents) {
                MatchDto match = liveByMatchId.get(event.getExternalMatchId());
                if (match != null) {
                    applyProviderMatch(event, match);
                } else {
                    finalizeMissingLiveEvent(event, missingLiveFallbackCache);
                }
            }
        }
    }

    private void finalizeMissingLiveEvent(Event event, Map<String, List<MatchDto>> fallbackCache) {
        String externalMatchId = event.getExternalMatchId();
        if (externalMatchId == null || externalMatchId.isBlank()) {
            return;
        }

        List<MatchDto> matches = fallbackCache.computeIfAbsent(externalMatchId, apiFootballClient::fetchEventsByMatchId);
        MatchDto match = matches.stream()
                .filter(m -> externalMatchId.equals(m.getMatchId()))
                .findFirst()
                .orElse(null);
        if (match == null) {
            return;
        }

        RealFootballMatchState state = statusMapper.map(match.getMatchStatus());
        if (state == RealFootballMatchState.FINISHED) {
            completeEvent(event, match);
        } else if (state == RealFootballMatchState.CANCELLED) {
            cancelEvent(event);
        }
    }

    private void finalizeStaleInProgressEvents() {
        List<Event> staleEvents = eventRepository.findInProgressRealFootballEvents().stream()
                .filter(event -> event.getGameDatetime() != null)
                .filter(event -> event.getGameDatetime().plusMinutes(schedulerProperties.getFinishFallbackAfterMinutes())
                        .isBefore(LocalDateTime.now()))
                .toList();

        Map<FixtureLookupKey, List<Event>> eventsByLookup = staleEvents.stream()
                .collect(Collectors.groupingBy(event -> new FixtureLookupKey(
                        event.getTournament().getCompetition().getId(), event.getGameDatetime().toLocalDate())));

        Map<FixtureLookupKey, Map<String, MatchDto>> matchCache = new HashMap<>();
        for (Map.Entry<FixtureLookupKey, List<Event>> entry : eventsByLookup.entrySet()) {
            Event sample = entry.getValue().get(0);
            Competition competition = sample.getTournament().getCompetition();
            LocalDate date = entry.getKey().date();
            List<MatchDto> matches = apiFootballClient.fetchEventsByLeague(
                    competition.getApiFootballLeagueId(), competition.getApiFootballCountryId(), date, date);
            matchCache.put(entry.getKey(), matches.stream()
                    .filter(m -> m.getMatchId() != null)
                    .collect(Collectors.toMap(MatchDto::getMatchId, m -> m, (existing, replacement) -> existing)));
        }

        for (Event event : staleEvents) {
            FixtureLookupKey key = new FixtureLookupKey(
                    event.getTournament().getCompetition().getId(), event.getGameDatetime().toLocalDate());
            MatchDto match = matchCache.getOrDefault(key, Map.of()).get(event.getExternalMatchId());
            if (match != null) {
                applyProviderMatch(event, match);
            }
        }
    }

    private void applyProviderMatch(Event event, MatchDto match) {
        RealFootballMatchState state = statusMapper.map(match.getMatchStatus());

        if (state == RealFootballMatchState.LIVE) {
            updateScore(event, match);
            event = eventRepository.save(event);
            publishEventChange(event);
            return;
        }

        if (state == RealFootballMatchState.FINISHED) {
            completeEvent(event, match);
            return;
        }

        if (state == RealFootballMatchState.CANCELLED) {
            cancelEvent(event);
            return;
        }

        if (isProviderLive(match)) {
            updateScore(event, match);
            event = eventRepository.save(event);
            publishEventChange(event);
        }
    }

    private boolean isProviderLive(MatchDto match) {
        return "1".equals(match.getMatchLive());
    }

    private void completeEvent(Event event, MatchDto match) {
        updateScore(event, match);
        event.setStatus(EventStatus.COMPLETED);
        event = eventRepository.save(event);
        eventPublisher.publishEvent(new EventCompletedEvent(this, event.getId(), EventStatus.COMPLETED));
        publishEventChange(event);
        log.info("Completed REAL_FOOTBALL event {} from provider match {}", event.getId(), match.getMatchId());
    }

    private void cancelEvent(Event event) {
        try {
            eventService.cancelEvent(event.getId());
            log.info("Cancelled REAL_FOOTBALL event {} from provider status", event.getId());
        } catch (RuntimeException ex) {
            log.warn("Could not cancel REAL_FOOTBALL event {}: {}", event.getId(), ex.getMessage());
        }
    }

    private void updateScore(Event event, MatchDto match) {
        event.setHomeScore(parseScore(match.getHomeScore(), event.getHomeScore()));
        event.setAwayScore(parseScore(match.getAwayScore(), event.getAwayScore()));
    }

    private int parseScore(String value, Integer fallback) {
        try {
            return value == null || value.isBlank() ? (fallback != null ? fallback : 0) : Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return fallback != null ? fallback : 0;
        }
    }

    private void publishEventChange(Event event) {
        eventPublisher.publishEvent(new EventChangeEvent(this, event.getId(), eventMapper.toResponse(event)));
    }

    private record FixtureLookupKey(Long competitionId, LocalDate date) {
    }
}
