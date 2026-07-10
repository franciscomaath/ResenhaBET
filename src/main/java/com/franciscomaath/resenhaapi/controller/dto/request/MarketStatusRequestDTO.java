package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class MarketStatusRequestDTO {

    @NotBlank(message = "O status e obrigatorio (OPEN ou CLOSED).")
    private String status;
}
