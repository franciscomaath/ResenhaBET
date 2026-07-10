package com.franciscomaath.resenhaapi.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlayerActiveRequestDTO {
    @NotNull(message = "active is required")
    private Boolean active;
}
