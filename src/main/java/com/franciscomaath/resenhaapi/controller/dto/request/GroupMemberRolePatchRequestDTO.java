package com.franciscomaath.resenhaapi.controller.dto.request;

import com.franciscomaath.resenhaapi.domain.enums.GroupRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GroupMemberRolePatchRequestDTO {
    @NotNull(message = "Role is required.")
    private GroupRole role;
}
