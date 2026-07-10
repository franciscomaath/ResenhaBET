package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.ApiFootballProperties;
import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.response.OddsImportResult;
import com.franciscomaath.resenhaapi.controller.dto.response.SyncResult;
import com.franciscomaath.resenhaapi.domain.entity.*;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.enums.PhaseType;
import com.franciscomaath.resenhaapi.domain.enums.TournamentStatus;
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
import com.franciscomaath.resenhaapi.service.FixtureSyncService;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.service.OddsImportService;
import com.franciscomaath.resenhaapi.service.dto.MatchDto;
import com.franciscomaath.resenhaapi.service.dto.StandingEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FixtureSyncServiceImpl implements FixtureSyncService {

    private final TournamentRepository tournamentRepository;
    private final EventRepository eventRepository;
    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final ApiFootballClient apiFootballClient;
    private final OddsImportService oddsImportService;
    private final ApiFootballProperties properties;
    private final CurrentUserContext currentUserContext;
    private final ApplicationEventPublisher eventPublisher;
    private final GroupAuthorizationService groupAuthorizationService;

    @Override
    @Transactional
    public SyncResult sync(Long tournamentId) {
        currentUserContext.requireAdmin();
        groupAuthorizationService.requireTournamentAccess(tournamentId);

        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + tournamentId));

        if (tournament.getType() != TournamentType.REAL_FOOTBALL) {
            throw new BusinessException("Only REAL_FOOTBALL tournaments support fixture sync.");
        }

        Competition competition = tournament.getCompetition();

        String leagueId = competition.getApiFootballLeagueId();
        String countryId = competition.getApiFootballCountryId();
        List<MatchDto> matches = apiFootballClient.fetchEventsByLeague(leagueId, countryId, competition.getStartDate().toLocalDate(), competition.getEndDate().toLocalDate());

        Map<String, String> teamGroupMap = buildTeamGroupMap(competition);

        SyncResult result = new SyncResult();
        Map<String, TournamentRound> roundsCache = new HashMap<>();
        final int[] roundCounter = {1};

        boolean needOddsUpdate = false;
        boolean tournamentInProgress = false;

        List<String> externalMatchIds = matches.stream()
                .map(MatchDto::getMatchId)
                .collect(Collectors.toList());

        Map<String, Event> eventsCache = eventRepository.findByTournamentIdAndExternalMatchIdIn(tournamentId, externalMatchIds).stream()
                .collect(Collectors.toMap(Event::getExternalMatchId, e -> e));

        for (MatchDto match : matches) {
            Team homeTeam = findOrCreateTeam(match.getHomeTeamName(), match.getHomeTeamId());
            Team awayTeam = findOrCreateTeam(match.getAwayTeamName(), match.getAwayTeamId());

            if (homeTeam.getApiFootballTeamId() == null) {
                homeTeam.setApiFootballTeamId(match.getHomeTeamId());
                homeTeam.setBadgeUrl(match.getTeamHomeBadge());
                teamRepository.save(homeTeam);
                result.setTeamsLinked(result.getTeamsLinked() + 1);
            }
            if (awayTeam.getApiFootballTeamId() == null) {
                awayTeam.setApiFootballTeamId(match.getAwayTeamId());
                awayTeam.setBadgeUrl(match.getTeamAwayBadge());
                teamRepository.save(awayTeam);
                result.setTeamsLinked(result.getTeamsLinked() + 1);
            }

            String homeGroup = teamGroupMap.get(match.getHomeTeamId());
            String awayGroup = teamGroupMap.get(match.getAwayTeamId());

            boolean isGroupStageMatch = homeGroup != null && homeGroup.equals(awayGroup);

            String roundName;
            PhaseType phaseType;

            if (isGroupStageMatch) {
                roundName = homeGroup;
                phaseType = PhaseType.GROUP_STAGE;
            } else {
                // `match_round` can be present as an empty string on knockout fixtures.
                roundName = firstNonBlank(match.getMatchRound(), match.getStageName());
                if (roundName == null) {
                    roundName = "Default";
                }
                phaseType = PhaseType.KNOCKOUT;
            }

            if (homeGroup != null && awayGroup != null && !homeGroup.equals(awayGroup)) {
                log.debug("Cross-group match detected (knockout): home={} ({}), away={} ({})",
                        match.getHomeTeamId(), homeGroup, match.getAwayTeamId(), awayGroup);
            }

            TournamentRound round = roundsCache.computeIfAbsent(roundName, name -> {
                TournamentRound r = roundRepository.findByTournamentIdAndName(tournamentId, name)
                        .orElse(null);
                if (r == null) {
                    r = TournamentRound.builder()
                            .tournament(tournament)
                            .name(name)
                            .phaseType(phaseType)
                            .roundOrder(roundCounter[0]++)
                            .build();
                    r = roundRepository.save(r);
                    result.setRoundsCreated(result.getRoundsCreated() + 1);
                }
                return r;
            });

            Integer homeScore = 0;
            Integer awayScore = 0;
            EventStatus status = EventStatus.CREATED;

            if ("Finished".equalsIgnoreCase(match.getMatchStatus())) {
                homeScore = parseScore(match.getHomeScore());
                awayScore = parseScore(match.getAwayScore());
                status = EventStatus.COMPLETED;
                tournamentInProgress = true;
            } else if (!"Not Started".equalsIgnoreCase(match.getMatchStatus()) && !"".equalsIgnoreCase(match.getMatchStatus())) {
                homeScore = parseScore(match.getHomeScore());
                awayScore = parseScore(match.getAwayScore());
                status = EventStatus.IN_PROGRESS;
                tournamentInProgress = true;
            }

            Event event = null;
            event = eventsCache.get(match.getMatchId());
            boolean isNewlyCompleted = false;
            boolean shouldCloseMarkets = false;

            if (event != null) {
                EventStatus previousStatus = event.getStatus();
                isNewlyCompleted = (event.getStatus() != EventStatus.COMPLETED) && (status == EventStatus.COMPLETED);
                shouldCloseMarkets = previousStatus != EventStatus.IN_PROGRESS && status == EventStatus.IN_PROGRESS;
                event.setRound(round);
                event.setTeamHome(homeTeam);
                event.setTeamAway(awayTeam);
                event.setGameDatetime(parseMatchDateTime(match.getMatchDate(), match.getMatchTime()));
                event.setStatus(status);
                event.setHomeScore(homeScore);
                event.setAwayScore(awayScore);
                event.setIsKnockout(phaseType == PhaseType.KNOCKOUT);
                result.setEventsUpdated(result.getEventsUpdated() + 1);
            } else {
                event = Event.builder()
                        .tournament(tournament)
                        .round(round)
                        .teamHome(homeTeam)
                        .teamAway(awayTeam)
                        .externalMatchId(match.getMatchId())
                        .gameDatetime(parseMatchDateTime(match.getMatchDate(), match.getMatchTime()))
                        .status(status)
                        .homeScore(homeScore)
                        .awayScore(awayScore)
                        .isKnockout(phaseType == PhaseType.KNOCKOUT)
                        .build();
                needOddsUpdate = true;
                shouldCloseMarkets = status == EventStatus.IN_PROGRESS;
                result.setEventsCreated(result.getEventsCreated() + 1);
            }

            event = eventRepository.save(event);
            if (shouldCloseMarkets) {
                eventPublisher.publishEvent(new EventMarketsCloseRequestedEvent(this, event.getId()));
            }
            if (isNewlyCompleted) {
                eventPublisher.publishEvent(new EventCompletedEvent(this, event.getId(), EventStatus.COMPLETED));
            }
        }

        if(needOddsUpdate) {
            OddsImportResult oddsResult = oddsImportService.importForTournament(tournamentId);
            result.setOddsImported(oddsResult.getMarketsCreated());
            result.setMarketsCreated(oddsResult.getMarketsCreated());
        }

        tournament.setStatus(tournamentInProgress ? TournamentStatus.IN_PROGRESS : TournamentStatus.CREATED);
        tournament.setNumberOfGroups(Math.toIntExact(teamGroupMap.values().stream().distinct().count()));
        tournamentRepository.save(tournament);

        log.info("Fixture sync completed for tournament {}: {} events created, {} events updated, {} teams, {} rounds, {} markets",
                tournamentId, result.getEventsCreated(), result.getEventsUpdated(), result.getTeamsLinked(),
                result.getRoundsCreated(), result.getMarketsCreated());

        return result;
    }

    private Map<String, String> buildTeamGroupMap(Competition competition) {
        List<StandingEntry> standings =
                apiFootballClient.getStandings(competition.getApiFootballLeagueId());

        if (standings.isEmpty()) {
            log.warn("get_standings() returned empty for league {} — all matches will fall back to match_round/stage_name",
                    competition.getApiFootballLeagueId());
        }

        return standings.stream()
                .filter(s -> s.getLeagueRound() != null && !s.getLeagueRound().isBlank())
                .collect(Collectors.toMap(
                        StandingEntry::getTeamId,
                        StandingEntry::getLeagueRound,
                        (existing, replacement) -> existing
                ));
    }

    private Team findOrCreateTeam(String name, String apiFootballId) {
        if (apiFootballId != null) {
            var existing = teamRepository.findByApiFootballTeamId(apiFootballId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        var existingByName = teamRepository.findByName(name);
        if (existingByName.isPresent()) {
            return existingByName.get();
        }

        String abbreviation = name.length() >= 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase();
        Team team = Team.builder()
                .name(name)
                .abbreviation(abbreviation)
                .build();
        return teamRepository.save(team);
    }

    private PhaseType determinePhaseType(String stageName) {
        String lower = stageName.toLowerCase();
        if (lower.contains("final") || lower.contains("semi") || lower.contains("quarter")
                || lower.contains("round of") || lower.contains("16") || lower.contains("8")) {
            return PhaseType.KNOCKOUT;
        }
        return PhaseType.GROUP_STAGE;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private LocalDateTime parseMatchDateTime(String date, String time) {
        try {
            if (date != null && time != null) {
                return LocalDateTime.parse(date + "T" + time, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else if (date != null) {
                return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
            }
        } catch (Exception e) {
            log.warn("Could not parse match datetime: {} {}", date, time);
        }
        return null;
    }

    private Integer parseScore(String score) {
        if (score == null || score.isBlank()) return 0;
        try {
            return Integer.parseInt(score.trim());
        } catch (NumberFormatException e) {
            log.warn("Could not parse score value: {}", score);
            return 0;
        }
    }
}
