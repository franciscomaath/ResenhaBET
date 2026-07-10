package com.franciscomaath.resenhaapi.mapper;

import com.franciscomaath.resenhaapi.controller.dto.response.TeamResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Team;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    TeamResponseDTO toResponse(Team team);
}

