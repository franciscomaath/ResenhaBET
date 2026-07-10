package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
public class EventRequestDTO {

    @NotNull(message = "Tournament ID is required.")
    private Long tournamentId;

    @NotNull(message = "Round ID is required.")
    private Long roundId;

    @NotNull(message = "Home player ID is required.")
    private Long playerHomeId;

    @NotNull(message = "Away player ID is required.")
    private Long playerAwayId;

    private LocalDateTime gameDatetime;
}

