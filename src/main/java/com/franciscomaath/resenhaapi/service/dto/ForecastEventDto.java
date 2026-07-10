package com.franciscomaath.resenhaapi.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastEventDto {
    private String id;

    @JsonProperty("start_at")
    private String startAt;

    @JsonProperty("team_home")
    private TeamInfo teamHome;

    @JsonProperty("team_away")
    private TeamInfo teamAway;

    private Prediction[] predictions;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TeamInfo {
        private String name;
        private String id;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Prediction {
        @JsonProperty("match_result")
        private MatchResult matchResult;

        @JsonProperty("total_goals")
        private TotalGoals totalGoals;

        @JsonProperty("both_teams_score")
        private BothTeamsScore bothTeamsScore;

        @JsonProperty("exact_score")
        private Map<String, Integer> exactScore;

    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchResult {
        private Integer home;
        private Integer draw;
        private Integer away;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TotalGoals {
        @JsonProperty("over_2_5")
        private Integer over25;

        @JsonProperty("under_2_5")
        private Integer under25;

        @JsonProperty("over_3_5")
        private Integer over35;

        @JsonProperty("under_3_5")
        private Integer under35;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BothTeamsScore {
        private Integer yes;
        private Integer no;
    }
}
