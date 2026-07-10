package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.GroupMemberRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.GroupRequestDTO;
import com.franciscomaath.resenhaapi.domain.entity.Group;
import com.franciscomaath.resenhaapi.domain.entity.GroupMember;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.entity.Session;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.enums.GroupRole;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import com.franciscomaath.resenhaapi.domain.repository.GroupMemberRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupRepository;
import com.franciscomaath.resenhaapi.domain.repository.SessionRepository;
import com.franciscomaath.resenhaapi.domain.repository.UserRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentRepository;
import com.franciscomaath.resenhaapi.domain.repository.PlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.service.TournamentService;
import com.franciscomaath.resenhaapi.mapper.GroupMapper;
import com.franciscomaath.resenhaapi.mapper.PlayerMapper;
import com.franciscomaath.resenhaapi.service.EloService;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.service.TournamentWalletProvisioningService;
import com.franciscomaath.resenhaapi.testsupport.MultiGroupFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupServiceImplTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    @Mock
    private TournamentWalletProvisioningService tournamentWalletProvisioningService;

    @Mock
    private EloService eloService;

    @Mock
    private GroupMapper groupMapper;

    @Mock
    private PlayerMapper playerMapper;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TournamentService tournamentService;

    @InjectMocks
    private GroupServiceImpl groupService;

    @Test
    void create_shouldCreateGroupOwnerMembershipAndSwitchSession() {
        User user = MultiGroupFixtures.user(1L, "Owner", UserType.USER);
        Group savedGroup = MultiGroupFixtures.group(10L, "Copa Gege");
        UUID token = UUID.randomUUID();
        Session session = MultiGroupFixtures.session(user, null, token);
        GroupRequestDTO request = new GroupRequestDTO();
        request.setName(" Copa Gege ");

        when(currentUserContext.getRequiredUser()).thenReturn(user);
        when(currentUserContext.getRequiredToken()).thenReturn(token);
        when(groupRepository.existsByNameIgnoreCase("Copa Gege")).thenReturn(false);
        when(groupRepository.save(any(Group.class))).thenReturn(savedGroup);
        when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.findByToken(token)).thenReturn(Optional.of(session));
        when(groupMapper.toResponse(any(GroupMember.class))).thenAnswer(invocation -> groupResponse(invocation.getArgument(0)));

        var response = groupService.create(request);

        assertEquals(10L, response.getId());
        assertEquals(GroupRole.OWNER, response.getRole());
        ArgumentCaptor<GroupMember> memberCaptor = ArgumentCaptor.forClass(GroupMember.class);
        verify(groupMemberRepository).save(memberCaptor.capture());
        assertEquals(GroupRole.OWNER, memberCaptor.getValue().getRole());
        assertEquals(savedGroup, session.getCurrentGroup());
        verify(currentUserContext).set(user, savedGroup, token);
    }

    @Test
    void addMember_asAdmin_shouldCreateMembershipAndProvisionWallets() {
        Group group = MultiGroupFixtures.group(10L, "Group");
        User user = MultiGroupFixtures.user(2L, "Member", UserType.USER);
        GroupMemberRequestDTO request = memberRequest(2L, GroupRole.MEMBER);

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(groupRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(false);
        when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(groupMapper.toResponse(any(GroupMember.class))).thenAnswer(invocation -> groupResponse(invocation.getArgument(0)));

        var response = groupService.addMember(10L, request);

        assertEquals(GroupRole.MEMBER, response.getRole());
        verify(groupAuthorizationService).requireCurrentGroupAdmin();
        verify(tournamentWalletProvisioningService).provisionForMember(user, 10L);
    }

    @Test
    void addMember_ownerRoleRequiresOwner() {
        Group group = MultiGroupFixtures.group(10L, "Group");
        User user = MultiGroupFixtures.user(2L, "Member", UserType.USER);
        GroupMemberRequestDTO request = memberRequest(2L, GroupRole.OWNER);

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(groupRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(group));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(false);
        when(groupMemberRepository.save(any(GroupMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(groupMapper.toResponse(any(GroupMember.class))).thenAnswer(invocation -> groupResponse(invocation.getArgument(0)));

        groupService.addMember(10L, request);

        verify(groupAuthorizationService).requireCurrentGroupOwner();
    }

    @Test
    void listMembers_requiresActiveGroupMembershipAndReturnsMembers() {
        Group group = MultiGroupFixtures.group(10L, "Group");
        User owner = MultiGroupFixtures.user(1L, "Owner", UserType.USER);
        User member = MultiGroupFixtures.user(2L, "Member", UserType.USER);
        GroupMember ownerMember = GroupMember.builder()
                .group(group)
                .user(owner)
                .role(GroupRole.OWNER)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
        GroupMember regularMember = GroupMember.builder()
                .group(group)
                .user(member)
                .role(GroupRole.MEMBER)
                .createdAt(LocalDateTime.of(2026, 1, 2, 10, 0))
                .build();

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(groupMemberRepository.findByGroupId(10L)).thenReturn(List.of(ownerMember, regularMember));
        when(groupMapper.toMemberResponse(any(GroupMember.class))).thenAnswer(invocation -> memberResponse(invocation.getArgument(0)));

        var response = groupService.listMembers(10L);

        assertEquals(2, response.size());
        assertEquals(1L, response.get(0).getUserId());
        assertEquals("Owner", response.get(0).getUserName());
        assertEquals(GroupRole.OWNER, response.get(0).getRole());
        assertEquals(2L, response.get(1).getUserId());
        verify(groupAuthorizationService).requireCurrentGroupMember();
    }

    @Test
    void listMembers_requiresRequestedGroupToBeActiveGroup() {
        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);

        assertThrows(UnauthorizedException.class, () -> groupService.listMembers(11L));
    }

    @Test
    void switchGroup_requiresMembership() {
        User user = MultiGroupFixtures.user(1L, "User", UserType.USER);
        when(currentUserContext.getRequiredUser()).thenReturn(user);
        when(groupMemberRepository.findByGroupIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class, () -> groupService.switchGroup(10L));
    }

    @Test
    void recalculateElo_requiresAdminAndReturnsUpdatedPlayers() {
        Group group = MultiGroupFixtures.group(10L, "Group");
        Player playerA = Player.builder().id(1L).name("A").currentElo(java.math.BigDecimal.valueOf(1000)).group(group).build();
        Player playerB = Player.builder().id(2L).name("B").currentElo(java.math.BigDecimal.valueOf(1000)).group(group).build();

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(eloService.recalculateGroupElos(10L)).thenReturn(List.of(playerA, playerB));
        when(playerMapper.toResponse(any(Player.class))).thenAnswer(invocation -> {
            Player player = invocation.getArgument(0);
            com.franciscomaath.resenhaapi.controller.dto.response.PlayerResponseDTO dto = new com.franciscomaath.resenhaapi.controller.dto.response.PlayerResponseDTO();
            dto.setId(player.getId());
            dto.setName(player.getName());
            return dto;
        });

        var response = groupService.recalculateElo(10L);

        assertEquals(2, response.size());
        assertEquals(1L, response.get(0).getId());
        verify(groupAuthorizationService).requireCurrentGroupAdmin();
        verify(eloService).recalculateGroupElos(10L);
    }

    private GroupMemberRequestDTO memberRequest(Long userId, GroupRole role) {
        GroupMemberRequestDTO request = new GroupMemberRequestDTO();
        request.setUserId(userId);
        request.setRole(role);
        return request;
    }

    private com.franciscomaath.resenhaapi.controller.dto.response.GroupResponseDTO groupResponse(GroupMember member) {
        com.franciscomaath.resenhaapi.controller.dto.response.GroupResponseDTO dto = new com.franciscomaath.resenhaapi.controller.dto.response.GroupResponseDTO();
        dto.setId(member.getGroup().getId());
        dto.setName(member.getGroup().getName());
        dto.setRole(member.getRole());
        dto.setPlayerClaimed(member.isPlayerClaimed());
        return dto;
    }

    private com.franciscomaath.resenhaapi.controller.dto.response.GroupMemberResponseDTO memberResponse(GroupMember member) {
        com.franciscomaath.resenhaapi.controller.dto.response.GroupMemberResponseDTO dto = new com.franciscomaath.resenhaapi.controller.dto.response.GroupMemberResponseDTO();
        dto.setUserId(member.getUser().getId());
        dto.setUserName(member.getUser().getName());
        dto.setRole(member.getRole());
        dto.setPlayerClaimed(member.isPlayerClaimed());
        dto.setCreatedAt(member.getCreatedAt());
        return dto;
    }
}
