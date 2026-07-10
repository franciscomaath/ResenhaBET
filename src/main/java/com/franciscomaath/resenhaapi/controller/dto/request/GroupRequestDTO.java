package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupRequestDTO {

    @NotBlank(message = "O nome do grupo e obrigatorio.")
    private String name;
}
