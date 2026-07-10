package com.franciscomaath.resenhaapi.controller.dto.response;

import com.franciscomaath.resenhaapi.domain.enums.UserType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class UserResponseDTO {

    private Long id;

    private String name;

    private String username;

    private UserType userType;

    private boolean firstLogin;

    private boolean hasPin;

    private LocalDateTime createdAt;
}
