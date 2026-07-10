package com.franciscomaath.resenhaapi.service.dto;

public class H2HRecord {
    private final int homeWins;
    private final int awayWins;
    private final int draws;

    public H2HRecord(int homeWins, int awayWins, int draws) {
        this.homeWins = homeWins;
        this.awayWins = awayWins;
        this.draws = draws;
    }

    public int getHomeWins() {
        return homeWins;
    }

    public int getAwayWins() {
        return awayWins;
    }

    public int getDraws() {
        return draws;
    }

    public int getTotalMatches() {
        return homeWins + awayWins + draws;
    }
}

