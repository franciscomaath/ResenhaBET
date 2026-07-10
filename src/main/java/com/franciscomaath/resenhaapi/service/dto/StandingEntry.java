package com.franciscomaath.resenhaapi.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class StandingEntry {

    @JsonProperty("team_id")
    private String teamId;

    @JsonProperty("league_round")
    private String leagueRound;
}
