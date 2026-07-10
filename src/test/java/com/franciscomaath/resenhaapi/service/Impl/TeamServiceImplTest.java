package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.TeamRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.TeamResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Team;
import com.franciscomaath.resenhaapi.domain.exception.DuplicateResourceException;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import com.franciscomaath.resenhaapi.domain.repository.TeamRepository;
import com.franciscomaath.resenhaapi.mapper.TeamMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceImplTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMapper teamMapper;

    @Mock
    private CurrentUserContext currentUserContext;

    @InjectMocks
    private TeamServiceImpl teamService;

    @Test
    void create_shouldPersistTeamWhenDataIsUnique() {
        TeamRequestDTO request = new TeamRequestDTO();
        request.setName("Falcons");
        request.setAbbreviation("FLC");
        request.setBadgeUrl("http://badge");

        Team team = Team.builder().id(1L).name("Falcons").abbreviation("FLC").badgeUrl("http://badge").build();
        TeamResponseDTO dto = new TeamResponseDTO();
        dto.setId(1L);
        dto.setName("Falcons");
        dto.setAbbreviation("FLC");

        when(teamRepository.existsByName("Falcons")).thenReturn(false);
        when(teamRepository.existsByAbbreviation("FLC")).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenReturn(team);
        when(teamMapper.toResponse(team)).thenReturn(dto);

        TeamResponseDTO result = teamService.create(request);

        assertEquals(1L, result.getId());
        assertEquals("Falcons", result.getName());
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    void create_whenNameAlreadyExists_shouldThrowDuplicateResourceException() {
        TeamRequestDTO request = new TeamRequestDTO();
        request.setName("Falcons");
        request.setAbbreviation("FLC");

        when(teamRepository.existsByName("Falcons")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> teamService.create(request));

        assertTrue(ex.getMessage().contains("Team"));
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void create_whenAbbreviationAlreadyExists_shouldThrowDuplicateResourceException() {
        TeamRequestDTO request = new TeamRequestDTO();
        request.setName("Falcons");
        request.setAbbreviation("FLC");

        when(teamRepository.existsByName("Falcons")).thenReturn(false);
        when(teamRepository.existsByAbbreviation("FLC")).thenReturn(true);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class,
                () -> teamService.create(request));

        assertTrue(ex.getMessage().contains("abbreviation"));
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    void findAll_shouldReturnMappedTeams() {
        Team first = Team.builder().id(1L).name("Falcons").abbreviation("FLC").build();
        Team second = Team.builder().id(2L).name("Sharks").abbreviation("SHK").build();

        TeamResponseDTO firstDto = new TeamResponseDTO();
        firstDto.setId(1L);
        firstDto.setName("Falcons");

        TeamResponseDTO secondDto = new TeamResponseDTO();
        secondDto.setId(2L);
        secondDto.setName("Sharks");

        when(teamRepository.findAll()).thenReturn(List.of(first, second));
        when(teamMapper.toResponse(first)).thenReturn(firstDto);
        when(teamMapper.toResponse(second)).thenReturn(secondDto);

        List<TeamResponseDTO> result = teamService.findAll();

        assertEquals(2, result.size());
        assertEquals("Falcons", result.get(0).getName());
        assertEquals("Sharks", result.get(1).getName());
    }

    @Test
    void findById_shouldReturnMappedTeam() {
        Team team = Team.builder().id(10L).name("Wolves").abbreviation("WLV").build();
        TeamResponseDTO dto = new TeamResponseDTO();
        dto.setId(10L);
        dto.setName("Wolves");

        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(teamMapper.toResponse(team)).thenReturn(dto);

        TeamResponseDTO result = teamService.findById(10L);

        assertEquals(10L, result.getId());
        assertEquals("Wolves", result.getName());
    }

    @Test
    void findById_whenNotFound_shouldThrowResourceNotFoundException() {
        when(teamRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> teamService.findById(999L));

        assertTrue(ex.getMessage().contains("Team"));
    }

    @Test
    void updateGameForecastTeamId_shouldSetGameForecastId() {
        Team team = Team.builder().id(1L).name("Brazil").abbreviation("BRA").build();
        TeamResponseDTO dto = new TeamResponseDTO();
        dto.setId(1L);
        dto.setName("Brazil");

        when(teamRepository.findById(1L)).thenReturn(Optional.of(team));
        when(teamRepository.save(any(Team.class))).thenReturn(team);
        when(teamMapper.toResponse(team)).thenReturn(dto);

        TeamResponseDTO result = teamService.updateGameForecastTeamId(1L, "gf-123");

        assertEquals("gf-123", team.getGameForecastTeamId());
        assertEquals(1L, result.getId());
        verify(teamRepository).save(team);
    }

    @Test
    void updateGameForecastTeamId_whenNotFound_shouldThrow() {
        when(teamRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> teamService.updateGameForecastTeamId(999L, "gf-123"));

        verify(teamRepository, never()).save(any());
    }

    @Test
    void updateGameForecastTeamId_whenNotAdmin_shouldThrow() {
        doThrow(new UnauthorizedException("Not authorized")).when(currentUserContext).requireAdmin();

        assertThrows(UnauthorizedException.class,
                () -> teamService.updateGameForecastTeamId(1L, "gf-123"));

        verify(teamRepository, never()).findById(any());
        verify(teamRepository, never()).save(any());
    }
}

