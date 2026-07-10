package com.franciscomaath.resenhaapi.controller.dto.request;

import com.franciscomaath.resenhaapi.domain.enums.GenerationMode;
import com.franciscomaath.resenhaapi.domain.enums.TournamentFormat;
import lombok.Data;

@Data
public class TournamentPatchRequestDTO {
    private String name;
    private TournamentFormat format;
    private GenerationMode generationMode;
    private Boolean hasThirdPlaceMatch;
    private Integer numberOfGroups;
    private Integer playersAdvancingPerGroup;
}
