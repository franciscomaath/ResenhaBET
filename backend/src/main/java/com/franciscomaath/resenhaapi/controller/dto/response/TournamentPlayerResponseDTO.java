package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TournamentPlayerResponseDTO {

    private Long tournamentPlayerId;

    private Long tournamentId;

    private Long playerId;

    private String playerName;

    private Long teamId;

    private String teamName;

    private Integer groupNumber;
}

