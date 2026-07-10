package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.Transaction;
import com.franciscomaath.resenhaapi.domain.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    boolean existsByTournamentWalletGroupTournamentIdAndTypeNot(Long groupTournamentId, TransactionType type);

    List<Transaction> findByTournamentWalletGroupTournamentId(Long groupTournamentId);
}

