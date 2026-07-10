package com.franciscomaath.resenhaapi.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TournamentScoreboardResponseDTO {

    private Long tournamentId;

    private String tournamentName;

    private String format;

    // LEAGUE + LEAGUE_BRACKET group stage
    private List<PlayerStatsResponseDTO> entries;

    // LEAGUE_BRACKET
    private List<GroupStandingsDTO> groups;

    // BRACKET + LEAGUE_BRACKET knockout
    private List<BracketPlacementDTO> placements;

}
