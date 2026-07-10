package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.domain.entity.Group;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.enums.GroupRole;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import com.franciscomaath.resenhaapi.domain.repository.GroupMemberRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupTournamentRepository;
import com.franciscomaath.resenhaapi.testsupport.MultiGroupFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupAuthorizationServiceImplTest {

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupTournamentRepository groupTournamentRepository;

    @InjectMocks
    private GroupAuthorizationServiceImpl service;

    @Test
    void requireCurrentGroupAdmin_allowsOwnerAndAdmin() {
        User user = MultiGroupFixtures.user(1L, "User", UserType.USER);
        Group group = MultiGroupFixtures.group(10L, "Group");
        when(currentUserContext.getRequiredUser()).thenReturn(user);
        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(groupMemberRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(10L, 1L))
                .thenReturn(Optional.of(MultiGroupFixtures.groupMember(group, user, GroupRole.OWNER)))
                .thenReturn(Optional.of(MultiGroupFixtures.groupMember(group, user, GroupRole.ADMIN)));

        assertDoesNotThrow(() -> service.requireCurrentGroupAdmin());
        assertDoesNotThrow(() -> service.requireCurrentGroupAdmin());
    }

    @Test
    void requireCurrentGroupAdmin_rejectsMember() {
        User user = MultiGroupFixtures.user(1L, "User", UserType.USER);
        Group group = MultiGroupFixtures.group(10L, "Group");
        when(currentUserContext.getRequiredUser()).thenReturn(user);
        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(groupMemberRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(10L, 1L))
                .thenReturn(Optional.of(MultiGroupFixtures.groupMember(group, user, GroupRole.MEMBER)));

        assertThrows(UnauthorizedException.class, () -> service.requireCurrentGroupAdmin());
    }

    @Test
    void requireTournamentAccess_requiresGroupTournament() {
        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(groupTournamentRepository.existsByTournamentIdAndGroupIdAndDeletedAtIsNull(99L, 10L)).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> service.requireTournamentAccess(99L));
    }

    @Test
    void requireTournamentAdmin_requiresBothAccessAndAdminRole() {
        User user = MultiGroupFixtures.user(1L, "User", UserType.USER);
        Group group = MultiGroupFixtures.group(10L, "Group");
        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(currentUserContext.getRequiredUser()).thenReturn(user);
        when(groupTournamentRepository.existsByTournamentIdAndGroupIdAndDeletedAtIsNull(99L, 10L)).thenReturn(true);
        when(groupMemberRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(10L, 1L))
                .thenReturn(Optional.of(MultiGroupFixtures.groupMember(group, user, GroupRole.ADMIN)));

        assertDoesNotThrow(() -> service.requireTournamentAdmin(99L));
    }
}
