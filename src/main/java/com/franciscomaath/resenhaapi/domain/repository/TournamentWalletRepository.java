package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.TournamentWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface TournamentWalletRepository extends JpaRepository<TournamentWallet, Long> {
    Optional<TournamentWallet> findByGroupTournamentIdAndUserId(Long groupTournamentId, Long userId);

    List<TournamentWallet> findByGroupTournamentId(Long groupTournamentId);

    @Modifying
    @Query("UPDATE TournamentWallet w SET w.balance = w.balance + :amount WHERE w.groupTournament.id = :groupTournamentId")
    int addAllBalances(@Param("groupTournamentId") Long groupTournamentId, @Param("amount") BigDecimal amount);
}
