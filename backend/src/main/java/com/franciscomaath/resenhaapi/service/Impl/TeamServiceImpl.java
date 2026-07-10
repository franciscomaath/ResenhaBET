package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.TeamRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TeamResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Team;
import com.franciscomaath.resenhaapi.domain.exception.DuplicateResourceException;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.repository.TeamRepository;
import com.franciscomaath.resenhaapi.mapper.TeamMapper;
import com.franciscomaath.resenhaapi.service.TeamService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamServiceImpl implements TeamService {

    private final TeamRepository teamRepository;
    private final TeamMapper teamMapper;
    private final CurrentUserContext currentUserContext;

    @Override
    @Transactional
    public TeamResponseDTO create(TeamRequestDTO dto) {
        currentUserContext.requireAdmin();

        if (teamRepository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("Team", "name", dto.getName());
        }

        if (teamRepository.existsByAbbreviation(dto.getAbbreviation())) {
            throw new DuplicateResourceException("Team", "abbreviation", dto.getAbbreviation());
        }

        Team team = Team.builder()
                .name(dto.getName())
                .abbreviation(dto.getAbbreviation())
                .badgeUrl(dto.getBadgeUrl())
                .build();

        team = teamRepository.save(team);
        return teamMapper.toResponse(team);
    }

    @Override
    public List<TeamResponseDTO> findAll() {
        List<Team> teams = teamRepository.findAll();
        return teams.stream()
                .map(teamMapper::toResponse)
                .toList();
    }

    @Override
    public TeamResponseDTO findById(Long id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));

        return teamMapper.toResponse(team);
    }

    @Override
    @Transactional
    public TeamResponseDTO updateGameForecastTeamId(Long id, String gameForecastTeamId) {
        currentUserContext.requireAdmin();

        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));

        team.setGameForecastTeamId(gameForecastTeamId);
        team = teamRepository.save(team);
        log.info("Updated gameForecastTeamId for team id={}", id);
        return teamMapper.toResponse(team);
    }
}


