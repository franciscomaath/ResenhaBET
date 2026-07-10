package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.domain.entity.GroupMember;
import com.franciscomaath.resenhaapi.domain.enums.GroupRole;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import com.franciscomaath.resenhaapi.domain.repository.GroupMemberRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupTournamentRepository;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GroupAuthorizationServiceImpl implements GroupAuthorizationService {

    private final CurrentUserContext currentUserContext;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupTournamentRepository groupTournamentRepository;

    @Override
    public void requireCurrentGroupAdmin() {
        GroupMember member = currentMember();
        if (member.getRole() != GroupRole.OWNER && member.getRole() != GroupRole.ADMIN) {
            throw new UnauthorizedException("Acesso restrito ao administrador do grupo.");
        }
    }

    @Override
    public void requireCurrentGroupOwner() {
        GroupMember member = currentMember();
        if (member.getRole() != GroupRole.OWNER) {
            throw new UnauthorizedException("Acesso restrito ao dono do grupo.");
        }
    }

    @Override
    public void requireCurrentGroupMember() {
        currentMember();
    }

    @Override
    public void requireTournamentAccess(Long tournamentId) {
        Long groupId = currentUserContext.getRequiredGroupId();
        if (!groupTournamentRepository.existsByTournamentIdAndGroupIdAndDeletedAtIsNull(tournamentId, groupId)) {
            throw new UnauthorizedException("Torneio nao pertence ao grupo ativo.");
        }
    }

    @Override
    public void requireTournamentAdmin(Long tournamentId) {
        requireTournamentAccess(tournamentId);
        requireCurrentGroupAdmin();
    }

    private GroupMember currentMember() {
        Long userId = currentUserContext.getRequiredUser().getId();
        Long groupId = currentUserContext.getRequiredGroupId();
        return groupMemberRepository.findByGroupIdAndUserIdAndDeletedAtIsNull(groupId, userId)
                .orElseThrow(() -> new UnauthorizedException("Usuario nao pertence ao grupo ativo."));
    }
}
