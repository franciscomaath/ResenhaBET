package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserPatchRequestDTO {
    @NotBlank(message = "O nome é obrigatório.")
    private String name;
}
