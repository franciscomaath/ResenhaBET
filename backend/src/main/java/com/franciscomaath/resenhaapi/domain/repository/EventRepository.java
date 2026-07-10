package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Optional<Event> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByPlayerHomeIdAndPlayerAwayIdAndRoundId(Long playerHomeId, Long playerAwayId, Long roundId);

    boolean existsByHomeSourceEventIdOrAwaySourceEventId(Long homeSourceEventId, Long awaySourceEventId);

	@Query("""
		SELECT COUNT(e) > 0
		FROM Event e
		WHERE e.tournament.id = :tournamentId
		  AND (e.playerHome.id = :playerId OR e.playerAway.id = :playerId)
    """)
	boolean existsPlayerInTournament(@Param("playerId") Long playerId,
	                                 @Param("tournamentId") Long tournamentId);

    Optional<Event> findByExternalMatchId(String externalMatchId);

	List<Event> findByTournamentIdAndExternalMatchIdIn(Long tournamentId, List<String> externalMatchIds);

	@Query("""
			select e
			from Event e
			where e.status = :status
				and e.isBye = false
				and (e.playerHome.id = :playerId or e.playerAway.id = :playerId)
			order by e.gameDatetime asc
			""")
	List<Event> findCompletedNonByeByPlayerId(@Param("playerId") Long playerId,
										  @Param("status") EventStatus status);

	@Query("""
			select e
			from Event e
			where e.status = :status
				and (
					(e.playerHome.id = :player1Id and e.playerAway.id = :player2Id)
					or (e.playerHome.id = :player2Id and e.playerAway.id = :player1Id)
				)
			order by e.gameDatetime desc
			""")
	List<Event> findDirectConfrontations(
			@Param("player1Id") Long player1Id,
			@Param("player2Id") Long player2Id,
			@Param("status") EventStatus status,
			Pageable pageable
	);

	@Query("""
			select e
			from Event e
			where e.status = :status
				and e.isBye = false
				and e.tournament.id = :tournamentId
				and (e.playerHome.id = :playerId or e.playerAway.id = :playerId)
			order by e.gameDatetime asc
			""")
	List<Event> findCompletedNonByeByPlayerIdAndTournamentId(
			@Param("playerId") Long playerId,
			@Param("tournamentId") Long tournamentId,
			@Param("status") EventStatus status
	);

	@Query("""
			select e
			from Event e
			where e.status = :status
				and (e.playerHome.id = :playerId or e.playerAway.id = :playerId)
			order by e.gameDatetime asc
			""")
	List<Event> findCompletedByPlayerId(
			@Param("playerId") Long playerId,
			@Param("status") EventStatus status
	);

	@Query("""
			select e
			from Event e
			where e.status = :status
				and e.tournament.id = :tournamentId
				and (e.playerHome.id = :playerId or e.playerAway.id = :playerId)
			order by e.gameDatetime asc
			""")
    List<Event> findCompletedByPlayerIdAndTournamentId(
			@Param("playerId") Long playerId,
			@Param("tournamentId") Long tournamentId,
			@Param("status") EventStatus status
	);

	@Query("""
			select e
			from Event e
			join fetch e.playerHome
			join fetch e.playerAway
			left join fetch e.round
			where e.status = :status
				and e.isBye = false
				and e.playerHome.group.id = :groupId
				and e.playerAway.group.id = :groupId
				and e.deletedAt is null
			order by e.gameDatetime asc, e.id asc
			""")
	List<Event> findCompletedNonByeByGroupId(
			@Param("groupId") Long groupId,
			@Param("status") EventStatus status
	);

	@Query("""
			select e
			from Event e
			where e.status = :status
				and e.tournament.id = :tournamentId
			order by e.gameDatetime asc
			""")
    List<Event> findCompletedByTournamentId(
			@Param("tournamentId") Long tournamentId,
			@Param("status") EventStatus status
	);

    @Query("""
			select e
			from Event e
			where e.tournament.id = :tournamentId
				and e.round.name = :roundName
			""")
	Optional<Event> findByTournamentIdAndRoundName(
			@Param("tournamentId") Long tournamentId,
			@Param("roundName") String roundName
	);

    @Query("""
			select e
			from Event e
			where e.tournament.id = :tournamentId
			""")
	List<Event> findAllByTournamentId(@Param("tournamentId") Long tournamentId);

	@Query("""
			SELECT DISTINCT e
			FROM Event e
			JOIN FETCH e.playerHome
			JOIN FETCH e.playerAway
			WHERE e.tournament.id = :tournamentId
				AND e.status = com.franciscomaath.resenhaapi.domain.enums.EventStatus.CREATED
				AND e.playerHome IS NOT NULL
				AND e.playerAway IS NOT NULL
				AND e.isBye = false
			""")
	List<Event> findFutureEventsWithPlayers(@Param("tournamentId") Long tournamentId);

	@Query("""
			select e
			from Event e
			join fetch e.tournament t
			join fetch t.competition c
			left join fetch e.teamHome
			left join fetch e.teamAway
			left join fetch e.round
			where t.type = com.franciscomaath.resenhaapi.domain.enums.TournamentType.REAL_FOOTBALL
				and e.status = com.franciscomaath.resenhaapi.domain.enums.EventStatus.CREATED
				and e.gameDatetime is not null
				and e.gameDatetime <= :now
			order by e.gameDatetime asc
			""")
	List<Event> findDueRealFootballEvents(@Param("now") LocalDateTime now);

	@Query("""
			select e
			from Event e
			join fetch e.tournament t
			join fetch t.competition c
			left join fetch e.teamHome
			left join fetch e.teamAway
			left join fetch e.round
			where t.type = com.franciscomaath.resenhaapi.domain.enums.TournamentType.REAL_FOOTBALL
				and e.status = com.franciscomaath.resenhaapi.domain.enums.EventStatus.IN_PROGRESS
				and e.externalMatchId is not null
			order by e.gameDatetime asc
			""")
    List<Event> findInProgressRealFootballEvents();

    @Query("""
			SELECT AVG(CASE
				WHEN e.playerHome.id = :playerId THEN e.homeScore
				ELSE e.awayScore
			END)
			FROM Event e
			WHERE (e.playerHome.id = :playerId OR e.playerAway.id = :playerId)
			  AND e.status = com.franciscomaath.resenhaapi.domain.enums.EventStatus.COMPLETED
			  AND e.isBye = false
			""")
    Double findAvgGoalsScoredByPlayer(@Param("playerId") Long playerId);

    @Query("""
			SELECT AVG(CASE
				WHEN e.playerHome.id = :playerId THEN e.awayScore
				ELSE e.homeScore
			END)
			FROM Event e
			WHERE (e.playerHome.id = :playerId OR e.playerAway.id = :playerId)
			  AND e.status = com.franciscomaath.resenhaapi.domain.enums.EventStatus.COMPLETED
			  AND e.isBye = false
			""")
    Double findAvgGoalsConcededByPlayer(@Param("playerId") Long playerId);

    @Query("""
			SELECT COUNT(e)
			FROM Event e
			WHERE (e.playerHome.id = :playerId OR e.playerAway.id = :playerId)
			  AND e.status = com.franciscomaath.resenhaapi.domain.enums.EventStatus.COMPLETED
			  AND e.isBye = false
			""")
    Long countCompletedMatchesByPlayer(@Param("playerId") Long playerId);

    @Query("""
			SELECT AVG((e.homeScore + e.awayScore) / 2.0)
			FROM Event e
			WHERE e.status = com.franciscomaath.resenhaapi.domain.enums.EventStatus.COMPLETED
			  AND e.isBye = false
			""")
    Double findGlobalAvgGoalsPerSide();
}
