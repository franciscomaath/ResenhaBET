package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.GroupTournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface GroupTournamentRepository extends JpaRepository<GroupTournament, Long> {
    boolean existsByTournamentId(Long tournamentId);

    boolean existsByTournamentIdAndGroupId(Long tournamentId, Long groupId);

    Optional<GroupTournament> findByTournamentIdAndGroupId(Long tournamentId, Long groupId);

    List<GroupTournament> findByGroupId(Long groupId);

    Optional<GroupTournament> findByTournamentIdAndGroupIdAndDeletedAtIsNull(Long tournamentId, Long groupId);

    boolean existsByTournamentIdAndGroupIdAndDeletedAtIsNull(Long tournamentId, Long groupId);
}
