package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.request.UserRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.UserPatchRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.UserResponseDTO;

import java.util.List;

public interface UserService {

    UserResponseDTO create(UserRequestDTO user);

    List<UserResponseDTO> findAll();

    void resetPin(Long id);

    UserResponseDTO getUserById(Long id);

    UserResponseDTO editUser(Long id, UserPatchRequestDTO dto);
}
