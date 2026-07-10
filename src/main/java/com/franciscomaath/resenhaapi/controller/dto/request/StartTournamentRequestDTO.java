package com.franciscomaath.resenhaapi.controller.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class StartTournamentRequestDTO {

    private Integer numberOfGroups;

    private Integer playersAdvancingPerGroup;

}
