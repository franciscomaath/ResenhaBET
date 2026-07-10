package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.*;
import com.franciscomaath.resenhaapi.controller.dto.response.*;
import com.franciscomaath.resenhaapi.domain.entity.*;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.enums.GroupRole;
import com.franciscomaath.resenhaapi.domain.enums.TournamentStatus;
import com.franciscomaath.resenhaapi.domain.exception.BusinessException;
import com.franciscomaath.resenhaapi.domain.exception.DuplicateResourceException;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import com.franciscomaath.resenhaapi.domain.repository.*;
import com.franciscomaath.resenhaapi.mapper.GroupMapper;
import com.franciscomaath.resenhaapi.mapper.PlayerMapper;
import com.franciscomaath.resenhaapi.service.EloService;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.service.GroupService;
import com.franciscomaath.resenhaapi.service.TournamentWalletProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final CurrentUserContext currentUserContext;
    private final GroupAuthorizationService groupAuthorizationService;
    private final TournamentWalletProvisioningService tournamentWalletProvisioningService;
    private final PlayerRepository playerRepository;
    private final EloService eloService;
    private final GroupMapper groupMapper;
    private final PlayerMapper playerMapper;
    private final TournamentRepository tournamentRepository;
    private final com.franciscomaath.resenhaapi.service.TournamentService tournamentService;
    private final com.franciscomaath.resenhaapi.domain.repository.EventRepository eventRepository;

    @Override
    @Transactional
    public GroupResponseDTO create(GroupRequestDTO dto) {
        User user = currentUserContext.getRequiredUser();
        String name = dto.getName() == null ? null : dto.getName().trim();
        if (name == null || name.isBlank()) {
            throw new BusinessException("O nome do grupo e obrigatorio.");
        }
        if (groupRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Ja existe um grupo com esse nome.");
        }

        String groupCode = generateGroupCode();
        Group group = groupRepository.save(Group.builder().name(name).active(true).groupCode(groupCode).build());

        Player player = Player.builder()
                .name(user.getName())
                .active(true)
                .group(group)
                .user(user)
                .build();
        playerRepository.save(player);

        GroupMember member = groupMemberRepository.save(GroupMember.builder()
                .group(group)
                .user(user)
                .role(GroupRole.OWNER)
                .build());
        setSessionGroup(group);
        return groupMapper.toResponse(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponseDTO> listMine() {
        User user = currentUserContext.getRequiredUser();
        return groupMemberRepository.findByUserIdOrderByGroupNameAsc(user.getId()).stream()
                .map(groupMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponseDTO> listMembers(Long groupId) {
        requireGroupMembership(groupId);
        return groupMemberRepository.findByGroupId(groupId).stream()
                .map(groupMapper::toMemberResponse)
                .toList();
    }

    @Override
    @Transactional
    public GroupResponseDTO addMember(Long groupId, GroupMemberRequestDTO dto) {
        requireGroupManagement(groupId);
        if (dto.getRole() == GroupRole.OWNER) {
            groupAuthorizationService.requireCurrentGroupOwner();
        }

        Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
        User user = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com id: " + dto.getUserId()));

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, dto.getUserId())) {
            throw new DuplicateResourceException("Usuario ja pertence ao grupo.");
        }

        GroupMember member = groupMemberRepository.save(GroupMember.builder()
                .group(group)
                .user(user)
                .role(dto.getRole())
                .build());
        tournamentWalletProvisioningService.provisionForMember(user, groupId);
        return groupMapper.toResponse(member);
    }

    @Override
    @Transactional
    public GroupMemberResponseDTO claimPlayer(Long groupId, GroupClaimPlayerRequestDTO dto){
        User user = currentUserContext.getRequiredUser();

        GroupMember existingMember = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario nao pertence ao grupo informado."));

        if(existingMember.isPlayerClaimed()){
            throw new BusinessException("Usuário já reinvindicou um jogador neste grupo.");
        }

        if(dto.getPlayerId() != null){
            Player player = playerRepository.findById(dto.getPlayerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Player", "id", dto.getPlayerId()));

            if(!player.getGroup().getId().equals(groupId)) {
                throw new BusinessException("O jogador informado não pertence a esse grupo.");
            }

            if(player.getUser() != null){
                throw new BusinessException("O jogador informado já está vinculado a um usuário.");
            }

            player.setUser(user);
            playerRepository.save(player);
        }

        existingMember.setPlayerClaimed(true);
        groupMemberRepository.save(existingMember);
        return groupMapper.toMemberResponse(existingMember);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerResponseDTO> listAvaliablePlayers(Long groupId) {
        List<Player> availablePlayers = playerRepository.findByGroupIdAndUserIsNull(groupId);

        return availablePlayers.stream()
                .map(playerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public GroupResponseDTO switchGroup(Long groupId) {
        User user = currentUserContext.getRequiredUser();
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario nao pertence ao grupo informado."));
        setSessionGroup(member.getGroup());
        return groupMapper.toResponse(member);
    }

    @Override
    @Transactional
    public List<PlayerResponseDTO> recalculateElo(Long groupId) {
        requireGroupManagement(groupId);
        return eloService.recalculateGroupElos(groupId).stream()
                .map(playerMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public GroupResponseDTO editGroup(Long groupId, GroupPatchRequestDTO dto) {
        requireGroupManagement(groupId);
        Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
        group.setName(dto.getName());
        groupRepository.save(group);
        
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserContext.getRequiredUser().getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario nao pertence ao grupo informado."));
        return groupMapper.toResponse(member);
    }

    @Override
    @Transactional
    public void softDeleteGroup(Long groupId) {
        requireGroupManagement(groupId);
        groupAuthorizationService.requireCurrentGroupOwner();
        
        if (tournamentRepository.existsByGroupIdAndStatus(groupId, TournamentStatus.IN_PROGRESS)) {
            throw new BusinessException("Cannot delete group with IN_PROGRESS tournaments.");
        }
        
        Group group = groupRepository.findByIdAndDeletedAtIsNull(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group", "id", groupId));
                
        group.setDeletedAt(LocalDateTime.now());
        groupRepository.save(group);
    }

    @Override
    @Transactional
    public void removeGroupMember(Long groupId, Long userId) {
        requireGroupManagement(groupId);
        GroupMember targetMember = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in group"));
        
        GroupMember currentUserMember = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserContext.getRequiredUser().getId())
                .orElseThrow(() -> new UnauthorizedException("Usuario nao pertence ao grupo informado."));
        
        if (currentUserMember.getRole() == GroupRole.MEMBER) {
             throw new UnauthorizedException("Apenas admins podem remover membros");
        }
        if (currentUserMember.getRole() == GroupRole.ADMIN && targetMember.getRole() != GroupRole.MEMBER) {
             throw new UnauthorizedException("Admins so podem remover members");
        }
        
        if (targetMember.getRole() == GroupRole.OWNER) {
             if (currentUserMember.getRole() != GroupRole.OWNER) {
                  throw new UnauthorizedException("Apenas owners podem remover owners");
             }
             long ownersCount = groupMemberRepository.findByGroupId(groupId).stream()
                   .filter(m -> m.getRole() == GroupRole.OWNER && m.getDeletedAt() == null)
                   .count();
             if (ownersCount <= 1) {
                  throw new BusinessException("Cannot remove the last OWNER of the group");
             }
        }
        
        targetMember.setDeletedAt(LocalDateTime.now());
        groupMemberRepository.save(targetMember);
    }

    @Override
    @Transactional
    public GroupMemberResponseDTO changeMemberRole(Long groupId, Long userId, GroupMemberRolePatchRequestDTO dto) {
        requireGroupManagement(groupId);
        groupAuthorizationService.requireCurrentGroupOwner();
        
        GroupMember targetMember = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in group"));
                
        if (targetMember.getDeletedAt() != null) {
            throw new BusinessException("Cannot change role of deleted member");
        }
        
        if (targetMember.getRole() == GroupRole.OWNER && dto.getRole() != GroupRole.OWNER) {
             long ownersCount = groupMemberRepository.findByGroupId(groupId).stream()
                   .filter(m -> m.getRole() == GroupRole.OWNER && m.getDeletedAt() == null)
                   .count();
             if (ownersCount <= 1) {
                  throw new BusinessException("Cannot demote the last OWNER of the group");
             }
        }
        
        targetMember.setRole(dto.getRole());
        groupMemberRepository.save(targetMember);
        return groupMapper.toMemberResponse(targetMember);
    }

    private void requireGroupManagement(Long groupId) {
        Long currentGroupId = currentUserContext.getRequiredGroupId();
        if (!currentGroupId.equals(groupId)) {
            throw new UnauthorizedException("Alterne para o grupo antes de gerencia-lo.");
        }
        groupAuthorizationService.requireCurrentGroupAdmin();
    }

    private void requireGroupMembership(Long groupId) {
        Long currentGroupId = currentUserContext.getRequiredGroupId();
        if (!currentGroupId.equals(groupId)) {
            throw new UnauthorizedException("Alterne para o grupo antes de consulta-lo.");
        }
        groupAuthorizationService.requireCurrentGroupMember();
    }

    private void setSessionGroup(Group group) {
        Session session = sessionRepository.findByToken(currentUserContext.getRequiredToken())
                .orElseThrow(() -> new UnauthorizedException("Sessao nao encontrada."));
        session.setCurrentGroup(group);
        sessionRepository.save(session);
        currentUserContext.set(session.getUser(), group, session.getToken());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerStatsResponseDTO> getRanking(Long groupId) {
        requireGroupMembership(groupId);

        // Fetch all completed events for this group to aggregate goals, matches, points
        List<Event> completedEvents = eventRepository.findCompletedNonByeByGroupId(groupId, EventStatus.COMPLETED);

        // Fetch all players for this group
        List<Player> players = playerRepository.findByGroupIdAndDeletedAtIsNullOrderByNameAsc(groupId);
        Map<Long, Player> playerMap = players.stream()
                .collect(java.util.stream.Collectors.toMap(Player::getId, p -> p));

        Map<Long, PlayerStatsResponseDTO> statsMap = new HashMap<>();
        for (Player player : players) {
            PlayerStatsResponseDTO dto = new PlayerStatsResponseDTO();
            dto.setPlayerId(player.getId());
            dto.setPlayerName(player.getName());
            dto.setCurrentElo(player.getCurrentElo());
            statsMap.put(player.getId(), dto);
        }

        for (Event event : completedEvents) {
            if (event.getHomeScore() == null || event.getAwayScore() == null) continue;
            
            Long homeId = event.getPlayerHome().getId();
            Long awayId = event.getPlayerAway().getId();

            PlayerStatsResponseDTO homeStats = statsMap.get(homeId);
            PlayerStatsResponseDTO awayStats = statsMap.get(awayId);

            if (homeStats != null) {
                homeStats.setMatchesPlayed(homeStats.getMatchesPlayed() + 1);
                homeStats.setGoalsScored(homeStats.getGoalsScored() + event.getHomeScore());
                homeStats.setGoalsConceded(homeStats.getGoalsConceded() + event.getAwayScore());
            }

            if (awayStats != null) {
                awayStats.setMatchesPlayed(awayStats.getMatchesPlayed() + 1);
                awayStats.setGoalsScored(awayStats.getGoalsScored() + event.getAwayScore());
                awayStats.setGoalsConceded(awayStats.getGoalsConceded() + event.getHomeScore());
            }

            if (event.getHomeScore() > event.getAwayScore()) {
                if (homeStats != null) { homeStats.setWins(homeStats.getWins() + 1); homeStats.setPoints(homeStats.getPoints() + 3); }
                if (awayStats != null) awayStats.setLosses(awayStats.getLosses() + 1);
            } else if (event.getHomeScore() < event.getAwayScore()) {
                if (homeStats != null) homeStats.setLosses(homeStats.getLosses() + 1);
                if (awayStats != null) { awayStats.setWins(awayStats.getWins() + 1); awayStats.setPoints(awayStats.getPoints() + 3); }
            } else {
                if(event.getPenaltiesHome() != null && event.getPenaltiesAway() != null) {
                    if(event.getPenaltiesHome() > event.getPenaltiesAway()){
                        if (homeStats != null) { homeStats.setWins(homeStats.getWins() + 1); homeStats.setPoints(homeStats.getPoints() + 3); }
                        if (awayStats != null) awayStats.setLosses(awayStats.getLosses() + 1);
                    } else if(event.getPenaltiesHome() < event.getPenaltiesAway()) {
                        if (homeStats != null) homeStats.setLosses(homeStats.getLosses() + 1);
                        if (awayStats != null) { awayStats.setWins(awayStats.getWins() + 1); awayStats.setPoints(awayStats.getPoints() + 3);
                        }
                    }
                } else {
                    if (homeStats != null) { homeStats.setDraws(homeStats.getDraws() + 1); homeStats.setPoints(homeStats.getPoints() + 1); }
                    if (awayStats != null) { awayStats.setDraws(awayStats.getDraws() + 1); awayStats.setPoints(awayStats.getPoints() + 1); }
                }
            }
        }

        // Calculate goal difference
        for (PlayerStatsResponseDTO dto : statsMap.values()) {
            dto.setGoalDifference(dto.getGoalsScored() - dto.getGoalsConceded());
        }

        // Calculate tournaments won
        List<Tournament> tournaments = tournamentRepository.findAllByGroupId(groupId, Pageable.unpaged()).getContent();
        for (Tournament tournament : tournaments) {
            if (tournament.getStatus() == TournamentStatus.IN_PROGRESS || tournament.getStatus() == TournamentStatus.COMPLETED) {
                try {
                    TournamentScoreboardResponseDTO scoreboard = tournamentService.getScoreboard(tournament.getId());
                    Long winnerId = null;
                    if (scoreboard.getPlacements() != null && !scoreboard.getPlacements().isEmpty()) {
                        BracketPlacementDTO firstPlace = scoreboard.getPlacements().stream()
                                .filter(p -> p.getPosition() == 1)
                                .findFirst().orElse(null);
                        if (firstPlace != null) {
                            winnerId = firstPlace.getPlayerId();
                        }
                    } else if (scoreboard.getEntries() != null && !scoreboard.getEntries().isEmpty()) {
                        winnerId = scoreboard.getEntries().get(0).getPlayerId();
                    }
                    
                    if (winnerId != null) {
                        PlayerStatsResponseDTO winnerStats = statsMap.get(winnerId);
                        if (winnerStats != null) {
                            winnerStats.setTournamentsWon(winnerStats.getTournamentsWon() + 1);
                        }
                    }
                } catch (Exception e) {
                    // Ignore exceptions for scoreboard calculation in ranking (e.g., tournament without any matches yet)
                }
            }
        }

        List<PlayerStatsResponseDTO> ranking = new ArrayList<>(statsMap.values());
        ranking.sort(Comparator.comparing(PlayerStatsResponseDTO::getCurrentElo).reversed());
        return ranking;
    }

    @Override
    @Transactional
    public GroupResponseDTO join(GroupJoinRequestDTO dto) {
        Group group = groupRepository.findByGroupCode(dto.getCode())
                .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        User user = currentUserContext.getRequiredUser();

        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), user.getId())) {
            throw new DuplicateResourceException("User is already a member of this group");
        }

        GroupMember member = groupMemberRepository.save(GroupMember.builder()
                .group(group)
                .user(user)
                .role(GroupRole.MEMBER)
                .playerClaimed(false)
                .build());

        tournamentWalletProvisioningService.provisionForMember(user, group.getId());

        return groupMapper.toResponse(member);
    }

    private String generateGroupCode() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        int code = 100000 + random.nextInt(900000);
        return String.valueOf(code);
    }
}
