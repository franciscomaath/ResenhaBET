package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.domain.entity.Group;
import com.franciscomaath.resenhaapi.domain.entity.GroupTournament;
import com.franciscomaath.resenhaapi.domain.entity.TournamentWallet;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.enums.GroupRole;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import com.franciscomaath.resenhaapi.domain.repository.GroupMemberRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupTournamentRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentWalletRepository;
import com.franciscomaath.resenhaapi.testsupport.MultiGroupFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentWalletProvisioningServiceImplTest {

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @Mock
    private GroupTournamentRepository groupTournamentRepository;

    @Mock
    private TournamentWalletRepository tournamentWalletRepository;

    @InjectMocks
    private TournamentWalletProvisioningServiceImpl service;

    @Test
    void provisionForGroupTournament_createsWalletForEveryGroupMember() {
        Group group = MultiGroupFixtures.group(10L, "Group");
        User user1 = MultiGroupFixtures.user(1L, "User1", UserType.USER);
        User user2 = MultiGroupFixtures.user(2L, "User2", UserType.USER);
        GroupTournament groupTournament = groupTournament(group, 100L);

        when(groupMemberRepository.findByGroupId(10L)).thenReturn(List.of(
                MultiGroupFixtures.groupMember(group, user1, GroupRole.MEMBER),
                MultiGroupFixtures.groupMember(group, user2, GroupRole.MEMBER)
        ));
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(100L, 1L)).thenReturn(Optional.empty());
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(100L, 2L)).thenReturn(Optional.empty());

        service.provisionForGroupTournament(groupTournament);

        ArgumentCaptor<TournamentWallet> captor = ArgumentCaptor.forClass(TournamentWallet.class);
        verify(tournamentWalletRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertEquals(BigDecimal.ZERO, captor.getAllValues().get(0).getBalance());
        assertEquals(user1, captor.getAllValues().get(0).getUser());
        assertEquals(user2, captor.getAllValues().get(1).getUser());
    }

    @Test
    void provisionForGroupTournament_isIdempotent() {
        Group group = MultiGroupFixtures.group(10L, "Group");
        User user = MultiGroupFixtures.user(1L, "User", UserType.USER);
        GroupTournament groupTournament = groupTournament(group, 100L);

        when(groupMemberRepository.findByGroupId(10L)).thenReturn(List.of(MultiGroupFixtures.groupMember(group, user, GroupRole.MEMBER)));
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(MultiGroupFixtures.tournamentWallet(1L, groupTournament, user, BigDecimal.ZERO)));

        service.provisionForGroupTournament(groupTournament);

        verify(tournamentWalletRepository, never()).save(any());
    }

    @Test
    void provisionForMember_createsWalletForEveryGroupTournament() {
        Group group = MultiGroupFixtures.group(10L, "Group");
        User user = MultiGroupFixtures.user(1L, "User", UserType.USER);
        GroupTournament gt1 = groupTournament(group, 100L);
        GroupTournament gt2 = groupTournament(group, 101L);

        when(groupTournamentRepository.findByGroupId(10L)).thenReturn(List.of(gt1, gt2));
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(100L, 1L)).thenReturn(Optional.empty());
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(101L, 1L)).thenReturn(Optional.empty());

        service.provisionForMember(user, 10L);

        verify(tournamentWalletRepository, org.mockito.Mockito.times(2)).save(any(TournamentWallet.class));
    }

    @Test
    void provisionForMember_isIdempotent() {
        Group group = MultiGroupFixtures.group(10L, "Group");
        User user = MultiGroupFixtures.user(1L, "User", UserType.USER);
        GroupTournament gt = groupTournament(group, 100L);

        when(groupTournamentRepository.findByGroupId(10L)).thenReturn(List.of(gt));
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(MultiGroupFixtures.tournamentWallet(1L, gt, user, BigDecimal.ZERO)));

        service.provisionForMember(user, 10L);

        verify(tournamentWalletRepository, never()).save(any());
    }

    private GroupTournament groupTournament(Group group, Long id) {
        return MultiGroupFixtures.groupTournament(id, group, MultiGroupFixtures.tournament(id + 1000, TournamentType.FIFA_MATCH));
    }
}
