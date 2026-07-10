package com.franciscomaath.resenhaapi.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.franciscomaath.resenhaapi.domain.enums.GroupRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupResponseDTO {
    private Long id;
    private String name;
    private GroupRole role;
    private boolean playerClaimed;
    private String groupCode;
}
