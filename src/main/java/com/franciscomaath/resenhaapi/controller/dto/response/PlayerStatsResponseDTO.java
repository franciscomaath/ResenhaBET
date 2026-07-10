package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PlayerStatsResponseDTO {

    private Long playerId;

    private String playerName;

    private int matchesPlayed;

    private int wins;

    private int losses;

    private int draws;

    private int goalsScored;

    private int goalsConceded;

    private int goalDifference;

    private int points;

    private BigDecimal currentElo;

    private int tournamentsWon;
}
