package com.franciscomaath.resenhaapi.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupStandingsDTO {

    private Integer groupNumber;

    private String groupName;

    private List<PlayerStatsResponseDTO> standings;

    private List<TeamStatsResponseDTO> teamStandings;

}
