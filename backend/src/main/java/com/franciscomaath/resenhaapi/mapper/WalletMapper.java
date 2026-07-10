package com.franciscomaath.resenhaapi.mapper;

import com.franciscomaath.resenhaapi.controller.dto.response.WalletResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.TournamentWallet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "balance", target = "balance")
    @Mapping(source = "groupTournament.id", target = "groupTournamentId")
    @Mapping(source = "groupTournament.tournament.id", target = "tournamentId")
    @Mapping(source = "initialBalance", target = "initialBalance")
    WalletResponseDTO toResponse(TournamentWallet wallet);
}
