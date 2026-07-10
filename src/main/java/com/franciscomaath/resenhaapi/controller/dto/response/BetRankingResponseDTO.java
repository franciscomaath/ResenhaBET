package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BetRankingResponseDTO {
    private Long userId;
    private String userName;
    private BigDecimal balance;
}
