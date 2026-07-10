package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class BetItemRequest {

    @NotNull(message = "O ID do evento é obrigatorio.")
    private Long eventId;

    @NotNull(message = "O ID do market é obrigatorio.")
    private Long marketId;

    @NotNull(message = "O ID do outcome é obrigatorio.")
    private Long outcomeId;
}
