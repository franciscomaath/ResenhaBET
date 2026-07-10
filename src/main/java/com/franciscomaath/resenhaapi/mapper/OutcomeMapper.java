package com.franciscomaath.resenhaapi.mapper;

import com.franciscomaath.resenhaapi.controller.dto.response.OutcomeResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Outcome;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OutcomeMapper {

    OutcomeResponseDTO toResponse(Outcome outcome);
}
