package com.franciscomaath.resenhaapi.controller.dto.request;

import com.franciscomaath.resenhaapi.domain.enums.GroupRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupMemberRequestDTO {

    @NotNull(message = "O ID do usuario e obrigatorio.")
    private Long userId;

    @NotNull(message = "A role do membro e obrigatoria.")
    private GroupRole role;
}
