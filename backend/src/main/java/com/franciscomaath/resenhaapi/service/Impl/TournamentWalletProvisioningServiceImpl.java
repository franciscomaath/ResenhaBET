package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.domain.entity.GroupMember;
import com.franciscomaath.resenhaapi.domain.entity.GroupTournament;
import com.franciscomaath.resenhaapi.domain.entity.TournamentWallet;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.repository.GroupMemberRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupTournamentRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentWalletRepository;
import com.franciscomaath.resenhaapi.service.TournamentWalletProvisioningService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TournamentWalletProvisioningServiceImpl implements TournamentWalletProvisioningService {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupTournamentRepository groupTournamentRepository;
    private final TournamentWalletRepository tournamentWalletRepository;

    @Override
    @Transactional
    public void provisionForGroupTournament(GroupTournament groupTournament) {
        groupMemberRepository.findByGroupId(groupTournament.getGroup().getId()).forEach(member ->
                ensureWallet(groupTournament, member.getUser()));
    }

    @Override
    @Transactional
    public void provisionForMember(User user, Long groupId) {
        groupTournamentRepository.findByGroupId(groupId).forEach(groupTournament ->
                ensureWallet(groupTournament, user));
    }

    private void ensureWallet(GroupTournament groupTournament, User user) {
        boolean exists = tournamentWalletRepository.findByGroupTournamentIdAndUserId(
                groupTournament.getId(), user.getId()).isPresent();
        if (exists) {
            return;
        }

        TournamentWallet wallet = new TournamentWallet();
        wallet.setGroupTournament(groupTournament);
        wallet.setUser(user);
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setInitialBalance(BigDecimal.ZERO);
        tournamentWalletRepository.save(wallet);
    }
}
