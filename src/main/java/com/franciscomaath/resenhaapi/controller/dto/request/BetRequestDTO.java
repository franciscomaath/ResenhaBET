package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
public class BetRequestDTO {

    @NotNull(message = "O ID do torneio e obrigatorio.")
    private Long tournamentId;

    @NotNull(message = "O valor da aposta e obrigatorio.")
    @DecimalMin(value = "0.01", message = "O valor minimo da aposta e 0.01.")
    @Digits(integer = 19, fraction = 2, message = "O valor da aposta deve ter no maximo 2 casas decimais.")
    private BigDecimal stake;

    @NotEmpty(message = "A aposta deve conter pelo menos uma selecao.")
    @Valid
    private List<BetItemRequest> items;
}
