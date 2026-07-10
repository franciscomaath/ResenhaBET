package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EventResponseDTO {

    private Long id;

    private Long tournamentId;

    private String tournamentType;

    private Long roundId;

    private String playerHomeName;

    private Long playerHomeId;

    private String playerAwayName;

    private Long playerAwayId;

    private Long teamHomeId;

    private String teamHomeName;

    private Long teamAwayId;

    private String teamAwayName;

    private String externalMatchId;

    private LocalDateTime gameDatetime;

    private String status;

    private Integer homeScore;

    private Integer awayScore;

    private Boolean isKnockout;

    private Boolean isBye;

    private Integer penaltiesHome;

    private Integer penaltiesAway;

    private Long nextRoundEventId;

    private Long homeSourceEventId;

    private Long awaySourceEventId;

    private boolean isThirdPlaceMatch;
}

