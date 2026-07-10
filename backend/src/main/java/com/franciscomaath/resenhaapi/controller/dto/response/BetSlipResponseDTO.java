package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class BetSlipResponseDTO {

    private Long id;
    private Long userId;
    private Long tournamentId;
    private Long groupTournamentId;
    private BigDecimal stake;
    private BigDecimal combinedOdd;
    private BigDecimal potentialReturn;
    private String status;
    private LocalDateTime createdAt;
    private List<BetSlipItemResponseDTO> items;
}
