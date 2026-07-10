package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BracketPlacementDTO {

    private Long playerId;

    private String playerName;

    private Integer position;

    private String eliminationRound;

}
