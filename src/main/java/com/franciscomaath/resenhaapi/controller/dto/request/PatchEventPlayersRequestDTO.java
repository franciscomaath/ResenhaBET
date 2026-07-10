package com.franciscomaath.resenhaapi.controller.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PatchEventPlayersRequestDTO {

    private Long playerHomeId;

    private Long playerAwayId;
}