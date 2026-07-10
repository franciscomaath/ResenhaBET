package com.franciscomaath.resenhaapi.controller.dto.request;

import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FinishEventRequestDTO {

    private Integer penaltiesHome;

    private Integer penaltiesAway;

    private EventStatus status;
}