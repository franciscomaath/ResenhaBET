package com.franciscomaath.resenhaapi.controller.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EventPatchRequestDTO {
    private Long roundId;
    private Long playerHomeId;
    private Long playerAwayId;
    private Long teamHomeId;
    private Long teamAwayId;
    private LocalDateTime gameDatetime;
    private Boolean isKnockout;
    private Boolean isThirdPlaceMatch;
}
