package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class TournamentPlayersResponseDTO {

    private Integer playerCount;

    List<TournamentPlayerResponseDTO> players;

}
