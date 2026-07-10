package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@RequiredArgsConstructor
public class WalletDepositRequestDTO {

    @NotNull(message = "O ID do usuario e obrigatorio.")
    private Long userId;

    @NotNull(message = "O ID do torneio e obrigatorio.")
    private Long tournamentId;

    @NotNull(message = "O valor do deposito e obrigatorio.")
    @DecimalMin(value = "0.01", message = "O valor do deposito deve ser maior que zero.")
    @Digits(integer = 19, fraction = 2, message = "O valor do deposito deve ter no maximo 2 casas decimais.")
    private BigDecimal amount;
}

