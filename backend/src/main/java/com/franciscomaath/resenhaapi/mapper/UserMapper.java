package com.franciscomaath.resenhaapi.mapper;

import com.franciscomaath.resenhaapi.controller.dto.response.UserResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    default UserResponseDTO toResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setUsername(user.getName());
        dto.setUserType(user.getUserType());
        dto.setFirstLogin(user.isFirstLogin());
        dto.setHasPin(user.getPinHash() != null);
        dto.setCreatedAt(user.getCreatedAt());
        return dto;
    }
}
