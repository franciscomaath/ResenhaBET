package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.TournamentPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TournamentPlayerRepository extends JpaRepository<TournamentPlayer, Long> {

    boolean existsByTournamentIdAndPlayerId(Long tournamentId, Long playerId);

    List<TournamentPlayer> findByTournamentId(Long tournamentId);

    Optional<TournamentPlayer> findByTournamentIdAndPlayerId(Long tournamentId, Long playerId);

    @org.springframework.data.jpa.repository.Query("SELECT CASE WHEN COUNT(tp) > 0 THEN true ELSE false END FROM TournamentPlayer tp WHERE tp.player.id = :playerId AND tp.tournament.status = :status")
    boolean existsByPlayerIdAndTournamentStatus(@org.springframework.data.repository.query.Param("playerId") Long playerId, @org.springframework.data.repository.query.Param("status") com.franciscomaath.resenhaapi.domain.enums.TournamentStatus status);
}

