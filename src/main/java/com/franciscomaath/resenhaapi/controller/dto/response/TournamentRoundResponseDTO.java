package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TournamentRoundResponseDTO {

    private String name;

    private Long roundId;

    private BigDecimal multiplier;

    private Integer roundOrder;

    private String phaseType;

    private Integer groupNumber;
}

