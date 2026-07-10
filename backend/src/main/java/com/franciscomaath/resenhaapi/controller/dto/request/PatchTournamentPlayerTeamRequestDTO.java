package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatchTournamentPlayerTeamRequestDTO {

    @NotNull(message = "Team ID cannot be null")
    private Long teamId;
}

