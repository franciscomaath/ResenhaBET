package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.request.WalletDepositRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.WalletResponseDTO;

import java.math.BigDecimal;

public interface WalletService {

    WalletResponseDTO me(Long userId, Long tournamentId);

    BigDecimal deposit(WalletDepositRequestDTO dto);

    void depositAll(Long tournamentId, BigDecimal amount);
}
