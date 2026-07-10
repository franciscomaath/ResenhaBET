package com.franciscomaath.resenhaapi.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public class UserLoginResponseDTO {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long id;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String name;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private UserType userType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long currentGroupId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String currentGroupName;

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean firstLogin;

    private boolean hasPin;

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private boolean pinRequired;
}
