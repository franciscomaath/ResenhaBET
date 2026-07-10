package com.franciscomaath.resenhaapi.controller.dto.request;

import com.franciscomaath.resenhaapi.domain.enums.GenerationMode;
import com.franciscomaath.resenhaapi.domain.enums.MarketType;
import com.franciscomaath.resenhaapi.domain.enums.TournamentFormat;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@RequiredArgsConstructor
public class TournamentRequestDTO {

    @NotNull(message = "O nome do torneio e obrigatorio.")
    private String name;

    @NotNull(message = "O formato do torneio e obrigatorio.")
    private TournamentFormat format;

    private TournamentType type;

    private Long competitionId;

    private Set<MarketType> marketTypes;

    private GenerationMode generationMode;

    private Boolean hasThirdPlaceMatch;

    private LocalDateTime startDate;

    private LocalDateTime endDate;
}

