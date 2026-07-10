package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.request.UserLoginRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.UserUpdatePinRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.UserLoginResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.UserResponseDTO;

import java.util.UUID;

public interface AuthService {

    UserLoginResponseDTO login(UserLoginRequestDTO dto);

    UserResponseDTO me();

    void logout(UUID token);

    void updatePin(UserUpdatePinRequestDTO dto);
}
