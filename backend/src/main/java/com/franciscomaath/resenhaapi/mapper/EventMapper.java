package com.franciscomaath.resenhaapi.mapper;

import com.franciscomaath.resenhaapi.controller.dto.response.EventResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "tournamentId", source = "tournament.id")
    @Mapping(target = "tournamentType", source = "tournament.type")
    @Mapping(target = "roundId", source = "round.id")
    @Mapping(target = "playerHomeId", source = "playerHome.id")
    @Mapping(target = "playerAwayId", source = "playerAway.id")
    @Mapping(target = "playerHomeName", source = "playerHome.name")
    @Mapping(target = "playerAwayName", source = "playerAway.name")
    @Mapping(target = "teamHomeId", source = "teamHome.id")
    @Mapping(target = "teamHomeName", source = "teamHome.name")
    @Mapping(target = "teamAwayId", source = "teamAway.id")
    @Mapping(target = "teamAwayName", source = "teamAway.name")
    @Mapping(target = "nextRoundEventId", source = "nextRoundEvent.id")
    @Mapping(target = "homeSourceEventId", source = "homeSourceEvent.id")
    @Mapping(target = "awaySourceEventId", source = "awaySourceEvent.id")
    @Mapping(target = "thirdPlaceMatch", source = "thirdPlaceMatch")
    EventResponseDTO toResponse(Event event);
}

