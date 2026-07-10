package com.franciscomaath.resenhaapi.mapper;

import com.franciscomaath.resenhaapi.controller.dto.response.TournamentRoundResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Tournament;
import com.franciscomaath.resenhaapi.domain.entity.TournamentRound;
import com.franciscomaath.resenhaapi.domain.enums.MarketType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

    @Mapper(componentModel = "spring")
public interface TournamentMapper {

    @Mapping(target = "competitionId", source = "tournament.competition.id")
    @Mapping(target = "competitionName", source = "tournament.competition.name")
    @Mapping(target = "groupTournamentId", source = "groupTournamentId")
    @Mapping(target = "marketTypes", ignore = true)
    TournamentResponseDTO toResponse(Tournament tournament, Long groupTournamentId);

    default TournamentResponseDTO toResponse(Tournament tournament, Long groupTournamentId, Set<MarketType> marketTypes) {
        TournamentResponseDTO dto = toResponse(tournament, groupTournamentId);
        if (marketTypes != null) {
            dto.setMarketTypes(marketTypes.stream()
                    .map(Enum::name)
                    .collect(java.util.stream.Collectors.toSet()));
        }
        return dto;
    }

    default TournamentResponseDTO toResponse(Tournament tournament) {
        return toResponse(tournament, null, null);
    }

    default List<TournamentResponseDTO> toResponseList(List<Tournament> tournaments, Long groupTournamentId) {
        if (tournaments == null) {
            return null;
        }

        return tournaments.stream()
                .map(tournament -> toResponse(tournament, groupTournamentId))
                .collect(Collectors.toList());
    }

    default List<TournamentResponseDTO> toResponseList(List<Tournament> tournaments) {
        return toResponseList(tournaments, null);
    }

    @Mapping(target = "roundId", source = "id")
    @Mapping(target = "phaseType", source = "phaseType")
    TournamentRoundResponseDTO toRoundResponse(TournamentRound round);

    default Page<TournamentResponseDTO> toResponsePage(Page<Tournament> tournaments, Long groupTournamentId) {
        if (tournaments == null) {
            return null;
        }

        List<TournamentResponseDTO> content = toResponseList(tournaments.getContent(), groupTournamentId);

        return new PageImpl<>(content, tournaments.getPageable(), tournaments.getTotalElements());
    }

    default Page<TournamentResponseDTO> toResponsePage(Page<Tournament> tournaments) {
        return toResponsePage(tournaments, null);
    }
}
