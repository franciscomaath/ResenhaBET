package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LinkUserRequestDTO {

    @NotNull(message = "O id do usuario e obrigatorio.")
    private Long userId;
}
