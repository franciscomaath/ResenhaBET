package com.franciscomaath.resenhaapi.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.franciscomaath.resenhaapi.service.dto.ForecastEventDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GameForecastResponse {
    private List<ForecastEventDto> data;
}
