package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.domain.entity.GroupTournament;
import com.franciscomaath.resenhaapi.domain.entity.User;

public interface TournamentWalletProvisioningService {
    void provisionForGroupTournament(GroupTournament groupTournament);

    void provisionForMember(User user, Long groupId);
}
