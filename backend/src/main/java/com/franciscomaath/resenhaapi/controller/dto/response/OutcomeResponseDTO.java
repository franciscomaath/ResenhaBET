package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OutcomeResponseDTO {
    private Long id;
    private String name;
    private BigDecimal odd;
}
