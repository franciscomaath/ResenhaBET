package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class TeamRequestDTO {

    @NotBlank(message = "O nome do time e obrigatorio.")
    private String name;

    @NotBlank(message = "A abreviacao do time e obrigatoria.")
    @Size(max = 4, message = "A abreviacao deve ter no maximo 4 caracteres.")
    private String abbreviation;

    private String badgeUrl;

}


