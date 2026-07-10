package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
public class WalletResponseDTO {

    private Long userId;

    private Long tournamentId;

    private Long groupTournamentId;

    private BigDecimal balance;

    private BigDecimal initialBalance;
}
