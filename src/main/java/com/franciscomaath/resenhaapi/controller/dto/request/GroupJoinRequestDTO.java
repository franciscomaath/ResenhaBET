package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupJoinRequestDTO {
    @NotBlank(message = "O código do grupo é obrigatório.")
    @Pattern(regexp = "^\\d{6}$", message = "O código do grupo deve conter exatamente 6 dígitos.")
    private String code;
}
