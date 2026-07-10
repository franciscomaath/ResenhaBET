package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamStatsResponseDTO {

    private Long teamId;

    private String teamName;

    private int matchesPlayed;

    private int wins;

    private int losses;

    private int draws;

    private int goalsScored;

    private int goalsConceded;

    private int goalDifference;

    private int points;

}
