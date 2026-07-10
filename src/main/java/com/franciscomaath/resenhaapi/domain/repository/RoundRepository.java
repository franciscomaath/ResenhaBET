package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.TournamentRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoundRepository extends JpaRepository<TournamentRound, Long> {
	List<TournamentRound> findByTournamentIdOrderByRoundOrderAsc(Long tournamentId);

	java.util.Optional<TournamentRound> findByTournamentIdAndName(Long tournamentId, String name);
}
