package com.franciscomaath.resenhaapi.service;

public interface GroupAuthorizationService {
    void requireCurrentGroupAdmin();

    void requireCurrentGroupOwner();

    void requireCurrentGroupMember();

    void requireTournamentAccess(Long tournamentId);

    void requireTournamentAdmin(Long tournamentId);
}
