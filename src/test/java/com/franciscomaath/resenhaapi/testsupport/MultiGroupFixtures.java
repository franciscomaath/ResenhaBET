package com.franciscomaath.resenhaapi.testsupport;

import com.franciscomaath.resenhaapi.domain.entity.Group;
import com.franciscomaath.resenhaapi.domain.entity.GroupMember;
import com.franciscomaath.resenhaapi.domain.entity.GroupTournament;
import com.franciscomaath.resenhaapi.domain.entity.Session;
import com.franciscomaath.resenhaapi.domain.entity.Tournament;
import com.franciscomaath.resenhaapi.domain.entity.TournamentWallet;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.enums.GroupRole;
import com.franciscomaath.resenhaapi.domain.enums.TournamentStatus;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import com.franciscomaath.resenhaapi.domain.enums.UserType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public final class MultiGroupFixtures {

    private MultiGroupFixtures() {
    }

    public static User user(Long id, String name, UserType type) {
        return User.builder()
                .id(id)
                .name(name)
                .userType(type)
                .firstLogin(false)
                .build();
    }

    public static Group group(Long id, String name) {
        return Group.builder()
                .id(id)
                .name(name)
                .active(true)
                .build();
    }

    public static GroupMember groupMember(Group group, User user, GroupRole role) {
        return GroupMember.builder()
                .group(group)
                .user(user)
                .role(role)
                .build();
    }

    public static Tournament tournament(Long id, TournamentType type) {
        return Tournament.builder()
                .id(id)
                .name("Tournament " + id)
                .uuid(UUID.randomUUID())
                .type(type)
                .status(TournamentStatus.CREATED)
                .build();
    }

    public static GroupTournament groupTournament(Long id, Group group, Tournament tournament) {
        return GroupTournament.builder()
                .id(id)
                .group(group)
                .tournament(tournament)
                .build();
    }

    public static TournamentWallet tournamentWallet(Long id, GroupTournament groupTournament, User user, BigDecimal balance) {
        TournamentWallet wallet = new TournamentWallet();
        wallet.setId(id);
        wallet.setGroupTournament(groupTournament);
        wallet.setUser(user);
        wallet.setBalance(balance);
        wallet.setInitialBalance(BigDecimal.ZERO);
        return wallet;
    }

    public static Session session(User user, Group group, UUID token) {
        Session session = new Session();
        session.setUser(user);
        session.setCurrentGroup(group);
        session.setToken(token);
        session.setExpiresAt(LocalDateTime.now().plusHours(1));
        return session;
    }
}
