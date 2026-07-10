package com.franciscomaath.resenhaapi.mapper;

import com.franciscomaath.resenhaapi.controller.dto.response.TournamentRoundResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.TournamentRound;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TournamentMapperTest {

    private final TournamentMapper tournamentMapper = Mappers.getMapper(TournamentMapper.class);

    @Test
    void toRoundResponse_shouldMapIdToRoundId() {
        TournamentRound round = TournamentRound.builder()
                .id(42L)
                .name("Rodada 1")
                .multiplier(BigDecimal.ONE)
                .roundOrder(1)
                .build();

        TournamentRoundResponseDTO result = tournamentMapper.toRoundResponse(round);

        assertEquals(42L, result.getRoundId());
        assertEquals("Rodada 1", result.getName());
        assertEquals(BigDecimal.ONE, result.getMultiplier());
        assertEquals(1, result.getRoundOrder());
    }
}

