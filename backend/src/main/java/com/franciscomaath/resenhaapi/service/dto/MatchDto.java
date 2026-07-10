package com.franciscomaath.resenhaapi.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MatchDto {
    @JsonProperty("match_id")
    private String matchId;

    @JsonProperty("league_id")
    private String leagueId;

    @JsonProperty("league_name")
    private String leagueName;

    @JsonProperty("match_date")
    private String matchDate;

    @JsonProperty("match_time")
    private String matchTime;

    @JsonProperty("match_status")
    private String matchStatus;

    @JsonProperty("match_live")
    private String matchLive;

    @JsonProperty("match_hometeam_id")
    private String homeTeamId;

    @JsonProperty("match_hometeam_name")
    private String homeTeamName;

    @JsonProperty("match_hometeam_score")
    private String homeScore;

    @JsonProperty("match_awayteam_id")
    private String awayTeamId;

    @JsonProperty("match_awayteam_name")
    private String awayTeamName;

    @JsonProperty("match_awayteam_score")
    private String awayScore;

    @JsonProperty("match_hometeam_halftime_score")
    private String halftimeHomeScore;

    @JsonProperty("match_awayteam_halftime_score")
    private String halftimeAwayScore;

    @JsonProperty("match_hometeam_extra_score")
    private String extraTimeHomeScore;

    @JsonProperty("match_awayteam_extra_score")
    private String extraTimeAwayScore;

    @JsonProperty("match_hometeam_penalty_score")
    private String penaltyHomeScore;

    @JsonProperty("match_awayteam_penalty_score")
    private String penaltyAwayScore;

    @JsonProperty("team_home_badge")
    private String teamHomeBadge;

    @JsonProperty("team_away_badge")
    private String teamAwayBadge;

    @JsonProperty("match_round")
    private String matchRound;

    @JsonProperty("stage_name")
    private String stageName;
}
