package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TournamentGroupConfigResponseDTO {

    private Integer playerCount;

    private List<TournamentGroupOptionDTO> validOptions;

    @Getter
    @Setter
    public static class TournamentGroupOptionDTO {
        private Integer groupCount;
        private Integer playersPerGroup;
        private Integer remainder;
    }
}