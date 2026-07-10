package com.franciscomaath.resenhaapi.mapper;

import com.franciscomaath.resenhaapi.controller.dto.response.CompetitionResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Competition;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompetitionMapper {

    CompetitionResponseDTO toResponse(Competition competition);

    List<CompetitionResponseDTO> toResponseList(List<Competition> competitions);
}
