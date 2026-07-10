package com.franciscomaath.resenhaapi.mapper;

import com.franciscomaath.resenhaapi.controller.dto.response.MarketResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.OutcomeResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Market;
import com.franciscomaath.resenhaapi.domain.entity.Outcome;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MarketMapper {

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "outcomes", ignore = true)
    @Mapping(target = "marketType", source = "marketType")
    MarketResponseDTO toResponse(Market market);

    OutcomeResponseDTO toOutcomeResponse(Outcome outcome);
}
