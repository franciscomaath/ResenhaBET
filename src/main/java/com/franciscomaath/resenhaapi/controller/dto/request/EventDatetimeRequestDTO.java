package com.franciscomaath.resenhaapi.controller.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class EventDatetimeRequestDTO {
    @NotNull(message = "gameDatetime is required")
    private LocalDateTime gameDatetime;
}
