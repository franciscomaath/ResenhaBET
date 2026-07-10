package com.franciscomaath.resenhaapi.mapper;

import com.franciscomaath.resenhaapi.controller.dto.response.PlayerResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlayerMapper {

    @Mapping(target = "active", source = "active")
    @Mapping(target = "userId", source = "user.id")
    PlayerResponseDTO toResponse(Player player);
}
