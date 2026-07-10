package com.franciscomaath.resenhaapi.domain.repository;

import com.franciscomaath.resenhaapi.domain.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

     @Modifying
     @Query("UPDATE Wallet w SET w.balance = w.balance + :amount")
     int addAllBalances(@Param("amount") BigDecimal amount);

     Optional<Wallet> findByUserId(Long userId);
}
