package com.franciscomaath.resenhaapi.mapper;

import com.franciscomaath.resenhaapi.controller.dto.response.BetSlipItemResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.BetSlipResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.BetSlip;
import com.franciscomaath.resenhaapi.domain.entity.BetSlipItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {EventMapper.class})
public interface BetMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "tournamentId", source = "groupTournament.tournament.id")
    @Mapping(target = "groupTournamentId", source = "groupTournament.id")
    BetSlipResponseDTO toResponse(BetSlip betSlip);

    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "outcomeId", source = "outcome.id")
    @Mapping(target = "outcomeName", source = "outcome.name")
    BetSlipItemResponseDTO toItemResponse(BetSlipItem item);
}
