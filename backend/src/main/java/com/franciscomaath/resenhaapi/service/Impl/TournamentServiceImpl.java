package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.PatchTournamentPlayerTeamRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.StartTournamentRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.TournamentPatchRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.TournamentPlayerRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.TournamentRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.BracketPlacementDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.GroupStandingsDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerStatsResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentPlayerResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentPlayersResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentRoundResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentScoreboardResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TeamStatsResponseDTO;
import com.franciscomaath.resenhaapi.config.OddsProperties;
import com.franciscomaath.resenhaapi.domain.entity.*;
import com.franciscomaath.resenhaapi.domain.enums.*;
import com.franciscomaath.resenhaapi.domain.exception.BusinessException;
import com.franciscomaath.resenhaapi.domain.exception.InvalidStateException;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.BetSlipRepository;
import com.franciscomaath.resenhaapi.domain.repository.TransactionRepository;
import com.franciscomaath.resenhaapi.domain.enums.TransactionType;
import com.franciscomaath.resenhaapi.domain.repository.MarketRepository;
import com.franciscomaath.resenhaapi.domain.repository.OutcomeRepository;
import com.franciscomaath.resenhaapi.domain.repository.PlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.RoundRepository;
import com.franciscomaath.resenhaapi.domain.repository.TeamRepository;
import com.franciscomaath.resenhaapi.domain.repository.CompetitionRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupTournamentRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentPlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentWalletRepository;
import com.franciscomaath.resenhaapi.mapper.TournamentMapper;
import com.franciscomaath.resenhaapi.service.BetService;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.service.GoalMarketsOddsCalculator;
import com.franciscomaath.resenhaapi.service.OddsCalculatorService;
import com.franciscomaath.resenhaapi.service.TournamentWalletProvisioningService;
import com.franciscomaath.resenhaapi.service.TournamentService;
import com.franciscomaath.resenhaapi.service.dto.H2HRecord;
import com.franciscomaath.resenhaapi.service.dto.OddsResult;
import com.franciscomaath.resenhaapi.service.validator.TournamentGroupConfigValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentServiceImpl implements TournamentService {

    private final TournamentRepository tournamentRepository;
    private final PlayerRepository playerRepository;
    private final TournamentMapper tournamentMapper;
    private final TournamentPlayerRepository tournamentPlayerRepository;
    private final TeamRepository teamRepository;
    private final RoundRepository roundRepository;
    private final CurrentUserContext currentUserContext;
    private final EventRepository eventRepository;
    private final OddsCalculatorService oddsCalculatorService;
    private final GoalMarketsOddsCalculator goalMarketsOddsCalculator;
    private final MarketRepository marketRepository;
    private final OutcomeRepository outcomeRepository;
    private final OddsProperties oddsProperties;
    private final BetService betService;
    private final TournamentGroupConfigValidator tournamentGroupConfigValidator;
    private final CompetitionRepository competitionRepository;
    private final GroupTournamentRepository groupTournamentRepository;
    private final GroupAuthorizationService groupAuthorizationService;
    private final TournamentWalletProvisioningService tournamentWalletProvisioningService;
    private final BetSlipRepository betSlipRepository;
    private final TransactionRepository transactionRepository;
    private final TournamentWalletRepository tournamentWalletRepository;

    private static final Comparator<PlayerStatsResponseDTO> STANDINGS_COMPARATOR =
            Comparator.comparingInt(PlayerStatsResponseDTO::getPoints).reversed()
                    .thenComparing(Comparator.comparingInt(PlayerStatsResponseDTO::getGoalDifference).reversed())
                    .thenComparing(Comparator.comparingInt(PlayerStatsResponseDTO::getGoalsScored).reversed())
                    .thenComparing(Comparator.comparing(PlayerStatsResponseDTO::getCurrentElo).reversed());

    private static final Comparator<TeamStatsResponseDTO> TEAM_STANDINGS_COMPARATOR =
            Comparator.comparingInt(TeamStatsResponseDTO::getPoints).reversed()
                    .thenComparing(Comparator.comparingInt(TeamStatsResponseDTO::getGoalDifference).reversed())
                    .thenComparing(Comparator.comparingInt(TeamStatsResponseDTO::getGoalsScored).reversed());

    private void attachCurrentGroup(Tournament tournament) {
        attachCurrentGroupWithMarketTypes(tournament, null);
    }

    private void attachCurrentGroupWithMarketTypes(Tournament tournament, Set<MarketType> marketTypes) {
        Long groupId = currentUserContext.getRequiredGroupId();
        Optional<GroupTournament> existingGroupTournament =
                groupTournamentRepository.findByTournamentIdAndGroupId(tournament.getId(), groupId);
        if (existingGroupTournament.isPresent()) {
            tournamentWalletProvisioningService.provisionForGroupTournament(existingGroupTournament.get());
            return;
        }

        if (tournament.getType() == TournamentType.FIFA_MATCH
                && groupTournamentRepository.existsByTournamentId(tournament.getId())) {
            throw new BusinessException("FIFA_MATCH tournaments can belong to only one group.");
        }

        GroupTournament groupTournament = GroupTournament.builder()
                .group(currentUserContext.getRequiredGroup())
                .tournament(tournament)
                .marketTypes(marketTypes != null ? marketTypes : new HashSet<>())
                .build();
        groupTournament = groupTournamentRepository.save(groupTournament);
        tournamentWalletProvisioningService.provisionForGroupTournament(groupTournament);
    }

    @Override
    @Transactional(readOnly = true)
    public TournamentResponseDTO getTournamentById(Long id) {
        groupAuthorizationService.requireTournamentAccess(id);
        Tournament tournament = tournamentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", id));
        return tournamentMapper.toResponse(tournament, getCurrentGroupTournamentId(id));
    }

    @Override
    @Transactional
    public TournamentResponseDTO updateTournament(Long id, TournamentPatchRequestDTO dto) {
        Tournament tournament = tournamentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", id));

        if (tournament.getType() == TournamentType.REAL_FOOTBALL) {
            throw new BusinessException("Cannot edit REAL_FOOTBALL tournaments globally.");
        }

        groupAuthorizationService.requireTournamentAdmin(id);

        if (tournament.getStatus() != TournamentStatus.CREATED) {
            throw new BusinessException("Only CREATED tournaments can be edited.");
        }

        if (dto.getName() != null) tournament.setName(dto.getName());
        if (dto.getFormat() != null) tournament.setFormat(dto.getFormat());
        if (dto.getGenerationMode() != null) tournament.setGenerationMode(dto.getGenerationMode());
        if (dto.getHasThirdPlaceMatch() != null) tournament.setHasThirdPlaceMatch(dto.getHasThirdPlaceMatch());
        if (dto.getNumberOfGroups() != null) tournament.setNumberOfGroups(dto.getNumberOfGroups());
        if (dto.getPlayersAdvancingPerGroup() != null) tournament.setPlayersAdvancingPerGroup(dto.getPlayersAdvancingPerGroup());

        tournament = tournamentRepository.save(tournament);
        return tournamentMapper.toResponse(tournament, getCurrentGroupTournamentId(id));
    }

    @Override
    @Transactional
    public void softDeleteTournament(Long id) {
        Tournament tournament = tournamentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", id));

        if (tournament.getType() == TournamentType.REAL_FOOTBALL) {
            groupAuthorizationService.requireCurrentGroupOwner();
        } else {
            groupAuthorizationService.requireTournamentAdmin(id);
        }

        if (tournament.getStatus() != TournamentStatus.CREATED && tournament.getStatus() != TournamentStatus.CANCELLED) {
            throw new BusinessException("Cannot delete a tournament that has already started.");
        }

        GroupTournament groupTournament = getCurrentGroupTournament(id);
        if (groupTournament == null) {
            throw new ResourceNotFoundException("Tournament not found in current group.");
        }

        List<Event> events = eventRepository.findAllByTournamentId(groupTournament.getTournament().getId());
        events.forEach(SoftDeletable::softDelete);
        eventRepository.saveAll(events);

        List<BetSlip> betSlips = betSlipRepository.findByGroupTournamentId(groupTournament.getId());
        betSlips.forEach(SoftDeletable::softDelete);
        betSlipRepository.saveAll(betSlips);

        List<Transaction> transactions = transactionRepository.findByTournamentWalletGroupTournamentId(groupTournament.getId());
        transactions.forEach(SoftDeletable::softDelete);
        transactionRepository.saveAll(transactions);

        if (tournament.getType() == TournamentType.REAL_FOOTBALL) {
            groupTournament.softDelete();
            groupTournamentRepository.save(groupTournament);
        } else {
            tournament.softDelete();
            tournamentRepository.save(tournament);
        }
    }

    @Override
    @Transactional
    public void cancelTournament(Long id) {
        Tournament tournament = tournamentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", id));

        if (tournament.getType() == TournamentType.REAL_FOOTBALL) {
            currentUserContext.requireAdmin();
        } else {
            groupAuthorizationService.requireTournamentAdmin(id);
        }

        if (tournament.getStatus() != TournamentStatus.CREATED && tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new BusinessException("Only CREATED or IN_PROGRESS tournaments can be cancelled.");
        }

        tournament.setStatus(TournamentStatus.CANCELLED);
        tournamentRepository.save(tournament);

        List<Event> events = eventRepository.findAllByTournamentId(id);
        for (Event event : events) {
            if (event.getStatus() != EventStatus.COMPLETED && event.getStatus() != EventStatus.CANCELLED) {
                event.setStatus(EventStatus.CANCELLED);
                eventRepository.save(event);

                marketRepository.findAllByEventId(event.getId()).forEach(market -> {
                    market.setStatus(MarketStatus.CANCELLED);
                    marketRepository.save(market);
                });

                betService.cancelBetsForEvent(event.getId());
            }
        }
    }

    @Override
    @Transactional
    public TournamentResponseDTO create(TournamentRequestDTO dto) {
        TournamentType type = dto.getType() != null ? dto.getType() : TournamentType.FIFA_MATCH;
        groupAuthorizationService.requireCurrentGroupAdmin();

        GenerationMode generationMode = dto.getGenerationMode() != null
                ? dto.getGenerationMode()
                : GenerationMode.MANUAL;

        Boolean hasThirdPlaceMatch = dto.getHasThirdPlaceMatch() != null
                ? dto.getHasThirdPlaceMatch()
                : false;

        Competition competition = null;
        if (type == TournamentType.REAL_FOOTBALL) {
            Long competitionId = dto.getCompetitionId();
            if (competitionId == null) {
                throw new BusinessException("competitionId is required for REAL_FOOTBALL tournaments.");
            }
            competition = competitionRepository.findById(competitionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Competition", "id", competitionId));
            Optional<Tournament> existing = tournamentRepository.findByCompetitionIdAndType(competitionId, TournamentType.REAL_FOOTBALL);
            if (existing.isPresent()) {
                Tournament tournament = existing.get();
                attachCurrentGroup(tournament);
                return tournamentMapper.toResponse(tournament, getCurrentGroupTournamentId(tournament.getId()));
            }
            currentUserContext.requireAdmin();
        } else if (dto.getCompetitionId() != null) {
            throw new BusinessException("competitionId must be null for FIFA_MATCH tournaments.");
        }
        Tournament tournament = Tournament.builder()
                .name(competition == null ? dto.getName() : competition.getName())
                .uuid(UUID.randomUUID())
                .type(type)
                .format(dto.getFormat())
                .status(TournamentStatus.CREATED)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .generationMode(generationMode)
                .hasThirdPlaceMatch(hasThirdPlaceMatch)
                .competition(competition)
                .build();

        tournament = tournamentRepository.save(tournament);
        attachCurrentGroupWithMarketTypes(tournament, dto.getMarketTypes());
        log.info("Created tournament id={}, name={}, format={}, type={}", tournament.getId(), tournament.getName(), tournament.getFormat(), type);
        GroupTournament groupTournament = getCurrentGroupTournament(tournament.getId());
        return tournamentMapper.toResponse(tournament, groupTournament != null ? groupTournament.getId() : null,
                groupTournament != null ? groupTournament.getMarketTypes() : null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TournamentResponseDTO> findAll(Pageable pageable) {
        return tournamentRepository.findAllByGroupId(currentUserContext.getRequiredGroupId(), pageable)
                .map(tournament -> tournamentMapper.toResponse(
                        tournament,
                        getCurrentGroupTournamentId(tournament.getId())));
    }

    @Override
    public TournamentPlayersResponseDTO findPlayersByTournamentId(Long tournamentId) {
        groupAuthorizationService.requireTournamentAccess(tournamentId);
        tournamentRepository.findByIdAndDeletedAtIsNull(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));

        List<TournamentPlayerResponseDTO> playersResponseDTOS = tournamentPlayerRepository.findByTournamentId(tournamentId).stream()
                .map(this::toTournamentPlayerResponse)
                .toList();

        TournamentPlayersResponseDTO response = new TournamentPlayersResponseDTO();
        response.setPlayers(playersResponseDTOS);
        response.setPlayerCount(playersResponseDTOS.size());

        return response;
    }

    @Override
    @Transactional
    public TournamentPlayerResponseDTO addPlayerToTournament(Long tournamentId, TournamentPlayerRequestDTO dto) {
        groupAuthorizationService.requireTournamentAdmin(tournamentId);

        Tournament tournament = tournamentRepository.findByIdAndDeletedAtIsNull(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));

        Player player = playerRepository.findByIdAndGroupId(dto.getPlayerId(), currentUserContext.getRequiredGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Player", "id", dto.getPlayerId()));

        if(tournamentPlayerRepository.existsByTournamentIdAndPlayerId(tournamentId, player.getId())) {
            throw new BusinessException("Player is already in this Tournament.");
        }

        if(tournament.getStatus() != TournamentStatus.CREATED) {
            throw new BusinessException("Cannot add players to a tournament that has already started.");
        }

        TournamentPlayer tp = TournamentPlayer.builder()
                .tournament(tournament)
                .player(player)
                .team(null) // time definido depois, via PATCH /tournaments/{id}/players/{playerId}/team
                .build();

        try {
            tournamentPlayerRepository.save(tp);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException("Player is already in this Tournament.");
        }

        log.info("Added player id={} to tournament id={}", dto.getPlayerId(), tournamentId);

        return toTournamentPlayerResponse(tp);
    }

    @Override
    @Transactional
    public void removePlayerFromTournament(Long tournamentId, Long playerId) {
        groupAuthorizationService.requireTournamentAdmin(tournamentId);

        Tournament tournament = tournamentRepository.findByIdAndDeletedAtIsNull(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));

        boolean playerInEvent = eventRepository.existsPlayerInTournament(playerId, tournamentId);

        if (tournament.getStatus() != TournamentStatus.CREATED && playerInEvent) {
            throw new BusinessException("Cannot remove players from a tournament that has already started.");
        }

        TournamentPlayer tournamentPlayer = tournamentPlayerRepository.findByTournamentIdAndPlayerId(tournamentId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("TournamentPlayer", "playerId", playerId));

        tournamentPlayerRepository.delete(tournamentPlayer);
        log.info("Removed player id={} from tournament id={}", playerId, tournamentId);
    }

    @Override
    @Transactional
    public TournamentPlayerResponseDTO updateTournamentPlayerTeam(Long tournamentId, Long playerId, PatchTournamentPlayerTeamRequestDTO dto) {
        groupAuthorizationService.requireTournamentAdmin(tournamentId);

        tournamentRepository.findByIdAndDeletedAtIsNull(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));

        Team team = teamRepository.findById(dto.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", dto.getTeamId()));

        TournamentPlayer tournamentPlayer = tournamentPlayerRepository.findByTournamentIdAndPlayerId(tournamentId, playerId)
                .orElseThrow(() -> new ResourceNotFoundException("TournamentPlayer", "playerId", playerId));

        tournamentPlayer.setTeam(team);
        tournamentPlayer = tournamentPlayerRepository.save(tournamentPlayer);
        log.info("Updated team for player id={} in tournament id={} to team id={}", playerId, tournamentId, dto.getTeamId());

        return toTournamentPlayerResponse(tournamentPlayer);
    }

    @Override
    public List<TournamentRoundResponseDTO> findRoundsByTournamentId(Long tournamentId) {
        groupAuthorizationService.requireTournamentAccess(tournamentId);
        tournamentRepository.findByIdAndDeletedAtIsNull(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));

        return roundRepository.findByTournamentIdOrderByRoundOrderAsc(tournamentId).stream()
                .map(tournamentMapper::toRoundResponse)
                .toList();
    }

    @Override
    @Transactional
    public TournamentResponseDTO startTournament(Long tournamentId, StartTournamentRequestDTO dto) {
        groupAuthorizationService.requireTournamentAdmin(tournamentId);

        Tournament tournament = tournamentRepository.findByIdAndDeletedAtIsNull(tournamentId)
                .orElseThrow(() ->  new ResourceNotFoundException("Tournament", "id", tournamentId));

        if(tournament.getStatus() != TournamentStatus.CREATED) {
            throw new BusinessException("Tournament is already started.");
        }

        List<TournamentPlayer> players = tournamentPlayerRepository.findByTournamentId(tournamentId);

        int n = players.size();
        if (n < 2) throw new BusinessException("Mínimo de 2 participantes");

        log.info("Starting tournament id={}, format={}, players={}, mode={}",
                tournamentId, tournament.getFormat(), n, tournament.getGenerationMode());

        List<TournamentRound> rounds = new ArrayList<>();

        if (tournament.getFormat() == TournamentFormat.LEAGUE) {
            int effectiveN = n % 2 == 0 ? n : n + 1;
            for (int i = 1; i <= (effectiveN - 1) * 2; i++) {
                TournamentRound round = new TournamentRound();
                round.setName("Rodada " + i);
                round.setMultiplier(BigDecimal.ONE);   // multiplicador flat para liga
                round.setRoundOrder(i);
                round.setPhaseType(PhaseType.GROUP_STAGE);
                round.setTournament(tournament);
                rounds.add(round);
            }
        }

        if (tournament.getFormat() == TournamentFormat.LEAGUE_BRACKET) {
            if(dto == null) {
                throw new BusinessException("Tournament must have an group configuration.");
            }

            Integer numberOfGroups = dto.getNumberOfGroups();
            Integer playersAdvancingPerGroup = dto.getPlayersAdvancingPerGroup();

            if (numberOfGroups == null || numberOfGroups < 1) {
                throw new BusinessException("numberOfGroups must be set for LEAGUE_BRACKET format.");
            }

            if (playersAdvancingPerGroup == null || playersAdvancingPerGroup < 1) {
                throw new BusinessException("playersAdvancingPerGroup must be at least 1.");
            }

            tournament.setNumberOfGroups(numberOfGroups);
            tournament.setPlayersAdvancingPerGroup(playersAdvancingPerGroup);

            // Create group-stage rounds per group
            int roundsPerGroup = getRoundsPerGroup(n, numberOfGroups, playersAdvancingPerGroup);

            for (int g = 1; g <= numberOfGroups; g++) {
                char letter = (char)(g + 64);
                for (int i = 1; i <= roundsPerGroup; i++) {
                    TournamentRound round = new TournamentRound();
                    String name = "Rodada " + i;
                    if(numberOfGroups != 1) name += " - Grupo " + letter;
                    round.setName(name);
                    round.setMultiplier(BigDecimal.ONE);
                    round.setRoundOrder(i);
                    round.setPhaseType(PhaseType.GROUP_STAGE);
                    round.setGroupNumber(g);
                    round.setTournament(tournament);
                    rounds.add(round);
                }
            }
        }

        int nextPowerOf2 = 0;
        int numRounds = 0;

        if (tournament.getFormat() == TournamentFormat.BRACKET) {
            nextPowerOf2 = computeNextPowerOf2(n);
            numRounds = Integer.numberOfTrailingZeros(nextPowerOf2);

            String[] roundNames = getBracketRoundNames(numRounds);
            BigDecimal[] multipliers = getBracketMultipliers(numRounds);

            for (int i = 0; i < numRounds; i++) {
                TournamentRound round = new TournamentRound();
                round.setName(roundNames[i]);
                round.setMultiplier(multipliers[i]);
                round.setRoundOrder(i + 1);
                round.setPhaseType(PhaseType.KNOCKOUT);
                round.setTournament(tournament);
                rounds.add(round);
            }

            if (Boolean.TRUE.equals(tournament.getHasThirdPlaceMatch()) && numRounds >= 2) {
                TournamentRound thirdPlaceRound = new TournamentRound();
                thirdPlaceRound.setName("3rd Place");
                thirdPlaceRound.setMultiplier(BigDecimal.valueOf(1.4));
                thirdPlaceRound.setRoundOrder(numRounds);
                thirdPlaceRound.setPhaseType(PhaseType.KNOCKOUT);
                thirdPlaceRound.setTournament(tournament);
                rounds.add(numRounds - 1, thirdPlaceRound);
                rounds.get(numRounds).setRoundOrder(numRounds + 1);
            }
        }

        tournament.setRounds(rounds);
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournament.setStartDate(LocalDateTime.now());
        tournament = tournamentRepository.save(tournament);

        if (tournament.getFormat() == TournamentFormat.BRACKET) {
            List<List<Event>> eventsByRound;
            if (tournament.getGenerationMode() == GenerationMode.AUTO) {
                eventsByRound = generateBracketAutoEvents(tournament, players, numRounds, nextPowerOf2);
            } else {
                eventsByRound = createBracketEvents(tournament, getStandardBracketRounds(tournament), numRounds, nextPowerOf2);
            }

            createThirdPlaceEvent(tournament, eventsByRound, numRounds);
        }

        // LEAGUE AUTO: generate round-robin events
        if (tournament.getFormat() == TournamentFormat.LEAGUE && tournament.getGenerationMode() == GenerationMode.AUTO) {
            List<TournamentPlayer> shuffledPlayers = new ArrayList<>(players);
            Collections.shuffle(shuffledPlayers);
            List<Player> playerList = shuffledPlayers.stream()
                    .map(TournamentPlayer::getPlayer)
                    .toList();
            List<TournamentRound> sortedRounds = tournament.getRounds().stream()
                    .sorted(Comparator.comparingInt(TournamentRound::getRoundOrder))
                    .toList();
            generateRoundRobinForGroup(tournament, playerList, sortedRounds, true, false);
        }

        // LEAGUE_BRACKET AUTO: generate group-stage events
        if (tournament.getFormat() == TournamentFormat.LEAGUE_BRACKET && tournament.getGenerationMode() == GenerationMode.AUTO) {
            generateLeagueBracketAutoEvents(tournament, players);
        }

        return tournamentMapper.toResponse(tournament, getCurrentGroupTournamentId(tournament.getId()));
    }

    private static int getRoundsPerGroup(int n, Integer numberOfGroups, Integer playersAdvancingPerGroup) {
        int playersPerGroup = n / numberOfGroups;

        if (playersPerGroup < 2) {
            throw new BusinessException("Not enough players per group. Need at least 2 players per group.");
        }

        if(playersAdvancingPerGroup > playersPerGroup){
            throw new BusinessException("Players advancing per group cannot be greater than players per group.");
        }

        int totalAdvancing = numberOfGroups * playersAdvancingPerGroup;
        if (totalAdvancing < 2) {
            throw new BusinessException("Total advancing players must be at least 2.");
        }

        if ((totalAdvancing & (totalAdvancing - 1)) != 0) {
            throw new BusinessException("Total advancing players (groups × playersAdvancingPerGroup) must be a power of 2.");
        }

        int effectiveSize = playersPerGroup % 2 == 0 ? playersPerGroup : playersPerGroup + 1;
        int roundsPerGroup = (effectiveSize - 1) * 2;
        return roundsPerGroup;
    }

    @Override
    @Transactional
    public TournamentResponseDTO advanceToBracket(Long tournamentId) {
        groupAuthorizationService.requireTournamentAdmin(tournamentId);
        return advanceToBracketInternal(tournamentId);
    }

    @Transactional
    public TournamentResponseDTO advanceToBracketInternal(Long tournamentId) {

        Tournament tournament = tournamentRepository.findByIdAndDeletedAtIsNull(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));

        if (tournament.getFormat() != TournamentFormat.LEAGUE_BRACKET) {
            throw new BusinessException("Tournament is not LEAGUE_BRACKET format.");
        }

        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new BusinessException("Tournament is not in progress.");
        }

        // Check if all GROUP_STAGE events are COMPLETED
        List<Event> allEvents = eventRepository.findAllByTournamentId(tournamentId);
        List<Event> groupStageEvents = allEvents.stream()
                .filter(e -> e.getRound().getPhaseType() == PhaseType.GROUP_STAGE)
                .toList();

        boolean allGroupCompleted = groupStageEvents.stream()
                .allMatch(e -> e.getStatus() == EventStatus.COMPLETED || e.getStatus() == EventStatus.CANCELLED);

        // Validate all group stage events are completed
        if (!allGroupCompleted) {
            long pendingCount = groupStageEvents.stream()
                    .filter(e -> e.getStatus() != EventStatus.COMPLETED)
                    .count();
            throw new InvalidStateException(
                    "Cannot advance to bracket: " + pendingCount +
                    " group stage event(s) are not yet completed. " +
                    "Please register all match results before advancing.");
        }

        // Check if knockout phase already exists
        boolean hasKnockoutRounds = tournament.getRounds().stream()
                .anyMatch(r -> r.getPhaseType() == PhaseType.KNOCKOUT);
        if (hasKnockoutRounds) {
            throw new BusinessException("Knockout phase already exists.");
        }

        // Calculate standings per group
        int numberOfGroups = tournament.getNumberOfGroups();
        int playersAdvancingPerGroup = tournament.getPlayersAdvancingPerGroup();
        List<Player> advancingPlayers = new ArrayList<>();

        for (int g = 1; g <= numberOfGroups; g++) {
            List<Player> groupStandings = calculateGroupStandings(tournament, g, groupStageEvents);
            int advancingCount = Math.min(playersAdvancingPerGroup, groupStandings.size());
            for (int i = 0; i < advancingCount; i++) {
                advancingPlayers.add(groupStandings.get(i));
            }
        }

        log.info("Advancing tournament id={} to bracket, {} players advancing",
                tournamentId, advancingPlayers.size());

        int maxExistingOrder = tournament.getRounds().stream()
                .mapToInt(TournamentRound::getRoundOrder)
                .max()
                .orElse(0);

        return performBracketAdvancement(tournament, advancingPlayers, maxExistingOrder);
    }

    @Override
    @Transactional
    public TournamentResponseDTO forceAdvanceToBracket(Long tournamentId) {
        groupAuthorizationService.requireTournamentAdmin(tournamentId);

        Tournament tournament = tournamentRepository.findByIdAndDeletedAtIsNull(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));

        if (tournament.getFormat() != TournamentFormat.LEAGUE_BRACKET) {
            throw new BusinessException("Tournament is not LEAGUE_BRACKET format.");
        }

        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new BusinessException("Tournament is not in progress.");
        }

        boolean hasKnockoutRounds = tournament.getRounds().stream()
                .anyMatch(r -> r.getPhaseType() == PhaseType.KNOCKOUT);
        if (hasKnockoutRounds) {
            throw new BusinessException("Knockout phase already exists.");
        }

        List<Event> allEvents = eventRepository.findAllByTournamentId(tournamentId);
        List<Event> groupStageEvents = allEvents.stream()
                .filter(e -> e.getRound().getPhaseType() == PhaseType.GROUP_STAGE)
                .toList();

        boolean hasInProgress = groupStageEvents.stream()
                .anyMatch(e -> e.getStatus() == EventStatus.IN_PROGRESS);
        if (hasInProgress) {
            throw new InvalidStateException(
                    "Cannot force advance: there are in-progress group stage events. Finish them first.");
        }

        List<Event> cancellableEvents = groupStageEvents.stream()
                .filter(e -> e.getStatus() != EventStatus.COMPLETED)
                .toList();

        log.warn("Force advancing tournament id={} to bracket, cancelling {} events",
                tournamentId, cancellableEvents.size());

        for (Event event : cancellableEvents) {
            event.setStatus(EventStatus.CANCELLED);
            eventRepository.save(event);

            marketRepository.findAllByEventId(event.getId()).forEach(market -> {
                market.setStatus(MarketStatus.CANCELLED);
                marketRepository.save(market);
            });

            betService.cancelBetsForEvent(event.getId());
            log.info("Cancelled event id={}, refunded bets", event.getId());
        }

        List<Event> completedGroupStageEvents = groupStageEvents.stream()
                .filter(e -> e.getStatus() == EventStatus.COMPLETED)
                .toList();

        int numberOfGroups = tournament.getNumberOfGroups();
        int playersAdvancingPerGroup = tournament.getPlayersAdvancingPerGroup();
        List<Player> advancingPlayers = new ArrayList<>();

        for (int g = 1; g <= numberOfGroups; g++) {
            List<Player> groupStandings = calculateGroupStandings(tournament, g, completedGroupStageEvents);
            int advancingCount = Math.min(playersAdvancingPerGroup, groupStandings.size());
            for (int i = 0; i < advancingCount; i++) {
                advancingPlayers.add(groupStandings.get(i));
            }
        }

        int maxExistingOrder = tournament.getRounds().stream()
                .mapToInt(TournamentRound::getRoundOrder)
                .max()
                .orElse(0);

        return performBracketAdvancement(tournament, advancingPlayers, maxExistingOrder);
    }

    private TournamentResponseDTO performBracketAdvancement(
            Tournament tournament, List<Player> advancingPlayers, int maxExistingOrder) {
        int n = advancingPlayers.size();
        int nextPowerOf2 = computeNextPowerOf2(n);
        int numRounds = Integer.numberOfTrailingZeros(nextPowerOf2);

        List<TournamentRound> rounds = new ArrayList<>(tournament.getRounds());
        String[] roundNames = getBracketRoundNames(numRounds);
        BigDecimal[] multipliers = getBracketMultipliers(numRounds);

        for (int i = 0; i < numRounds; i++) {
            TournamentRound round = new TournamentRound();
            round.setName(roundNames[i]);
            round.setMultiplier(multipliers[i]);
            round.setRoundOrder(maxExistingOrder + i + 1);
            round.setPhaseType(PhaseType.KNOCKOUT);
            round.setTournament(tournament);
            rounds.add(round);
        }

        if (Boolean.TRUE.equals(tournament.getHasThirdPlaceMatch()) && numRounds >= 2) {
            TournamentRound thirdPlaceRound = new TournamentRound();
            thirdPlaceRound.setName("3rd Place");
            thirdPlaceRound.setMultiplier(BigDecimal.valueOf(1.4));
            thirdPlaceRound.setRoundOrder(maxExistingOrder + numRounds);
            thirdPlaceRound.setPhaseType(PhaseType.KNOCKOUT);
            thirdPlaceRound.setTournament(tournament);
            rounds.add(rounds.size() - 1, thirdPlaceRound);
            rounds.get(rounds.size() - 1).setRoundOrder(maxExistingOrder + numRounds + 1);
        }

        tournament.setRounds(rounds);
        tournament = tournamentRepository.save(tournament);

        final Tournament finalTournament = tournament;
        List<List<Event>> eventsByRound;
        if (tournament.getGenerationMode() == GenerationMode.AUTO) {
            eventsByRound = generateBracketAutoEvents(tournament,
                advancingPlayers.stream().map(p -> {
                    TournamentPlayer tp = new TournamentPlayer();
                    tp.setPlayer(p);
                    tp.setTournament(finalTournament);
                    return tp;
                }).toList(),
                numRounds, nextPowerOf2);
        } else {
            eventsByRound = createBracketEvents(tournament, getStandardBracketRounds(tournament), numRounds, nextPowerOf2);
        }

        createThirdPlaceEvent(tournament, eventsByRound, numRounds);

        return tournamentMapper.toResponse(tournament, getCurrentGroupTournamentId(tournament.getId()));
    }

    private Long getCurrentGroupTournamentId(Long tournamentId) {
        GroupTournament gt = getCurrentGroupTournament(tournamentId);
        return gt != null ? gt.getId() : null;
    }

    private GroupTournament getCurrentGroupTournament(Long tournamentId) {
        return groupTournamentRepository.findByTournamentIdAndGroupId(tournamentId, currentUserContext.getRequiredGroupId())
                .orElse(null);
    }

    @Override
    public TournamentScoreboardResponseDTO getScoreboard(Long tournamentId) {
        groupAuthorizationService.requireTournamentAccess(tournamentId);
        Tournament tournament = tournamentRepository.findByIdAndDeletedAtIsNull(tournamentId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));

        TournamentScoreboardResponseDTO response = new TournamentScoreboardResponseDTO();
        response.setTournamentId(tournament.getId());
        response.setTournamentName(tournament.getName());
        response.setFormat(tournament.getFormat().name());

        List<Event> allEvents = eventRepository.findAllByTournamentId(tournament.getId());

        if (tournament.getFormat() == TournamentFormat.LEAGUE) {
            response.setEntries(buildLeagueScoreboard(tournament, allEvents));
        } else if (tournament.getFormat() == TournamentFormat.BRACKET) {
            response.setPlacements(buildBracketPlacements(tournament, allEvents));
        } else if (tournament.getFormat() == TournamentFormat.LEAGUE_BRACKET) {
            response.setGroups(buildLeagueBracketGroups(tournament, allEvents));
            boolean hasKnockout = tournament.getRounds().stream()
                    .anyMatch(r -> r.getPhaseType() == PhaseType.KNOCKOUT);
            if (hasKnockout) {
                response.setPlacements(buildBracketPlacements(tournament, allEvents));
            }
        }

        return response;
    }

    private List<PlayerStatsResponseDTO> buildLeagueScoreboard(Tournament tournament, List<Event> allEvents) {
        List<TournamentPlayer> tournamentPlayers = tournamentPlayerRepository.findByTournamentId(tournament.getId());
        List<Event> events = allEvents.stream()
                .filter(e -> e.getRound().getPhaseType() == PhaseType.GROUP_STAGE && e.getStatus() == EventStatus.COMPLETED)
                .collect(Collectors.toList());

        Map<Long, StatsAccumulator> statsByPlayer = calculateStats(events);

        List<PlayerStatsResponseDTO> entries = new ArrayList<>();
        for (TournamentPlayer tp : tournamentPlayers) {
            entries.add(toPlayerStatsDTO(tp.getPlayer(), statsByPlayer));
        }

        entries.sort(STANDINGS_COMPARATOR);

        return entries;
    }

    private List<GroupStandingsDTO> buildLeagueBracketGroups(Tournament tournament, List<Event> allEvents) {
        List<Event> groupStageEvents = allEvents.stream()
                .filter(e -> e.getRound().getPhaseType() == PhaseType.GROUP_STAGE && (e.getStatus() == EventStatus.COMPLETED || e.getStatus() == EventStatus.IN_PROGRESS))
                .toList();

        if (tournament.getType() == TournamentType.REAL_FOOTBALL) {
            return buildRealFootballGroups(tournament, groupStageEvents);
        }

        return buildFifaMatchGroups(tournament, groupStageEvents);
    }

    private List<GroupStandingsDTO> buildFifaMatchGroups(Tournament tournament, List<Event> groupStageEvents) {
        List<GroupStandingsDTO> groups = new ArrayList<>();
        int numberOfGroups = tournament.getNumberOfGroups();

        Map<Long, StatsAccumulator> statsByPlayer = calculateStats(groupStageEvents);

        for (int g = 1; g <= numberOfGroups; g++) {
            List<Player> groupStandings = calculateGroupStandings(tournament, g, groupStageEvents);
            GroupStandingsDTO groupDTO = new GroupStandingsDTO();
            groupDTO.setGroupNumber(g);

            List<PlayerStatsResponseDTO> standings = new ArrayList<>();
            for (Player player : groupStandings) {
                standings.add(toPlayerStatsDTO(player, statsByPlayer));
            }
            groupDTO.setStandings(standings);
            groups.add(groupDTO);
        }

        return groups;
    }

    private List<GroupStandingsDTO> buildRealFootballGroups(Tournament tournament, List<Event> groupStageEvents) {
        List<GroupStandingsDTO> groups = new ArrayList<>();

        List<String> groupNames = tournament.getRounds().stream()
                .filter(r -> r.getPhaseType() == PhaseType.GROUP_STAGE)
                .map(TournamentRound::getName)
                .distinct()
                .sorted()
                .toList();

        for (String groupName : groupNames) {
            List<Event> groupEvents = groupStageEvents.stream()
                    .filter(e -> groupName.equals(e.getRound().getName()))
                    .toList();

            List<Team> teamStandings = calculateTeamGroupStandings(groupName, groupEvents);

            Map<Long, StatsAccumulator> statsByTeam = calculateTeamStats(groupEvents);

            GroupStandingsDTO groupDTO = new GroupStandingsDTO();
            groupDTO.setGroupName(groupName);

            List<TeamStatsResponseDTO> standings = new ArrayList<>();
            for (Team team : teamStandings) {
                standings.add(toTeamStatsDTO(team, statsByTeam));
            }
            groupDTO.setTeamStandings(standings);
            groups.add(groupDTO);
        }

        return groups;
    }

    private List<BracketPlacementDTO> buildBracketPlacements(Tournament tournament, List<Event> allEvents) {
        List<BracketPlacementDTO> placements = new ArrayList<>();
        // Find final event
        Event finalEvent = allEvents.stream()
                .filter(e -> e.getRound().getName().equals("Final") && e.getStatus() == EventStatus.COMPLETED)
                .findFirst()
                .orElse(null);

        if (finalEvent != null) {
            Player champion = determineWinner(finalEvent);
            if (champion != null) {
                placements.add(createPlacement(champion, 1, "Champion"));

                Player runnerUp = determineLoser(finalEvent);
                if (runnerUp != null) {
                    placements.add(createPlacement(runnerUp, 2, "Runner-up"));
                }
            }
        }

        // 3rd place match
        Event thirdPlaceEvent = allEvents.stream()
                .filter(e -> "3rd Place".equals(e.getRound().getName()) && e.getStatus() == EventStatus.COMPLETED)
                .findFirst()
                .orElse(null);

        if (thirdPlaceEvent != null) {
            Player thirdPlace = determineWinner(thirdPlaceEvent);
            if (thirdPlace != null) {
                placements.add(createPlacement(thirdPlace, 3, "3rd Place"));

                Player fourthPlace = determineLoser(thirdPlaceEvent);
                if (fourthPlace != null) {
                    placements.add(createPlacement(fourthPlace, 4, "4th Place"));
                }
            }
        }

        // Remaining players: sorted by furthest round reached
        List<Event> knockoutEvents = allEvents.stream()
                .filter(e -> e.getRound().getPhaseType() == PhaseType.KNOCKOUT && e.getStatus() == EventStatus.COMPLETED)
                .toList();

        Map<Long, String> playerEliminationRound = new HashMap<>();
        for (Event event : knockoutEvents) {
            Player loser = determineLoser(event);
            if (loser != null && !playerEliminationRound.containsKey(loser.getId())) {
                playerEliminationRound.put(loser.getId(), event.getRound().getName());
            }
        }

        int position = placements.size() + 1;
        List<Map.Entry<Long, String>> sortedEliminations = new ArrayList<>(playerEliminationRound.entrySet());
        // Sort by round order (final = highest, earlier = lower)
        sortedEliminations.sort((a, b) -> {
            int orderA = getRoundOrder(tournament, a.getValue());
            int orderB = getRoundOrder(tournament, b.getValue());
            return Integer.compare(orderB, orderA);
        });

        List<Player> allPlayers = playerRepository.findAllById(playerEliminationRound.keySet());
        Map<Long, Player> playerMap = allPlayers.stream()
                .collect(Collectors.toMap(Player::getId, p -> p));

        for (Map.Entry<Long, String> entry : sortedEliminations) {
            Player player = playerMap.get(entry.getKey());
            if (player != null && placements.stream().noneMatch(p -> p.getPlayerId().equals(player.getId()))) {
                placements.add(createPlacement(player, position++, entry.getValue()));
            }
        }

        return placements;
    }

    private Map<Long, StatsAccumulator> calculateStats(List<Event> events) {
        Map<Long, StatsAccumulator> statsByPlayer = new HashMap<>();
        for (Event event : events) {
            if (event.getHomeScore() == null || event.getAwayScore() == null) {
                continue;
            }
            if (event.getPlayerHome() == null || event.getPlayerAway() == null) {
                continue;
            }

            Long homeId = event.getPlayerHome().getId();
            Long awayId = event.getPlayerAway().getId();
            StatsAccumulator homeStats = statsByPlayer.computeIfAbsent(homeId, k -> new StatsAccumulator());
            StatsAccumulator awayStats = statsByPlayer.computeIfAbsent(awayId, k -> new StatsAccumulator());

            int homeScore = event.getHomeScore();
            int awayScore = event.getAwayScore();
            homeStats.matchesPlayed++;
            homeStats.goalsScored += homeScore;
            homeStats.goalsConceded += awayScore;
            awayStats.matchesPlayed++;
            awayStats.goalsScored += awayScore;
            awayStats.goalsConceded += homeScore;

            if (homeScore > awayScore) {
                homeStats.wins++;
                awayStats.losses++;
            } else if (homeScore < awayScore) {
                homeStats.losses++;
                awayStats.wins++;
            } else {
                homeStats.draws++;
                awayStats.draws++;
            }
        }
        return statsByPlayer;
    }

    private Map<Long, StatsAccumulator> calculateTeamStats(List<Event> events) {
        Map<Long, StatsAccumulator> statsByTeam = new HashMap<>();
        for (Event event : events) {
            if (event.getHomeScore() == null || event.getAwayScore() == null) {
                continue;
            }
            if (event.getTeamHome() == null || event.getTeamAway() == null) {
                continue;
            }

            Long homeId = event.getTeamHome().getId();
            Long awayId = event.getTeamAway().getId();
            StatsAccumulator homeStats = statsByTeam.computeIfAbsent(homeId, k -> new StatsAccumulator());
            StatsAccumulator awayStats = statsByTeam.computeIfAbsent(awayId, k -> new StatsAccumulator());

            int homeScore = event.getHomeScore();
            int awayScore = event.getAwayScore();
            homeStats.matchesPlayed++;
            homeStats.goalsScored += homeScore;
            homeStats.goalsConceded += awayScore;
            awayStats.matchesPlayed++;
            awayStats.goalsScored += awayScore;
            awayStats.goalsConceded += homeScore;

            if (homeScore > awayScore) {
                homeStats.wins++;
                awayStats.losses++;
            } else if (homeScore < awayScore) {
                homeStats.losses++;
                awayStats.wins++;
            } else {
                homeStats.draws++;
                awayStats.draws++;
            }
        }
        return statsByTeam;
    }

    private PlayerStatsResponseDTO toPlayerStatsDTO(Player player, Map<Long, StatsAccumulator> statsByPlayer) {
        StatsAccumulator acc = statsByPlayer.getOrDefault(player.getId(), new StatsAccumulator());
        PlayerStatsResponseDTO dto = new PlayerStatsResponseDTO();
        dto.setPlayerId(player.getId());
        dto.setPlayerName(player.getName());
        dto.setMatchesPlayed(acc.matchesPlayed);
        dto.setWins(acc.wins);
        dto.setLosses(acc.losses);
        dto.setDraws(acc.draws);
        dto.setGoalsScored(acc.goalsScored);
        dto.setGoalsConceded(acc.goalsConceded);
        dto.setGoalDifference(acc.goalsScored - acc.goalsConceded);
        dto.setPoints(acc.wins * 3 + acc.draws);
        dto.setCurrentElo(player.getCurrentElo());
        return dto;
    }

    private TeamStatsResponseDTO toTeamStatsDTO(Team team, Map<Long, StatsAccumulator> statsByTeam) {
        StatsAccumulator acc = statsByTeam.getOrDefault(team.getId(), new StatsAccumulator());
        TeamStatsResponseDTO dto = new TeamStatsResponseDTO();
        dto.setTeamId(team.getId());
        dto.setTeamName(team.getName());
        dto.setMatchesPlayed(acc.matchesPlayed);
        dto.setWins(acc.wins);
        dto.setLosses(acc.losses);
        dto.setDraws(acc.draws);
        dto.setGoalsScored(acc.goalsScored);
        dto.setGoalsConceded(acc.goalsConceded);
        dto.setGoalDifference(acc.goalsScored - acc.goalsConceded);
        dto.setPoints(acc.wins * 3 + acc.draws);
        return dto;
    }

    private BracketPlacementDTO createPlacement(Player player, int position, String eliminationRound) {
        BracketPlacementDTO dto = new BracketPlacementDTO();
        dto.setPlayerId(player.getId());
        dto.setPlayerName(player.getName());
        dto.setPosition(position);
        dto.setEliminationRound(eliminationRound);
        return dto;
    }

    private int getRoundOrder(Tournament tournament, String roundName) {
        return tournament.getRounds().stream()
                .filter(r -> r.getName().equals(roundName))
                .map(TournamentRound::getRoundOrder)
                .findFirst()
                .orElse(0);
    }

    private Player determineWinner(Event event) {
        if (event.getPlayerHome() == null || event.getPlayerAway() == null) {
            return event.getPlayerHome() != null ? event.getPlayerHome() : event.getPlayerAway();
        }

        boolean usePenalties = event.getPenaltiesHome() != null && event.getPenaltiesAway() != null;
        if (usePenalties) {
            return event.getPenaltiesHome() > event.getPenaltiesAway()
                    ? event.getPlayerHome() : event.getPlayerAway();
        }

        if (event.getHomeScore().equals(event.getAwayScore())) {
            throw new InvalidStateException(
                    "Match " + event.getId() + " is tied (" + event.getHomeScore() +
                    "-" + event.getAwayScore() + ") with no penalties. Cannot determine winner.");
        }

        return event.getHomeScore() > event.getAwayScore()
                ? event.getPlayerHome() : event.getPlayerAway();
    }

    private Player determineLoser(Event event) {
        Player winner = determineWinner(event);
        if (winner == null || event.getPlayerHome() == null || event.getPlayerAway() == null) {
            return null;
        }
        return winner.getId().equals(event.getPlayerHome().getId())
                ? event.getPlayerAway() : event.getPlayerHome();
    }

    private TournamentPlayerResponseDTO toTournamentPlayerResponse(TournamentPlayer tournamentPlayer) {
        TournamentPlayerResponseDTO response = new TournamentPlayerResponseDTO();
        response.setTournamentPlayerId(tournamentPlayer.getId());
        response.setTournamentId(tournamentPlayer.getTournament().getId());
        response.setPlayerId(tournamentPlayer.getPlayer().getId());
        response.setPlayerName(tournamentPlayer.getPlayer().getName());
        response.setGroupNumber(tournamentPlayer.getGroupNumber());

        if (tournamentPlayer.getTeam() != null) {
            response.setTeamId(tournamentPlayer.getTeam().getId());
            response.setTeamName(tournamentPlayer.getTeam().getName());
        }

        return response;
    }

    private static class StatsAccumulator {
        int matchesPlayed = 0;
        int wins = 0;
        int losses = 0;
        int draws = 0;
        int goalsScored = 0;
        int goalsConceded = 0;
    }

    private List<Player> calculateGroupStandings(Tournament tournament, int groupNumber, List<Event> groupStageEvents) {
        if(tournament.getStatus() == TournamentStatus.CREATED){
            return Collections.emptyList();
        }

        // Get players in this group
        List<TournamentPlayer> groupPlayers = tournament.getTournamentPlayers().stream()
                .filter(tp -> groupNumber == tp.getGroupNumber())
                .toList();

        // Get events for this group
        List<Event> groupEvents = groupStageEvents.stream()
                .filter(e -> e.getRound().getGroupNumber() != null && e.getRound().getGroupNumber() == groupNumber)
                .toList();

        Map<Long, StatsAccumulator> statsByPlayer = calculateStats(groupEvents);

        List<Player> players = groupPlayers.stream()
                .map(TournamentPlayer::getPlayer)
                .toList();

        Map<Long, Player> playerById = players.stream()
                .collect(Collectors.toMap(Player::getId, p -> p));

        List<PlayerStatsResponseDTO> playerStats = new ArrayList<>(players.stream()
                .map(p -> toPlayerStatsDTO(p, statsByPlayer))
                .toList());

        playerStats.sort(STANDINGS_COMPARATOR);

        return playerStats.stream()
                .map(dto -> playerById.get(dto.getPlayerId()))
                .toList();
    }

    private List<Team> calculateTeamGroupStandings(String groupName, List<Event> groupEvents) {
        Map<Long, StatsAccumulator> statsByTeam = calculateTeamStats(groupEvents);

        Set<Long> teamIds = new HashSet<>();
        for (Event event : groupEvents) {
            if (event.getTeamHome() != null) teamIds.add(event.getTeamHome().getId());
            if (event.getTeamAway() != null) teamIds.add(event.getTeamAway().getId());
        }

        List<Team> teams = teamRepository.findAllById(teamIds);
        Map<Long, Team> teamById = teams.stream()
                .collect(Collectors.toMap(Team::getId, t -> t));

        List<TeamStatsResponseDTO> teamStats = new ArrayList<>(teams.stream()
                .map(t -> toTeamStatsDTO(t, statsByTeam))
                .toList());

        teamStats.sort(TEAM_STANDINGS_COMPARATOR);

        return teamStats.stream()
                .map(dto -> teamById.get(dto.getTeamId()))
                .filter(Objects::nonNull)
                .toList();
    }

    private String[] getBracketRoundNames(int numRounds) {
        String[] names = new String[numRounds];
        if (numRounds == 1) {
            names[0] = "Final";
        } else if (numRounds == 2) {
            names[0] = "Semifinais";
            names[1] = "Final";
        } else if (numRounds == 3) {
            names[0] = "Quartas de Final";
            names[1] = "Semifinais";
            names[2] = "Final";
        } else if (numRounds == 4) {
            names[0] = "Oitavas de Final";
            names[1] = "Quartas de Final";
            names[2] = "Semifinais";
            names[3] = "Final";
        } else {
            for (int i = 0; i < numRounds; i++) {
                if (i == numRounds - 1) {
                    names[i] = "Final";
                } else {
                    names[i] = "Rodada " + (i + 1);
                }
            }
        }
        return names;
    }

    private BigDecimal[] getBracketMultipliers(int numRounds) {
        BigDecimal[] multipliers = new BigDecimal[numRounds];
        for (int i = 0; i < numRounds; i++) {
            if (i == numRounds - 1) {
                multipliers[i] = BigDecimal.valueOf(2.0);
            } else {
                multipliers[i] = BigDecimal.valueOf(1.0 + (0.2 * i));
            }
        }
        return multipliers;
    }

    private List<List<Event>> createBracketEvents(Tournament tournament, List<TournamentRound> rounds, int numRounds, int nextPowerOf2) {
        List<List<Event>> eventsByRound = new ArrayList<>();

        for (int i = 0; i < numRounds; i++) {
            int eventsInRound = nextPowerOf2 / (int) Math.pow(2, i + 1);
            List<Event> roundEvents = new ArrayList<>();
            for (int j = 0; j < eventsInRound; j++) {
                Event event = Event.builder()
                        .tournament(tournament)
                        .round(rounds.get(i))
                        .status(EventStatus.CREATED)
                        .homeScore(0)
                        .awayScore(0)
                        .isKnockout(true)
                        .build();
                event = eventRepository.save(event);
                roundEvents.add(event);
            }
            eventsByRound.add(roundEvents);
        }

        linkBracketEvents(eventsByRound, numRounds);

        return eventsByRound;
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

        H2HRecord h2hRecord = buildH2HRecord(h2hEvents, playerHome.getId(), playerAway.getId());
        OddsResult odds = oddsCalculatorService.calculate(
                playerHome.getCurrentElo(), playerAway.getCurrentElo(), h2hRecord);
        Market market = new Market();
        market.setEvent(event);
        market.setName("Resultado Final");
        market.setStatus(MarketStatus.OPEN);
        market.setMarketType(MarketType.MATCH_RESULT);
        market = marketRepository.save(market);

        createOutcome(market, "Vitória Casa", odds.getHomeOdd());
        createOutcome(market, "Empate", odds.getDrawOdd());
        createOutcome(market, "Vitória Fora", odds.getAwayOdd());

        GroupTournament groupTournament = getCurrentGroupTournament(event.getTournament().getId());
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

    private H2HRecord buildH2HRecord(List<Event> h2hEvents, Long homePlayerId, Long awayPlayerId) {
        int homeWins = 0;
        int awayWins = 0;
        int draws = 0;

        for (Event pastEvent : h2hEvents) {
            boolean homeWasHome = pastEvent.getPlayerHome().getId().equals(homePlayerId);
            int pastHomeScore = pastEvent.getHomeScore();
            int pastAwayScore = pastEvent.getAwayScore();

            if (pastHomeScore == pastAwayScore) {
                draws++;
            } else if (homeWasHome) {
                if (pastHomeScore > pastAwayScore) {
                    homeWins++;
                } else {
                    awayWins++;
                }
            } else {
                if (pastAwayScore > pastHomeScore) {
                    homeWins++;
                } else {
                    awayWins++;
                }
            }
        }

        return new H2HRecord(homeWins, awayWins, draws);
    }

    private void createOutcome(Market market, String name, BigDecimal odd) {
        Outcome outcome = new Outcome();
        outcome.setMarket(market);
        outcome.setName(name);
        outcome.setOdd(odd);
        outcomeRepository.save(outcome);
    }

    private List<List<Event>> generateBracketAutoEvents(Tournament tournament, List<TournamentPlayer> players, int numRounds, int nextPowerOf2) {
        List<Player> sortedPlayers = players.stream()
                .map(TournamentPlayer::getPlayer)
                .sorted((p1, p2) -> p2.getCurrentElo().compareTo(p1.getCurrentElo()))
                .toList();

        Player[] positions = new Player[nextPowerOf2];
        for (int i = 0; i < sortedPlayers.size() && i < nextPowerOf2; i++) {
            positions[i] = sortedPlayers.get(i);
        }

        List<TournamentRound> standardRounds = getStandardBracketRounds(tournament);

        TournamentRound firstRound;
        if (tournament.getFormat() == TournamentFormat.LEAGUE_BRACKET) {
            firstRound = tournament.getRounds().stream()
                    .filter(r -> r.getPhaseType() == PhaseType.KNOCKOUT)
                    .min(Comparator.comparingInt(TournamentRound::getRoundOrder))
                    .orElseThrow(() -> new BusinessException("No group stage round found"));
        } else {
            firstRound = standardRounds.get(0);
        }

        List<Event> round1Events = new ArrayList<>();
        int eventsInRound1 = nextPowerOf2 / 2;
        for (int i = 0; i < eventsInRound1; i++) {
            Player home = positions[i];
            Player away = positions[nextPowerOf2 - 1 - i];
            Event event = Event.builder()
                    .tournament(tournament)
                    .round(firstRound)
                    .playerHome(home)
                    .playerAway(away)
                    .status(EventStatus.CREATED)
                    .homeScore(0)
                    .awayScore(0)
                    .isKnockout(true)
                    .build();
            event = eventRepository.save(event);
            round1Events.add(event);

            if (home != null && away != null) {
                createMarketAndOutcomesForEvent(event, home, away);
            }
        }

        List<List<Event>> eventsByRound = new ArrayList<>();
        eventsByRound.add(round1Events);
//        int knockoutRoundOffset = tournament.getFormat() == TournamentFormat.LEAGUE_BRACKET ? 1 : 0;
        for (int i = 1; i < numRounds; i++) {
            int eventsInRound = nextPowerOf2 / (int) Math.pow(2, i + 1);
            List<Event> roundEvents = new ArrayList<>();
            for (int j = 0; j < eventsInRound; j++) {
                Event event = Event.builder()
                        .tournament(tournament)
                        .round(standardRounds.get(i))
                        .status(EventStatus.CREATED)
                        .homeScore(0)
                        .awayScore(0)
                        .isKnockout(true)
                        .build();
                event = eventRepository.save(event);
                roundEvents.add(event);
            }
            eventsByRound.add(roundEvents);
        }

        linkBracketEvents(eventsByRound, numRounds);

        for (Event event : round1Events) {
            if ((event.getPlayerHome() != null && event.getPlayerAway() == null) ||
                (event.getPlayerHome() == null && event.getPlayerAway() != null)) {
                Player byeWinner = event.getPlayerHome() != null ? event.getPlayerHome() : event.getPlayerAway();
                event.setStatus(EventStatus.COMPLETED);
                event.setIsBye(true);
                if (event.getPlayerHome() != null) {
                    event.setHomeScore(1);
                    event.setAwayScore(0);
                } else {
                    event.setHomeScore(0);
                    event.setAwayScore(1);
                }
                eventRepository.save(event);
                autoAdvanceByeWinner(event, byeWinner);
            }
        }

        return eventsByRound;
    }

    private void autoAdvanceByeWinner(Event event, Player winner) {
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

            // Create market if both players are now assigned
            if (nextEvent.getPlayerHome() != null && nextEvent.getPlayerAway() != null) {
                createMarketAndOutcomesForEvent(nextEvent, nextEvent.getPlayerHome(), nextEvent.getPlayerAway());
            }
        }
    }

    private boolean isSameEvent(Event expectedSource, Event sourceEvent) {
        return expectedSource != null
                && sourceEvent != null
                && expectedSource.getId() != null
                && expectedSource.getId().equals(sourceEvent.getId());
    }

    private void assignPlayerToFirstAvailableSlot(Player player, Event event) {
        if (event.getPlayerHome() == null) {
            event.setPlayerHome(player);
        } else if (event.getPlayerAway() == null) {
            event.setPlayerAway(player);
        }
    }

    private void generateLeagueBracketAutoEvents(Tournament tournament, List<TournamentPlayer> players) {
        int n = players.size();
        Integer numberOfGroups = tournament.getNumberOfGroups();
        int playersPerGroup = n / numberOfGroups;
        int extraPlayers = n % numberOfGroups;

        // Shuffle players and distribute into groups
        List<TournamentPlayer> shuffledPlayers = new ArrayList<>(players);
        Collections.shuffle(shuffledPlayers);

        // Assign group numbers and distribute
        List<TournamentPlayer> playersToSave = new ArrayList<>();
        List<List<Player>> groups = new ArrayList<>();
        int playerIndex = 0;
        for (int g = 0; g < numberOfGroups; g++) {
            int groupSize = playersPerGroup + (g < extraPlayers ? 1 : 0);
            List<Player> groupPlayers = new ArrayList<>();
            for (int i = 0; i < groupSize && playerIndex < shuffledPlayers.size(); i++) {
                TournamentPlayer tp = shuffledPlayers.get(playerIndex++);
                tp.setGroupNumber(g + 1);
                playersToSave.add(tp);
                groupPlayers.add(tp.getPlayer());
            }
            groups.add(groupPlayers);
        }

        tournamentPlayerRepository.saveAll(playersToSave);

        // Get group-stage rounds
        List<TournamentRound> groupRounds = tournament.getRounds().stream()
                .filter(r -> r.getPhaseType() == PhaseType.GROUP_STAGE)
                .toList();

        // Generate round-robin events per group
        for (int g = 0; g < numberOfGroups; g++) {
            List<Player> groupPlayers = groups.get(g);
            int groupNumber = g + 1;
            List<TournamentRound> roundsForGroup = groupRounds.stream()
                    .filter(r -> groupNumber == r.getGroupNumber())
                    .sorted(Comparator.comparingInt(TournamentRound::getRoundOrder))
                    .toList();
            generateRoundRobinForGroup(tournament, groupPlayers, roundsForGroup, true, false);
        }
    }

    private int computeNextPowerOf2(int n) {
        if (n <= 1) return 1;
        return Integer.highestOneBit(n - 1) << 1;
    }

    private void rotateCircle(List<Player> list) {
        Player last = list.get(list.size() - 1);
        for (int i = list.size() - 1; i > 1; i--) {
            list.set(i, list.get(i - 1));
        }
        list.set(1, last);
    }

    private void linkBracketEvents(List<List<Event>> eventsByRound, int numRounds) {
        List<Event> eventsToSave = new ArrayList<>();

        for (int i = 0; i < numRounds; i++) {
            List<Event> currentRoundEvents = eventsByRound.get(i);
            List<Event> nextRoundEvents = i + 1 < numRounds ? eventsByRound.get(i + 1) : null;
            for (int j = 0; j < currentRoundEvents.size(); j++) {
                Event currentEvent = currentRoundEvents.get(j);
                if (nextRoundEvents != null) {
                    Event nextEvent = nextRoundEvents.get(j / 2);
                    currentEvent.setNextRoundEvent(nextEvent);
                    if (j % 2 == 0) {
                        nextEvent.setHomeSourceEvent(currentEvent);
                    } else {
                        nextEvent.setAwaySourceEvent(currentEvent);
                    }
                    eventsToSave.add(nextEvent);
                }
                eventsToSave.add(currentEvent);
            }
        }

        eventRepository.saveAll(eventsToSave);
    }

    private void generateRoundRobinForGroup(
            Tournament tournament, List<Player> groupPlayers,
            List<TournamentRound> rounds, boolean doubleRoundRobin, boolean isKnockout) {
        List<Player> playerList = new ArrayList<>(groupPlayers);
        boolean hasDummy = playerList.size() % 2 != 0;
        if (hasDummy) playerList.add(null);

        int n = playerList.size();
        int halfRounds = n - 1;

        for (int round = 0; round < halfRounds; round++) {
            TournamentRound currentRound = rounds.get(round);
            for (int i = 0; i < n / 2; i++) {
                Player home = playerList.get(i);
                Player away = playerList.get(n - 1 - i);
                if (home == null || away == null) continue;
                createLeagueEvent(tournament, currentRound, home, away, isKnockout);
            }
            rotateCircle(playerList);
        }

        if (!doubleRoundRobin) return;

        for (int round = 0; round < halfRounds; round++) {
            TournamentRound currentRound = rounds.get(round + halfRounds);
            for (int i = 0; i < n / 2; i++) {
                Player home = playerList.get(n - 1 - i);
                Player away = playerList.get(i);
                if (home == null || away == null) continue;
                createLeagueEvent(tournament, currentRound, home, away, isKnockout);
            }
            rotateCircle(playerList);
        }
    }

    private Event createLeagueEvent(Tournament tournament, TournamentRound round, Player home, Player away, boolean isKnockout) {
        Event event = Event.builder()
                .tournament(tournament)
                .round(round)
                .playerHome(home)
                .playerAway(away)
                .status(EventStatus.CREATED)
                .homeScore(0)
                .awayScore(0)
                .isKnockout(isKnockout)
                .build();
        event = eventRepository.save(event);
        createMarketAndOutcomesForEvent(event, home, away);
        return event;
    }

    private List<TournamentRound> getStandardBracketRounds(Tournament tournament) {
        return tournament.getRounds().stream()
                .filter(r -> r.getPhaseType() == PhaseType.KNOCKOUT && !"3rd Place".equals(r.getName()))
                .sorted(Comparator.comparingInt(TournamentRound::getRoundOrder))
                .toList();
    }

    private void createThirdPlaceEvent(Tournament tournament, List<List<Event>> eventsByRound, int numRounds) {
        if (!Boolean.TRUE.equals(tournament.getHasThirdPlaceMatch()) || numRounds < 2) return;

        TournamentRound thirdPlaceRound = tournament.getRounds().stream()
                .filter(r -> "3rd Place".equals(r.getName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("3rd Place round not found"));
        List<Event> semiFinals = eventsByRound.get(numRounds - 2);
        Event thirdPlaceEvent = Event.builder()
                .tournament(tournament)
                .round(thirdPlaceRound)
                .status(EventStatus.CREATED)
                .homeScore(0)
                .awayScore(0)
                .isKnockout(true)
                .build();
        thirdPlaceEvent.setHomeSourceEvent(semiFinals.get(0));
        thirdPlaceEvent.setAwaySourceEvent(semiFinals.get(1));
        thirdPlaceEvent.setThirdPlaceMatch(true);
        eventRepository.save(thirdPlaceEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.franciscomaath.resenhaapi.controller.dto.response.BetRankingResponseDTO> getBetRanking(Long tournamentId) {
        groupAuthorizationService.requireTournamentAccess(tournamentId);

        Long currentGroupId = currentUserContext.getRequiredGroupId();
        GroupTournament groupTournament = groupTournamentRepository.findByTournamentIdAndGroupId(tournamentId, currentGroupId)
                .orElseThrow(() -> new ResourceNotFoundException("Tournament not found in current group."));

        List<TournamentWallet> wallets = tournamentWalletRepository.findByGroupTournamentId(groupTournament.getId());

        List<com.franciscomaath.resenhaapi.controller.dto.response.BetRankingResponseDTO> ranking = new ArrayList<>();
        for (TournamentWallet wallet : wallets) {
            com.franciscomaath.resenhaapi.controller.dto.response.BetRankingResponseDTO dto = new com.franciscomaath.resenhaapi.controller.dto.response.BetRankingResponseDTO();
            dto.setUserId(wallet.getUser().getId());
            dto.setUserName(wallet.getUser().getName());
            dto.setBalance(wallet.getBalance());
            ranking.add(dto);
        }

        ranking.sort((w1, w2) -> w2.getBalance().compareTo(w1.getBalance()));

        return ranking;
    }
}
