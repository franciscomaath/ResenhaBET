package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.request.PatchTournamentPlayerTeamRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.StartTournamentRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.TournamentPatchRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.TournamentPlayerRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.TournamentRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentPlayerResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentPlayersResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentRoundResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TournamentScoreboardResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TournamentService {

    TournamentResponseDTO create(TournamentRequestDTO dto);

    Page<TournamentResponseDTO> findAll(Pageable pageable);

    TournamentResponseDTO getTournamentById(Long id);

    TournamentResponseDTO updateTournament(Long id, TournamentPatchRequestDTO dto);

    void softDeleteTournament(Long id);

    void cancelTournament(Long id);

    TournamentPlayersResponseDTO findPlayersByTournamentId(Long tournamentId);

    TournamentPlayerResponseDTO addPlayerToTournament(Long tournamentId, TournamentPlayerRequestDTO dto);

    void removePlayerFromTournament(Long tournamentId, Long playerId);

    TournamentPlayerResponseDTO updateTournamentPlayerTeam(Long tournamentId, Long playerId, PatchTournamentPlayerTeamRequestDTO dto);

    List<TournamentRoundResponseDTO> findRoundsByTournamentId(Long tournamentId);

    TournamentResponseDTO startTournament(Long tournamentId, StartTournamentRequestDTO dto);

    TournamentResponseDTO advanceToBracket(Long tournamentId);

    TournamentResponseDTO forceAdvanceToBracket(Long tournamentId);

    TournamentScoreboardResponseDTO getScoreboard(Long tournamentId);

    List<com.franciscomaath.resenhaapi.controller.dto.response.BetRankingResponseDTO> getBetRanking(Long tournamentId);
}



