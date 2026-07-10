package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.LinkUserRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.PlayerRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.PlayerUpdateRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerStatsResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.PlayerActiveRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerInviteResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.enums.TournamentStatus;
import com.franciscomaath.resenhaapi.domain.exception.BusinessException;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.PlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentPlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.UserRepository;
import com.franciscomaath.resenhaapi.mapper.PlayerMapper;
import com.franciscomaath.resenhaapi.service.PlayerService;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerServiceImpl implements PlayerService {

    private final PlayerRepository playerRepository;
    private final PlayerMapper playerMapper;
    private final UserRepository userRepository;
    private final CurrentUserContext currentUserContext;
    private final EventRepository eventRepository;
    private final GroupAuthorizationService groupAuthorizationService;
    private final TournamentPlayerRepository tournamentPlayerRepository;

    @Override
    @Transactional
    public PlayerResponseDTO create(PlayerRequestDTO dto){
        groupAuthorizationService.requireCurrentGroupAdmin();

        Player player = Player.builder()
                .name(dto.getName())
                .active(true)
                .group(currentUserContext.getRequiredGroup())
                .build();

        player = playerRepository.save(player);
        return playerMapper.toResponse(player);
    }

    @Override
    public List<PlayerResponseDTO> findAll(){
        List<Player> players = playerRepository.findByGroupIdAndDeletedAtIsNullOrderByNameAsc(currentUserContext.getRequiredGroupId());
        return players.stream()
                .map(playerMapper::toResponse)
                .toList();
    }

    @Override
    public PlayerResponseDTO findPlayerById(Long id){
        Player player = playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(id, currentUserContext.getRequiredGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Player", "id", id));

        return playerMapper.toResponse(player);
    }

    @Override
    @Transactional
    public PlayerResponseDTO update(Long id, PlayerUpdateRequestDTO dto){
        groupAuthorizationService.requireCurrentGroupAdmin();

        Player player = playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(id, currentUserContext.getRequiredGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Player", "id", id));

        player.setName(dto.getName());
        player.setActive(dto.isActive());

        player = playerRepository.save(player);
        return playerMapper.toResponse(player);
    }

    @Override
    @Transactional
    public PlayerResponseDTO linkUser(Long id, LinkUserRequestDTO dto) {
        groupAuthorizationService.requireCurrentGroupAdmin();

        Player player = playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(id, currentUserContext.getRequiredGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Player", "id", id));

        if (player.getUser() != null) {
            throw new BusinessException("Player ja possui usuario vinculado.");
        }

        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com id: " + dto.getUserId()));

        if (playerRepository.existsByGroupIdAndUserId(currentUserContext.getRequiredGroupId(), user.getId())) {
            throw new BusinessException("Usuario ja esta vinculado a outro player.");
        }

        player.setUser(user);
        player = playerRepository.save(player);
        return playerMapper.toResponse(player);
    }

    @Override
    public PlayerStatsResponseDTO getPlayerStats(Long playerId, Long tournamentId) {
        Player player = playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(playerId, currentUserContext.getRequiredGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Player", "id", playerId));

        if (tournamentId != null) {
            groupAuthorizationService.requireTournamentAccess(tournamentId);
        }

        List<Event> events;
        if (tournamentId != null) {
            events = eventRepository.findCompletedByPlayerIdAndTournamentId(
                    playerId, tournamentId, EventStatus.COMPLETED);
        } else {
            events = eventRepository.findCompletedByPlayerId(
                    playerId, EventStatus.COMPLETED);
        }

        int matchesPlayed = 0;
        int wins = 0;
        int losses = 0;
        int draws = 0;
        int goalsScored = 0;
        int goalsConceded = 0;

        for (Event event : events) {
            if (event.getHomeScore() == null || event.getAwayScore() == null) {
                continue;
            }

            matchesPlayed++;
            boolean isHome = playerId.equals(event.getPlayerHome().getId());
            int playerScore = isHome ? event.getHomeScore() : event.getAwayScore();
            int opponentScore = isHome ? event.getAwayScore() : event.getHomeScore();

            goalsScored += playerScore;
            goalsConceded += opponentScore;

            if (playerScore > opponentScore) {
                wins++;
            } else if (playerScore < opponentScore) {
                losses++;
            } else {
                draws++;
            }
        }

        PlayerStatsResponseDTO stats = new PlayerStatsResponseDTO();
        stats.setPlayerId(player.getId());
        stats.setPlayerName(player.getName());
        stats.setMatchesPlayed(matchesPlayed);
        stats.setWins(wins);
        stats.setLosses(losses);
        stats.setDraws(draws);
        stats.setGoalsScored(goalsScored);
        stats.setGoalsConceded(goalsConceded);
        stats.setGoalDifference(goalsScored - goalsConceded);
        stats.setPoints(wins * 3 + draws);
        stats.setCurrentElo(player.getCurrentElo());

        return stats;
    }

    @Override
    @Transactional
    public PlayerResponseDTO changeActiveStatus(Long id, PlayerActiveRequestDTO dto) {
        groupAuthorizationService.requireCurrentGroupAdmin();
        Long groupId = currentUserContext.getRequiredGroupId();
        Player player = playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(id, groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found in this group"));
        
        player.setActive(dto.getActive());
        playerRepository.save(player);
        return playerMapper.toResponse(player);
    }

    @Override
    @Transactional
    public void softDeletePlayer(Long id) {
        groupAuthorizationService.requireCurrentGroupAdmin();
        Long groupId = currentUserContext.getRequiredGroupId();
        Player player = playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(id, groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found in this group"));
        
        if (tournamentPlayerRepository.existsByPlayerIdAndTournamentStatus(id, TournamentStatus.IN_PROGRESS)) {
            throw new BusinessException("Cannot delete player participating in an IN_PROGRESS tournament");
        }
        
        player.setDeletedAt(LocalDateTime.now());
        playerRepository.save(player);
    }

    @Override
    @Transactional
    public PlayerInviteResponseDTO generateInvite(Long id) {
        groupAuthorizationService.requireCurrentGroupAdmin();
        Long groupId = currentUserContext.getRequiredGroupId();
        Player player = playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(id, groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Player not found in this group"));
        // TODO: this method should not exist anymore, but an similar logic will be used to generate an invite to the group.
        // TODO: Remember to remove this method later.
        if (player.getUser() != null) {
            throw new BusinessException("Player is already linked to a user");
        }

        String token = java.util.UUID.randomUUID().toString();
        String inviteUrl = "https://resenhabet.com/invite/" + token;

        return PlayerInviteResponseDTO.builder()
                .inviteToken(token)
                .inviteUrl(inviteUrl)
                .build();
    }
}
