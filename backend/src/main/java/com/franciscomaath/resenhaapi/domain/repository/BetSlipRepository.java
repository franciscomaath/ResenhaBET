package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.BetSlip;
import com.franciscomaath.resenhaapi.domain.enums.BetSlipStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BetSlipRepository extends JpaRepository<BetSlip, Long> {

    List<BetSlip> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByGroupTournamentId(Long groupTournamentId);

    List<BetSlip> findByGroupTournamentId(Long groupTournamentId);

    List<BetSlip> findByUserIdAndGroupTournamentGroupIdOrderByCreatedAtDesc(Long userId, Long groupId);

    @Query("SELECT b FROM BetSlip b JOIN b.items i WHERE i.event.id = :eventId AND b.groupTournament.group.id = :groupId")
    List<BetSlip> findByEventIdAndGroupId(@Param("eventId") Long eventId, @Param("groupId") Long groupId);

    @Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM BetSlip b JOIN b.items i WHERE i.event.id = :eventId")
    boolean existsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT b FROM BetSlip b JOIN b.items i WHERE i.event.id = :eventId AND b.status = :status")
    List<BetSlip> findByEventIdAndStatus(@Param("eventId") Long eventId, @Param("status") BetSlipStatus status);

    @Query("SELECT b FROM BetSlip b JOIN b.items i WHERE i.event.id IN :eventIds AND b.user.id = :userId AND b.groupTournament.id = :groupTournamentId AND b.status = :status")
    List<BetSlip> findByUserAndGroupTournamentAndEventIdsAndStatus(
            @Param("userId") Long userId,
            @Param("groupTournamentId") Long groupTournamentId,
            @Param("eventIds") List<Long> eventIds,
            @Param("status") BetSlipStatus status);

    @Query("SELECT DISTINCT b FROM BetSlip b JOIN b.items i WHERE i.event.id IN :eventIds AND b.status = :status")
    List<BetSlip> findByEventIdsAndStatus(@Param("eventIds") List<Long> eventIds, @Param("status") BetSlipStatus status);
}
