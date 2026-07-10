package com.franciscomaath.resenhaapi.controller.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserRequestDTO {

    private String name;

    private String username;

    public String resolvedName() {
        return name != null && !name.isBlank() ? name : username;
    }
}
