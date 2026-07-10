package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
public class CompletedEventRequestDTO {

    @NotNull(message = "Tournament ID is required.")
    private Long tournamentId;

    @NotNull(message = "Round ID is required.")
    private Long roundId;

    private Long playerHomeId;

    private Long playerAwayId;

    @NotNull(message = "Home score is required.")
    private Integer homeScore;

    @NotNull(message = "Away score is required.")
    private Integer awayScore;

    private LocalDateTime gameDatetime;

    private Boolean isBye;
}
