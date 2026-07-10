package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class TournamentResponseDTO {

    private Long id;

    private Long groupTournamentId;

    private UUID uuid;

    private String name;

    private String type;

    private String format;

    private String status;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private String generationMode;

    private Boolean hasThirdPlaceMatch;

    private Integer numberOfGroups;

    private Integer playersAdvancingPerGroup;

    private Long competitionId;

    private String competitionName;

    private Set<String> marketTypes;

    private List<TournamentRoundResponseDTO> rounds;
}

