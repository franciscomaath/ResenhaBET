package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MarketResponseDTO {
    private Long id;
    private Long eventId;
    private String name;
    private String status;
    private String marketType;
    private List<OutcomeResponseDTO> outcomes;
}
