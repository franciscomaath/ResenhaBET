package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.CompetitionRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.CompetitionResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Competition;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.repository.CompetitionRepository;
import com.franciscomaath.resenhaapi.mapper.CompetitionMapper;
import com.franciscomaath.resenhaapi.service.CompetitionService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitionServiceImpl implements CompetitionService {

    private final CompetitionRepository competitionRepository;
    private final CompetitionMapper competitionMapper;
    private final CurrentUserContext currentUserContext;

    @Override
    @Transactional
    public CompetitionResponseDTO create(CompetitionRequestDTO dto) {
        currentUserContext.requireAdmin();

        Competition competition = Competition.builder()
                .uuid(UUID.randomUUID())
                .name(dto.getName())
                .season(dto.getSeason())
                .apiFootballLeagueId(dto.getApiFootballLeagueId())
                .apiFootballCountryId(dto.getApiFootballCountryId())
                .gameForecastLeagueId(dto.getGameForecastLeagueId())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();

        competition = competitionRepository.save(competition);
        log.info("Created competition id={}, name={}, season={}", competition.getId(), competition.getName(), competition.getSeason());
        return competitionMapper.toResponse(competition);
    }

    @Override
    public List<CompetitionResponseDTO> findAll(Boolean active) {
        List<Competition> competitions;
        if (active != null && active) {
            competitions = competitionRepository.findByActiveTrue();
        } else {
            competitions = competitionRepository.findAll();
        }
        return competitionMapper.toResponseList(competitions);
    }

    @Override
    public CompetitionResponseDTO findById(Long id) {
        Competition competition = competitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competition", "id", id));
        return competitionMapper.toResponse(competition);
    }

    @Override
    @Transactional
    public CompetitionResponseDTO toggleActive(Long id) {
        currentUserContext.requireAdmin();

        Competition competition = competitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Competition", "id", id));

        competition.setActive(!competition.getActive());
        competition = competitionRepository.save(competition);
        log.info("Toggled competition id={} active={}", competition.getId(), competition.getActive());
        return competitionMapper.toResponse(competition);
    }
}
