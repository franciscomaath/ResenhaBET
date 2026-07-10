package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.Tournament;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    Optional<Tournament> findByCompetitionIdAndType(Long competitionId, TournamentType type);

    Optional<Tournament> findByIdAndDeletedAtIsNull(Long id);

    @Query("select t from GroupTournament gt join gt.tournament t where gt.group.id = :groupId and t.deletedAt is null and gt.deletedAt is null")
    Page<Tournament> findAllByGroupId(@Param("groupId") Long groupId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM GroupTournament gt JOIN gt.tournament t WHERE gt.group.id = :groupId AND t.status = :status AND t.deletedAt IS NULL AND gt.deletedAt IS NULL")
    boolean existsByGroupIdAndStatus(@Param("groupId") Long groupId, @Param("status") com.franciscomaath.resenhaapi.domain.enums.TournamentStatus status);
}

