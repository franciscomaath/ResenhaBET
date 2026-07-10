package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.request.TeamRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TeamResponseDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TeamService {

    TeamResponseDTO create(TeamRequestDTO dto);

    List<TeamResponseDTO> findAll();

    TeamResponseDTO findById(Long id);

    TeamResponseDTO updateGameForecastTeamId(Long id, String gameForecastTeamId);
}

