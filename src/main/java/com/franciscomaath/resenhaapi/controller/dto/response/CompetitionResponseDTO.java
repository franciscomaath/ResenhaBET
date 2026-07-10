package com.franciscomaath.resenhaapi.controller.dto.response;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class CompetitionResponseDTO {

    private Long id;

    private UUID uuid;

    private String name;

    private String season;

    private String apiFootballLeagueId;

    private String apiFootballCountryId;

    private String gameForecastLeagueId;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Boolean active;

    private LocalDateTime createdAt;
}
