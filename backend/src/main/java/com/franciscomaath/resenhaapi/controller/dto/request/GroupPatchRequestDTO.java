package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GroupPatchRequestDTO {
    @NotBlank(message = "O nome do grupo e obrigatorio.")
    private String name;
}
