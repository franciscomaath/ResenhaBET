package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdatePinRequestDTO {

    @Pattern(regexp = "\\d{4}", message = "O PIN deve conter exatamente 4 digitos numericos.")
    private String pin;
}
