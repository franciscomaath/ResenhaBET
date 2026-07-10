package com.franciscomaath.resenhaapi.controller.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class EventUpdateRequestDTO {

    private Integer homeScore;

    private Integer awayScore;
}
