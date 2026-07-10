package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.request.PlayerRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.PlayerUpdateRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.LinkUserRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.PlayerActiveRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerStatsResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerInviteResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface PlayerService {

    PlayerResponseDTO create(PlayerRequestDTO dto);

    List<PlayerResponseDTO> findAll();

    PlayerResponseDTO findPlayerById(Long id);

    PlayerResponseDTO update(Long id, PlayerUpdateRequestDTO dto);

    PlayerResponseDTO linkUser(Long id, LinkUserRequestDTO dto);

    PlayerStatsResponseDTO getPlayerStats(Long playerId, Long tournamentId);

    PlayerResponseDTO changeActiveStatus(Long id, PlayerActiveRequestDTO dto);

    void softDeletePlayer(Long id);

    PlayerInviteResponseDTO generateInvite(Long id);
}
