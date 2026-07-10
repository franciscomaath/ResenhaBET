package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.service.dto.ForecastEventDto;

import java.util.List;

public interface GameForecastClient {
    List<ForecastEventDto> fetchPredictions(String leagueId, int pageSize);
}
