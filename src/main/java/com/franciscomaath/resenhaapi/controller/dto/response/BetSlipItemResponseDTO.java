package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BetSlipItemResponseDTO {

    private Long id;
    private Long eventId;
    private Long marketId;
    private Long outcomeId;
    private String outcomeName;
    private BigDecimal oddSnapshot;
    private String status;
    private EventResponseDTO event;
}
