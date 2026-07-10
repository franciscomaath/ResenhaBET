package com.franciscomaath.resenhaapi.controller.dto.response;

import com.franciscomaath.resenhaapi.domain.enums.GroupRole;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class GroupMemberResponseDTO {
    private Long userId;
    private String userName;
    private GroupRole role;
    private boolean playerClaimed;
    private LocalDateTime createdAt;
}
