package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
public class CompetitionRequestDTO {

    @NotBlank(message = "O nome da competicao e obrigatorio.")
    private String name;

    @NotBlank(message = "A temporada e obrigatoria.")
    private String season;

    @NotBlank(message = "O ID da liga na APIfootball e obrigatorio.")
    private String apiFootballLeagueId;

    @NotBlank(message = "O ID do pais na APIfootball e obrigatorio.")
    private String apiFootballCountryId;

    @NotBlank(message = "O ID da liga na GameForecastAPI e obrigatorio.")
    private String gameForecastLeagueId;

    @NotNull(message = "A data de inicio da competicao e obrigatoria.")
    private LocalDateTime startDate;

    @NotNull(message = "A data de fim da competicao e obrigatoria.")
    private LocalDateTime endDate;
}
