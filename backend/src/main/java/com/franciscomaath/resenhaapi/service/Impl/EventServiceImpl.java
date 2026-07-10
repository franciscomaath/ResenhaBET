package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.domain.event.EventChangeEvent;
import com.franciscomaath.resenhaapi.domain.event.EventCompletedEvent;
import com.franciscomaath.resenhaapi.domain.event.OddsRecalculationEvent;
import com.franciscomaath.resenhaapi.config.OddsProperties;
import com.franciscomaath.resenhaapi.controller.dto.request.CompletedEventRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.EventRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.EventUpdateRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.FinishEventRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.PatchEventPlayersRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.EventPatchRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.EventDatetimeRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.EventResponseDTO;
import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.entity.GroupTournament;
import com.franciscomaath.resenhaapi.domain.entity.Market;
import com.franciscomaath.resenhaapi.domain.entity.Outcome;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.entity.Team;
import com.franciscomaath.resenhaapi.domain.entity.Tournament;
import com.franciscomaath.resenhaapi.domain.entity.TournamentRound;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.enums.GenerationMode;
import com.franciscomaath.resenhaapi.domain.enums.MarketStatus;
import com.franciscomaath.resenhaapi.domain.enums.MarketType;
import com.franciscomaath.resenhaapi.domain.enums.PhaseType;
import com.franciscomaath.resenhaapi.domain.enums.TournamentFormat;
import com.franciscomaath.resenhaapi.domain.enums.TournamentStatus;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import com.franciscomaath.resenhaapi.domain.exception.BusinessException;
import com.franciscomaath.resenhaapi.domain.exception.InvalidStateException;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.exception.ValidationException;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupTournamentRepository;
import com.franciscomaath.resenhaapi.domain.repository.MarketRepository;
import com.franciscomaath.resenhaapi.domain.repository.OutcomeRepository;
import com.franciscomaath.resenhaapi.domain.repository.PlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.RoundRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentPlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentRepository;
import com.franciscomaath.resenhaapi.domain.specs.EventSpecs;
import com.franciscomaath.resenhaapi.mapper.EventMapper;
import com.franciscomaath.resenhaapi.service.BetService;
import com.franciscomaath.resenhaapi.service.EventService;
import com.franciscomaath.resenhaapi.service.EloService;
import com.franciscomaath.resenhaapi.service.GoalMarketsOddsCalculator;
import com.franciscomaath.resenhaapi.domain.utils.H2HUtil;
import com.franciscomaath.resenhaapi.service.MarketService;
import com.franciscomaath.resenhaapi.service.OddsCalculatorService;
import com.franciscomaath.resenhaapi.service.TournamentService;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.service.dto.H2HRecord;
import com.franciscomaath.resenhaapi.service.dto.OddsResult;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final PlayerRepository playerRepository;
    private final TournamentRepository tournamentRepository;
    private final EventMapper eventMapper;
    private final RoundRepository roundRepository;
    private final TournamentPlayerRepository tournamentPlayerRepository;
    private final EloService eloService;
    private final OddsCalculatorService oddsCalculatorService;
    private final GoalMarketsOddsCalculator goalMarketsOddsCalculator;
    private final MarketRepository marketRepository;
    private final OutcomeRepository outcomeRepository;
    private final BetService betService;
    private final MarketService marketService;
    private final OddsProperties oddsProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final CurrentUserContext currentUserContext;
    private final TournamentServiceImpl tournamentService;
    private final GroupAuthorizationService groupAuthorizationService;
    private final GroupTournamentRepository groupTournamentRepository;
    private final com.franciscomaath.resenhaapi.domain.repository.BetSlipRepository betSlipRepository;
    private final com.franciscomaath.resenhaapi.domain.repository.TeamRepository teamRepository;

    @Override
    @Transactional
    public EventResponseDTO create(EventRequestDTO dto) {
        Tournament tournament = tournamentRepository.findByIdAndDeletedAtIsNull(dto.getTournamentId())
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + dto.getTournamentId()));
        requireTournamentMutation(tournament);
        validateEventCreation(tournament, dto);

        Player playerHome = playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(dto.getPlayerHomeId(), currentUserContext.getRequiredGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + dto.getPlayerHomeId()));

        if (!tournamentPlayerRepository.existsByTournamentIdAndPlayerId(
                dto.getTournamentId(), dto.getPlayerHomeId())) {
            throw new BusinessException("Player não está inscrito neste torneio.");
        }

        Player playerAway = playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(dto.getPlayerAwayId(), currentUserContext.getRequiredGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + dto.getPlayerAwayId()));

        if (!tournamentPlayerRepository.existsByTournamentIdAndPlayerId(
                dto.getTournamentId(), dto.getPlayerAwayId())) {
            throw new BusinessException("Player não está inscrito neste torneio.");
        }

        if(eventRepository.existsByPlayerHomeIdAndPlayerAwayIdAndRoundId(dto.getPlayerHomeId(), dto.getPlayerAwayId(), dto.getRoundId())) {
            throw new BusinessException("Essa partida já existe nessa rodada.");
        }

        TournamentRound round = roundRepository.findById(dto.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Round not found with id: " + dto.getRoundId()));

        LocalDateTime gameDatetime = dto.getGameDatetime() != null ? dto.getGameDatetime() : LocalDateTime.now();

        boolean isKnockout = round.getPhaseType() == PhaseType.KNOCKOUT;

        Event event = Event.builder()
                        .tournament(tournament)
                        .playerHome(playerHome)
                        .homeEloBefore(playerHome.getCurrentElo())
                        .playerAway(playerAway)
                        .awayEloBefore(playerAway.getCurrentElo())
                        .gameDatetime(gameDatetime)
                        .round(round)
                        .status(EventStatus.CREATED)
                        .homeScore(0)
                        .awayScore(0)
                        .isKnockout(isKnockout)
                        .build();

        event = eventRepository.save(event);

        createMarketAndOutcomesForEvent(event, playerHome, playerAway);

        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional
    public EventResponseDTO createCompleted(CompletedEventRequestDTO dto) {
        Tournament tournament = tournamentRepository.findByIdAndDeletedAtIsNull(dto.getTournamentId())
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found with id: " + dto.getTournamentId()));
        requireTournamentMutation(tournament);
        validateCompletedEventCreation(tournament, dto);

        TournamentRound round = roundRepository.findById(dto.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Round not found with id: " + dto.getRoundId()));

        LocalDateTime gameDatetime = dto.getGameDatetime() != null ? dto.getGameDatetime() : LocalDateTime.now();
        boolean isKnockout = round.getPhaseType() == PhaseType.KNOCKOUT;
        boolean isBye = Boolean.TRUE.equals(dto.getIsBye());

        Player playerHome = null;
        Player playerAway = null;

        if (isBye) {
            if (dto.getPlayerHomeId() != null) {
                playerHome = loadTournamentPlayer(dto.getPlayerHomeId(), tournament.getId());
            }
            if (dto.getPlayerAwayId() != null) {
                playerAway = loadTournamentPlayer(dto.getPlayerAwayId(), tournament.getId());
            }
        } else {
            if (dto.getPlayerHomeId() == null || dto.getPlayerAwayId() == null) {
                throw new ValidationException("Player home and player away are required for completed matches.");
            }

            playerHome = loadTournamentPlayer(dto.getPlayerHomeId(), tournament.getId());
            playerAway = loadTournamentPlayer(dto.getPlayerAwayId(), tournament.getId());

            if (isKnockout && dto.getHomeScore().equals(dto.getAwayScore())) {
                throw new BusinessException("Knockout completed matches must have different scores.");
            }

            if (eventRepository.existsByPlayerHomeIdAndPlayerAwayIdAndRoundId(dto.getPlayerHomeId(), dto.getPlayerAwayId(), dto.getRoundId())) {
                throw new BusinessException("Essa partida já existe nessa rodada.");
            }
        }

        Event event = Event.builder()
                .tournament(tournament)
                .round(round)
                .playerHome(playerHome)
                .playerAway(playerAway)
                .gameDatetime(gameDatetime)
                .status(EventStatus.COMPLETED)
                .homeScore(dto.getHomeScore())
                .awayScore(dto.getAwayScore())
                .isKnockout(isKnockout)
                .isBye(isBye)
                .build();

        return completeEvent(event, false);
    }

    @Override
    public EventResponseDTO findEvent(Long eventId){
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        groupAuthorizationService.requireTournamentAccess(event.getTournament().getId());
        return eventMapper.toResponse(event);
    }

    @Override
    public List<EventResponseDTO> findAll(Long tournamentId, EventStatus status) {
        if (tournamentId != null) {
            groupAuthorizationService.requireTournamentAccess(tournamentId);
        }
        Specification<Event> spec = EventSpecs.withFilters(tournamentId, status);
        List<Event> events = eventRepository.findAll(spec);
        return events.stream()
                .filter(event -> tournamentId != null || event.getTournament() == null
                        || isTournamentVisible(event.getTournament().getId()))
                .sorted(Comparator.comparing(Event::getId, Comparator.nullsLast(Long::compareTo)))
                .map(eventMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public EventResponseDTO startEvent(Long eventId){
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        requireTournamentMutation(event.getTournament());

        if(event.getRound() == null){
            throw new ResourceNotFoundException("Round not found");
        }

        if(event.getStatus() !=  EventStatus.CREATED){
            throw new BusinessException("Event already started or completed");
        }

        event.setStatus(EventStatus.IN_PROGRESS);

        marketService.closeMarket(eventId);

        event = eventRepository.save(event);
        EventResponseDTO response = eventMapper.toResponse(event);
        eventPublisher.publishEvent(new EventChangeEvent(this, eventId, response));
        return response;
    }

    @Override
    @Transactional
    public EventResponseDTO resetEvent(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        requireTournamentMutation(event.getTournament());

        if (event.getStatus() != EventStatus.IN_PROGRESS) {
            throw new BusinessException("Only IN_PROGRESS events can be reset.");
        }

        event.setStatus(EventStatus.CREATED);
        event.setHomeScore(0);
        event.setAwayScore(0);
        event.setPenaltiesHome(null);
        event.setPenaltiesAway(null);
        event.setHomeEloBefore(null);
        event.setAwayEloBefore(null);

        event = eventRepository.save(event);
        if(marketRepository.existsByEventId(eventId)) marketService.openMarket(eventId);

        EventResponseDTO response = eventMapper.toResponse(event);
        eventPublisher.publishEvent(new EventChangeEvent(this, eventId, response));
        return response;
    }

    @Override
    @Transactional
    public EventResponseDTO finishEvent(Long eventId){
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        requireTournamentMutation(event.getTournament());

        if(event.getHomeScore() == null || event.getAwayScore() == null){
            throw new BusinessException("Score not found");
        }

        if(event.getStatus() != EventStatus.IN_PROGRESS){
            throw new  BusinessException("Event already finished or not started.");
        }

        boolean isKnockout = event.getIsKnockout() != null && event.getIsKnockout();

        if (isKnockout && event.getHomeScore().equals(event.getAwayScore())) {
            event.setStatus(EventStatus.PENALTIES);
            event = eventRepository.save(event);
            EventResponseDTO response = eventMapper.toResponse(event);
            eventPublisher.publishEvent(new EventChangeEvent(this, eventId, response));
            return response;
        }

        event.setStatus(EventStatus.COMPLETED);
        return completeEvent(event, false);
    }

    @Override
    @Transactional
    public EventResponseDTO reopenEvent(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        requireTournamentMutation(event.getTournament());

        if (event.getStatus() != EventStatus.COMPLETED) {
            throw new BusinessException("Only COMPLETED events can be reopened.");
        }

        rollbackKnockoutProgression(event);
        betService.reopenBetsForEvent(eventId);

        event.setStatus(EventStatus.IN_PROGRESS);
        event.setHomeScore(null);
        event.setAwayScore(null);
        event.setPenaltiesHome(null);
        event.setPenaltiesAway(null);
        event.setHomeEloBefore(null);
        event.setAwayEloBefore(null);

        Tournament tournament = event.getTournament();
        if (tournament != null && tournament.getStatus() == TournamentStatus.COMPLETED) {
            tournament.setStatus(TournamentStatus.IN_PROGRESS);
            tournament.setEndDate(null);
            tournamentRepository.save(tournament);
        }

        event = eventRepository.save(event);

        if (event.getTournament() != null && groupTournamentRepository.findByTournamentIdAndGroupId(
                event.getTournament().getId(), currentUserContext.getRequiredGroupId()).isPresent()) {
            eloService.recalculateGroupElos(currentUserContext.getRequiredGroupId());
        }
        EventResponseDTO response = eventMapper.toResponse(event);
        eventPublisher.publishEvent(new EventChangeEvent(this, eventId, response));
        return response;
    }

    @Override
    @Transactional
    public EventResponseDTO recordPenalties(Long eventId, FinishEventRequestDTO dto) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        requireTournamentMutation(event.getTournament());

        if (event.getStatus() != EventStatus.PENALTIES) {
            throw new BusinessException("Event is not in PENALTIES status.");
        }

        if (Boolean.FALSE.equals(event.getIsKnockout())) {
            throw new BusinessException("Event is not a knockout match.");
        }

        if (dto.getPenaltiesHome() != null) {
            event.setPenaltiesHome(dto.getPenaltiesHome());
        }
        if (dto.getPenaltiesAway() != null) {
            event.setPenaltiesAway(dto.getPenaltiesAway());
        }

        // If DTO status is COMPLETED, finalize the event
        if (dto.getStatus() == EventStatus.COMPLETED) {
            if (event.getPenaltiesHome() == null || event.getPenaltiesAway() == null) {
                throw new BusinessException("Penalty scores not set");
            }
            if (event.getPenaltiesHome().equals(event.getPenaltiesAway())) {
                throw new BusinessException("Penalty scores must be different to determine a winner.");
            }

            event.setStatus(EventStatus.COMPLETED);

            Player homePlayer = event.getPlayerHome();
            Player awayPlayer = event.getPlayerAway();
            if (homePlayer != null) {
                event.setHomeEloBefore(homePlayer.getCurrentElo());
            }
            if (awayPlayer != null) {
                event.setAwayEloBefore(awayPlayer.getCurrentElo());
            }

            return completeEvent(event, true);
        } else {
            // Just update penalties scores
            event = eventRepository.save(event);
        }

        EventResponseDTO response = eventMapper.toResponse(event);
        eventPublisher.publishEvent(new EventChangeEvent(this, eventId, response));
        return response;
    }

    @Override
    @Transactional
    public EventResponseDTO cancelEvent(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        requireTournamentMutation(event.getTournament());

        if (event.getStatus() != EventStatus.CREATED && event.getStatus() != EventStatus.IN_PROGRESS) {
            throw new BusinessException("Event must be CREATED or IN_PROGRESS to be cancelled.");
        }

        event.setStatus(EventStatus.CANCELLED);
        event = eventRepository.save(event);

        marketService.cancelMarket(eventId);
        betService.cancelBetsForEvent(eventId);

        EventResponseDTO response = eventMapper.toResponse(event);
        eventPublisher.publishEvent(new EventChangeEvent(this, eventId, response));
        eventPublisher.publishEvent(new EventCompletedEvent(this, eventId, EventStatus.CANCELLED));
        return response;
    }

    @Override
    @Transactional
    public EventResponseDTO editEvent(Long eventId, EventPatchRequestDTO dto) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        
        if (event.getTournament() != null && event.getTournament().getType() == TournamentType.REAL_FOOTBALL) {
            currentUserContext.requireAdmin();
        } else {
            requireTournamentMutation(event.getTournament());
        }

        if (event.getStatus() != EventStatus.CREATED) {
            throw new BusinessException("Only CREATED events can be edited.");
        }

        if (dto.getRoundId() != null) {
            TournamentRound round = roundRepository.findById(dto.getRoundId())
                    .orElseThrow(() -> new ResourceNotFoundException("Round not found with id: " + dto.getRoundId()));
            event.setRound(round);
        }

        if (dto.getPlayerHomeId() != null) {
            Player player = playerRepository.findById(dto.getPlayerHomeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + dto.getPlayerHomeId()));
            event.setPlayerHome(player);
        }

        if (dto.getPlayerAwayId() != null) {
            Player player = playerRepository.findById(dto.getPlayerAwayId())
                    .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + dto.getPlayerAwayId()));
            event.setPlayerAway(player);
        }

        if (dto.getTeamHomeId() != null) {
            Team team = teamRepository.findById(dto.getTeamHomeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + dto.getTeamHomeId()));
            event.setTeamHome(team);
        }

        if (dto.getTeamAwayId() != null) {
            Team team = teamRepository.findById(dto.getTeamAwayId())
                    .orElseThrow(() -> new ResourceNotFoundException("Team not found with id: " + dto.getTeamAwayId()));
            event.setTeamAway(team);
        }

        if (dto.getGameDatetime() != null) {
            event.setGameDatetime(dto.getGameDatetime());
        }

        if (dto.getIsKnockout() != null) {
            event.setIsKnockout(dto.getIsKnockout());
        }

        if (dto.getIsThirdPlaceMatch() != null) {
            event.setThirdPlaceMatch(dto.getIsThirdPlaceMatch());
        }

        event = eventRepository.save(event);
        EventResponseDTO response = eventMapper.toResponse(event);
        eventPublisher.publishEvent(new EventChangeEvent(this, eventId, response));
        return response;
    }

    @Override
    @Transactional
    public EventResponseDTO rescheduleEvent(Long eventId, EventDatetimeRequestDTO dto) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        
        if (event.getTournament() != null && event.getTournament().getType() == TournamentType.REAL_FOOTBALL) {
            currentUserContext.requireAdmin();
        } else {
            requireTournamentMutation(event.getTournament());
        }

        if (event.getStatus() != EventStatus.CREATED) {
            throw new BusinessException("Only CREATED events can be rescheduled.");
        }

        event.setGameDatetime(dto.getGameDatetime());
        event = eventRepository.save(event);
        
        EventResponseDTO response = eventMapper.toResponse(event);
        eventPublisher.publishEvent(new EventChangeEvent(this, eventId, response));
        return response;
    }

    @Override
    @Transactional
    public void softDeleteEvent(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        
        if (event.getTournament() != null && event.getTournament().getType() == TournamentType.REAL_FOOTBALL) {
            currentUserContext.requireAdmin();
        } else {
            requireTournamentMutation(event.getTournament());
        }

        if (event.getStatus() != EventStatus.CREATED) {
            throw new BusinessException("Cannot delete event: it is not in CREATED status.");
        }

        if (betSlipRepository.existsByEventId(eventId)) {
            throw new BusinessException("Cannot delete event: it already has bets.");
        }

        if (event.getTournament() != null && 
            (event.getTournament().getFormat() == TournamentFormat.BRACKET || 
             event.getTournament().getFormat() == TournamentFormat.LEAGUE_BRACKET)) {
            if (eventRepository.existsByHomeSourceEventIdOrAwaySourceEventId(eventId, eventId)) {
                throw new BusinessException("Cannot delete event: it is a source for another bracket event.");
            }
        }

        event.setDeletedAt(LocalDateTime.now());
        eventRepository.save(event);
    }

    private void advanceKnockoutWinner(Event event) {
        Player winner = determineWinner(event);
        Player loser = determineLoser(event);

        // Advance winner to next round
        Event nextEvent = event.getNextRoundEvent();
        if (nextEvent != null) {
            assignPlayerToNextEvent(winner, event, nextEvent);
        }

        // If semi-final, advance loser to 3rd place match
        if (nextEvent != null && nextEvent.getRound() != null && "Final".equals(nextEvent.getRound().getName())
                && Boolean.TRUE.equals(event.getTournament().getHasThirdPlaceMatch())) {
            Event thirdPlaceEvent = eventRepository.findByTournamentIdAndRoundName(
                    event.getTournament().getId(), "3rd Place").orElse(null);
            if (thirdPlaceEvent != null && loser != null) {
                assignPlayerToNextEvent(loser, event, thirdPlaceEvent);
            }
        }

        // Check if tournament is complete
        checkTournamentCompletion(event.getTournament());
    }

    private void rollbackKnockoutProgression(Event event) {
        if (!Boolean.TRUE.equals(event.getIsKnockout())) {
            return;
        }

        Player winner = determineWinner(event);
        Player loser = Boolean.TRUE.equals(event.getIsBye()) ? null : determineLoser(event);

        Event nextEvent = event.getNextRoundEvent();
        if (nextEvent != null) {
            if (nextEvent.getStatus() != EventStatus.CREATED) {
                throw new BusinessException("Cannot reopen event because the next round event has already started.");
            }

            clearAdvancedPlayer(nextEvent, event, winner);
            closeMarketsIfPresent(nextEvent.getId());
            eventRepository.save(nextEvent);
        }

        if (nextEvent != null && "Final".equals(nextEvent.getRound().getName())
                && Boolean.TRUE.equals(event.getTournament().getHasThirdPlaceMatch())) {
            Event thirdPlaceEvent = eventRepository.findByTournamentIdAndRoundName(
                    event.getTournament().getId(), "3rd Place").orElse(null);
            if (thirdPlaceEvent != null) {
                if (thirdPlaceEvent.getStatus() != EventStatus.CREATED) {
                    throw new BusinessException("Cannot reopen event because the third place match has already started.");
                }
                clearAdvancedPlayer(thirdPlaceEvent, event, loser);
                closeMarketsIfPresent(thirdPlaceEvent.getId());
                eventRepository.save(thirdPlaceEvent);
            }
        }
    }

    private void clearAdvancedPlayer(Event targetEvent, Event sourceEvent, Player player) {
        if (targetEvent == null || player == null) {
            return;
        }

        if (isSameEvent(targetEvent.getHomeSourceEvent(), sourceEvent)
                || (targetEvent.getPlayerHome() != null && targetEvent.getPlayerHome().getId().equals(player.getId()))) {
            targetEvent.setPlayerHome(null);
        }
        if (isSameEvent(targetEvent.getAwaySourceEvent(), sourceEvent)
                || (targetEvent.getPlayerAway() != null && targetEvent.getPlayerAway().getId().equals(player.getId()))) {
            targetEvent.setPlayerAway(null);
        }
    }

    private void closeMarketsIfPresent(Long eventId) {
        if (marketRepository.findAllByEventId(eventId).isEmpty()) {
            return;
        }
        marketService.closeMarket(eventId);
    }

    private Player determineWinner(Event event) {
        boolean usePenalties = event.getPenaltiesHome() != null && event.getPenaltiesAway() != null;
        if (usePenalties) {
            if (event.getPenaltiesHome() > event.getPenaltiesAway()) {
                return event.getPlayerHome();
            } else {
                return event.getPlayerAway();
            }
        }
        if (event.getHomeScore() > event.getAwayScore()) {
            return event.getPlayerHome();
        }
        return event.getPlayerAway();
    }

    private Player determineLoser(Event event) {
        Player winner = determineWinner(event);
        if (winner.getId().equals(event.getPlayerHome().getId())) {
            return event.getPlayerAway();
        }
        return event.getPlayerHome();
    }

    private void assignPlayerToNextEvent(Player player, Event sourceEvent, Event nextEvent) {
        if (isSameEvent(nextEvent.getHomeSourceEvent(), sourceEvent)) {
            nextEvent.setPlayerHome(player);
        } else if (isSameEvent(nextEvent.getAwaySourceEvent(), sourceEvent)) {
            nextEvent.setPlayerAway(player);
        } else if (nextEvent.getHomeSourceEvent() == null && nextEvent.getAwaySourceEvent() == null) {
            assignPlayerToFirstAvailableSlot(player, nextEvent);
        } else {
            throw new BusinessException("Completed event is not a source for the target knockout event.");
        }
        nextEvent = eventRepository.save(nextEvent);

        // Create market if both players are now assigned
        if (nextEvent.getPlayerHome() != null && nextEvent.getPlayerAway() != null) {
            createMarketAndOutcomesForEvent(nextEvent, nextEvent.getPlayerHome(), nextEvent.getPlayerAway());
        }
    }

    private boolean isSameEvent(Event expectedSource, Event sourceEvent) {
        return expectedSource != null
                && sourceEvent != null
                && expectedSource.getId() != null
                && expectedSource.getId().equals(sourceEvent.getId());
    }

    private void assignPlayerToFirstAvailableSlot(Player player, Event nextEvent) {
        if (nextEvent.getPlayerHome() == null) {
            nextEvent.setPlayerHome(player);
        } else if (nextEvent.getPlayerAway() == null) {
            nextEvent.setPlayerAway(player);
        } else {
            throw new BusinessException("Next round event already has both players assigned.");
        }
    }

    private void checkTournamentCompletion(Tournament tournament) {
        if (tournament == null) {
            return;
        }
        // For LEAGUE_BRACKET, only check completion if knockout phase has been created
        if (tournament.getFormat() == TournamentFormat.LEAGUE_BRACKET) {
            boolean hasKnockoutRounds = tournament.getRounds() != null &&
                    tournament.getRounds().stream()
                            .anyMatch(r -> r.getPhaseType() == PhaseType.KNOCKOUT);
            if (!hasKnockoutRounds) {
                // Still in group stage - check if all group events are completed
                List<Event> allEvents = eventRepository.findAllByTournamentId(tournament.getId());
                if (allEvents.isEmpty()) {
                    return;
                }
                boolean allCompleted = allEvents.stream().allMatch(e -> e.getStatus() == EventStatus.COMPLETED || e.getStatus() == EventStatus.CANCELLED);
                if (allCompleted && tournament.getGenerationMode() == GenerationMode.AUTO) {
                    // Auto-trigger advance to bracket
                    tournamentService.advanceToBracketInternal(tournament.getId());
                }
                return;
            }
        }
        List<Event> allEvents = eventRepository.findAllByTournamentId(tournament.getId());
        if (allEvents.isEmpty()) {
            return;
        }
        boolean allCompleted = allEvents.stream().allMatch(e -> e.getStatus() == EventStatus.COMPLETED || e.getStatus() == EventStatus.CANCELLED);
        if (allCompleted && tournament.getStatus() != TournamentStatus.COMPLETED) {
            tournament.setStatus(TournamentStatus.COMPLETED);
            tournament.setEndDate(LocalDateTime.now());
            tournamentRepository.save(tournament);
        }
    }

    @Override
    @Transactional
    public EventResponseDTO updateScore(Long eventId, EventUpdateRequestDTO dto){
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        requireTournamentMutation(event.getTournament());

        if(event.getStatus() != EventStatus.IN_PROGRESS){
            throw new InvalidStateException("Event is not in progress");
        }

        event.setHomeScore(dto.getHomeScore());
        event.setAwayScore(dto.getAwayScore());

        event = eventRepository.save(event);
        EventResponseDTO response = eventMapper.toResponse(event);
        eventPublisher.publishEvent(new EventChangeEvent(this, eventId, response));
        return response;
    }

    @Override
    @Transactional
    public EventResponseDTO patchEventPlayers(Long eventId, PatchEventPlayersRequestDTO dto) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
        requireTournamentMutation(event.getTournament());

        if (event.getStatus() != EventStatus.CREATED) {
            throw new BusinessException("Event is not in CREATED status.");
        }

        Tournament tournament = event.getTournament();
        Player playerHome = null;
        Player playerAway = null;

        if (dto.getPlayerHomeId() != null) {
            playerHome = playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(dto.getPlayerHomeId(), currentUserContext.getRequiredGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + dto.getPlayerHomeId()));

            if (!tournamentPlayerRepository.existsByTournamentIdAndPlayerId(tournament.getId(), playerHome.getId())) {
                throw new BusinessException("Player não está inscrito neste torneio.");
            }

            event.setPlayerHome(playerHome);
        }

        if (dto.getPlayerAwayId() != null) {
            playerAway = playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(dto.getPlayerAwayId(), currentUserContext.getRequiredGroupId())
                    .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + dto.getPlayerAwayId()));

            if (!tournamentPlayerRepository.existsByTournamentIdAndPlayerId(tournament.getId(), playerAway.getId())) {
                throw new BusinessException("Player não está inscrito neste torneio.");
            }

            event.setPlayerAway(playerAway);
        }

        event = eventRepository.save(event);

        // Create market if both players are now assigned and no market exists
        Player homePlayer = event.getPlayerHome();
        Player awayPlayer = event.getPlayerAway();
        if (homePlayer != null && awayPlayer != null) {
            boolean hasMarket = marketRepository.findByEventIdAndMarketType(eventId, MarketType.MATCH_RESULT).isPresent();
            if (!hasMarket) {
                createMarketAndOutcomesForEvent(event, homePlayer, awayPlayer);
            }
        }

        EventResponseDTO response = eventMapper.toResponse(event);
        eventPublisher.publishEvent(new EventChangeEvent(this, eventId, response));
        return response;
    }

    private void createMarketAndOutcomesForEvent(Event event, Player playerHome, Player playerAway) {
        if (Boolean.TRUE.equals(event.getIsBye())) {
            return;
        }

        if (marketRepository.findByEventIdAndMarketType(event.getId(), MarketType.MATCH_RESULT).isPresent()) {
            return;
        }

        List<Event> h2hEvents = eventRepository.findDirectConfrontations(
                playerHome.getId(), playerAway.getId(), EventStatus.COMPLETED,
                PageRequest.of(0, oddsProperties.getH2hMatchLimit()));

        H2HRecord h2hRecord = H2HUtil.buildH2HRecord(h2hEvents, playerHome.getId(), playerAway.getId());
        OddsResult odds = oddsCalculatorService.calculate(
                playerHome.getCurrentElo(), playerAway.getCurrentElo(), h2hRecord);
        GroupTournament groupTournament = null;
        if (groupTournamentRepository != null) {
            groupTournament = groupTournamentRepository.findByTournamentIdAndGroupId(
                            event.getTournament().getId(), currentUserContext.getRequiredGroupId())
                    .orElse(null);
        }

        Market market = new Market();
        market.setEvent(event);
        market.setName("Resultado Final");
        market.setStatus(MarketStatus.OPEN);
        market.setMarketType(MarketType.MATCH_RESULT);
        market = marketRepository.save(market);

        createOutcome(market, "Vitória Casa", odds.getHomeOdd());
        createOutcome(market, "Empate", odds.getDrawOdd());
        createOutcome(market, "Vitória Fora", odds.getAwayOdd());

        if (goalMarketsOddsCalculator == null || groupTournament == null) {
            return;
        }

        GoalMarketsOddsCalculator.GoalMarketsOdds goalOdds = goalMarketsOddsCalculator.calculate(playerHome, playerAway);

        Set<MarketType> marketTypes = groupTournament.getMarketTypes();
        if (marketTypes == null || marketTypes.isEmpty()) {
            return;
        }

        boolean isKnockout = event.getIsKnockout() != null && event.getIsKnockout();

        if (marketTypes.contains(MarketType.OVER_UNDER_25)) {
            Market goalMarket = createMarket(event, MarketType.OVER_UNDER_25, "Over/Under 2.5");
            createOutcome(goalMarket, "Acima de 2.5", goalOdds.over25());
            createOutcome(goalMarket, "Abaixo de 2.5", goalOdds.under25());
        }

        if (marketTypes.contains(MarketType.OVER_UNDER_35)) {
            Market goalMarket = createMarket(event, MarketType.OVER_UNDER_35, "Over/Under 3.5");
            createOutcome(goalMarket, "Acima de 3.5", goalOdds.over35());
            createOutcome(goalMarket, "Abaixo de 3.5", goalOdds.under35());
        }

        if (marketTypes.contains(MarketType.BTTS)) {
            Market goalMarket = createMarket(event, MarketType.BTTS, "Ambas Marcam");
            createOutcome(goalMarket, "Ambas Marcam - Sim", goalOdds.bttsYes());
            createOutcome(goalMarket, "Ambas Marcam - Não", goalOdds.bttsNo());
        }

        if (marketTypes.contains(MarketType.EXACT_SCORE)) {
            Market goalMarket = createMarket(event, MarketType.EXACT_SCORE, "Placar Exato");
            goalOdds.exactScoreOdds().forEach((label, odd) -> createOutcome(goalMarket, label, odd));
        }

        if (isKnockout && marketTypes.contains(MarketType.QUALIFY)) {
            double pHomeAdv = odds.getPHome() / (odds.getPHome() + odds.getPAway());
            double pAwayAdv = odds.getPAway() / (odds.getPHome() + odds.getPAway());
            Market goalMarket = createMarket(event, MarketType.QUALIFY, "Para se Classificar");
            createOutcome(goalMarket, playerHome.getName() + " avança", toOdd(pHomeAdv));
            createOutcome(goalMarket, playerAway.getName() + " avança", toOdd(pAwayAdv));
        }
    }

    private Market createMarket(Event event, MarketType marketType, String name) {
        Market market = new Market();
        market.setEvent(event);
        market.setName(name);
        market.setStatus(MarketStatus.OPEN);
        market.setMarketType(marketType);
        return marketRepository.save(market);
    }

    private BigDecimal toOdd(double probability) {
        BigDecimal minOdd = oddsProperties.getMinOdd();
        BigDecimal odd = BigDecimal.valueOf(probability <= 0.0d ? 0.0d : 1.0d / probability);
        return odd.compareTo(minOdd) < 0 ? minOdd : odd;
    }

    private void createOutcome(Market market, String name, BigDecimal odd) {
        Outcome outcome = new Outcome();
        outcome.setMarket(market);
        outcome.setName(name);
        outcome.setOdd(odd);
        outcomeRepository.save(outcome);
    }

    private void validateEventCreation(Tournament tournament, EventRequestDTO dto) {
        TournamentType type = tournament.getType();

        if (type == TournamentType.REAL_FOOTBALL) {
            throw new BusinessException("Events for REAL_FOOTBALL tournaments must be created via fixture sync.");
        }

        if (type == TournamentType.FIFA_MATCH) {
            if (dto.getPlayerHomeId() == null || dto.getPlayerAwayId() == null) {
                throw new ValidationException("Player home and player away are required for FIFA_MATCH tournaments.");
            }
        }
    }

    private void validateCompletedEventCreation(Tournament tournament, CompletedEventRequestDTO dto) {
        if (tournament.getType() == TournamentType.REAL_FOOTBALL) {
            throw new BusinessException("Events for REAL_FOOTBALL tournaments must be created via fixture sync.");
        }
    }

    private EventResponseDTO completeEvent(Event event, boolean publishOddsRecalculation) {
        Player homePlayer = event.getPlayerHome();
        Player awayPlayer = event.getPlayerAway();

        if (homePlayer != null) {
            event.setHomeEloBefore(homePlayer.getCurrentElo());
        }
        if (awayPlayer != null) {
            event.setAwayEloBefore(awayPlayer.getCurrentElo());
        }

        event = eventRepository.save(event);

        eventPublisher.publishEvent(new EventCompletedEvent(this, event.getId(), EventStatus.COMPLETED));

        if (!Boolean.TRUE.equals(event.getIsBye()) && homePlayer != null && awayPlayer != null) {
            eloService.applyEloForEvent(event);
        }

        if (Boolean.TRUE.equals(event.getIsKnockout())) {
            if (Boolean.TRUE.equals(event.getIsBye())) {
                advanceByeWinner(event, homePlayer != null ? homePlayer : awayPlayer);
            } else {
                advanceKnockoutWinner(event);
            }
        } else {
            checkTournamentCompletion(event.getTournament());
        }

        if (publishOddsRecalculation && event.getTournament() != null) {
            eventPublisher.publishEvent(new OddsRecalculationEvent(this, event.getTournament().getId()));
        }

        EventResponseDTO response = eventMapper.toResponse(event);
        eventPublisher.publishEvent(new EventChangeEvent(this, event.getId(), response));
        return response;
    }

    private Player loadTournamentPlayer(Long playerId, Long tournamentId) {
        Player player = playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(playerId, currentUserContext.getRequiredGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Player not found with id: " + playerId));

        if (!tournamentPlayerRepository.existsByTournamentIdAndPlayerId(tournamentId, playerId)) {
            throw new BusinessException("Player não está inscrito neste torneio.");
        }

        return player;
    }

    private void advanceByeWinner(Event event, Player winner) {
        Event nextEvent = event.getNextRoundEvent();
        if (nextEvent != null) {
            if (isSameEvent(nextEvent.getHomeSourceEvent(), event)) {
                nextEvent.setPlayerHome(winner);
            } else if (isSameEvent(nextEvent.getAwaySourceEvent(), event)) {
                nextEvent.setPlayerAway(winner);
            } else if (nextEvent.getHomeSourceEvent() == null && nextEvent.getAwaySourceEvent() == null) {
                assignPlayerToFirstAvailableSlot(winner, nextEvent);
            }
            nextEvent = eventRepository.save(nextEvent);

            if (nextEvent.getPlayerHome() != null && nextEvent.getPlayerAway() != null) {
                createMarketAndOutcomesForEvent(nextEvent, nextEvent.getPlayerHome(), nextEvent.getPlayerAway());
            }
        }

        checkTournamentCompletion(event.getTournament());
    }

    private void requireTournamentMutation(Tournament tournament) {
        if (tournament == null) {
            return;
        }
        if (tournament.getType() == TournamentType.REAL_FOOTBALL) {
            currentUserContext.requireAdmin();
            groupAuthorizationService.requireTournamentAccess(tournament.getId());
            return;
        }
        groupAuthorizationService.requireTournamentAdmin(tournament.getId());
    }

    private boolean isTournamentVisible(Long tournamentId) {
        try {
            groupAuthorizationService.requireTournamentAccess(tournamentId);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }
}
