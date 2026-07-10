package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateGameForecastTeamIdRequestDTO {

    @NotBlank
    private String gameForecastTeamId;
}
