package com.franciscomaath.resenhaapi.controller.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerResponseDTO {

    private Long id;

    private String name;

    private boolean active;

    private Long userId;

}
