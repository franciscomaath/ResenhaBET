package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.service.dto.MatchDto;
import com.franciscomaath.resenhaapi.service.dto.StandingEntry;

import java.time.LocalDate;
import java.util.List;

public interface ApiFootballClient {
    List<MatchDto> fetchEventsByLeague(String leagueId, String countryId, LocalDate from, LocalDate to);
    List<MatchDto> fetchLiveEvents(String leagueId, String countryId);
    List<MatchDto> fetchEventsByMatchId(String matchId);
    List<StandingEntry> getStandings(String leagueId);
}
