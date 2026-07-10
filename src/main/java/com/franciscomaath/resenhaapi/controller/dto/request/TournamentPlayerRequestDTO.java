package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TournamentPlayerRequestDTO {

    @NotNull(message = "Player ID cannot be null")
    private Long playerId;

}
