package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.CompetitionRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.CompetitionResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Competition;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import com.franciscomaath.resenhaapi.domain.repository.CompetitionRepository;
import com.franciscomaath.resenhaapi.mapper.CompetitionMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceImplTest {

    @Mock
    private CompetitionRepository competitionRepository;

    @Mock
    private CompetitionMapper competitionMapper;

    @Mock
    private CurrentUserContext currentUserContext;

    @InjectMocks
    private CompetitionServiceImpl competitionService;

    @Test
    void create_withValidData_shouldPersistAndReturnResponse() {
        CompetitionRequestDTO request = new CompetitionRequestDTO();
        request.setName("Copa do Mundo 2026");
        request.setSeason("2026");
        request.setApiFootballLeagueId("28");
        request.setApiFootballCountryId("8");
        request.setGameForecastLeagueId("149");

        Competition savedCompetition = Competition.builder()
                .id(1L)
                .uuid(UUID.randomUUID())
                .name("Copa do Mundo 2026")
                .season("2026")
                .apiFootballLeagueId("28")
                .apiFootballCountryId("8")
                .gameForecastLeagueId("149")
                .build();

        CompetitionResponseDTO responseDTO = new CompetitionResponseDTO();
        responseDTO.setId(1L);
        responseDTO.setName("Copa do Mundo 2026");

        when(competitionRepository.save(any(Competition.class))).thenReturn(savedCompetition);
        when(competitionMapper.toResponse(savedCompetition)).thenReturn(responseDTO);

        CompetitionResponseDTO result = competitionService.create(request);

        ArgumentCaptor<Competition> captor = ArgumentCaptor.forClass(Competition.class);
        verify(competitionRepository).save(captor.capture());
        Competition persisted = captor.getValue();

        assertEquals("Copa do Mundo 2026", persisted.getName());
        assertEquals("2026", persisted.getSeason());
        assertEquals("28", persisted.getApiFootballLeagueId());
        assertEquals("8", persisted.getApiFootballCountryId());
        assertEquals("149", persisted.getGameForecastLeagueId());
        assertNotNull(persisted.getUuid());
        assertEquals(1L, result.getId());
        assertEquals("Copa do Mundo 2026", result.getName());
    }

    @Test
    void create_whenNotAdmin_shouldThrowUnauthorized() {
        doThrow(new UnauthorizedException("Not authorized")).when(currentUserContext).requireAdmin();

        assertThrows(UnauthorizedException.class,
                () -> competitionService.create(new CompetitionRequestDTO()));

        verify(competitionRepository, never()).save(any());
    }

    @Test
    void findAll_withActiveTrue_shouldReturnActiveCompetitions() {
        Competition competition = Competition.builder().id(1L).name("Copa").build();
        CompetitionResponseDTO dto = new CompetitionResponseDTO();
        dto.setId(1L);
        dto.setName("Copa");

        when(competitionRepository.findByActiveTrue()).thenReturn(List.of(competition));
        when(competitionMapper.toResponseList(List.of(competition))).thenReturn(List.of(dto));

        List<CompetitionResponseDTO> result = competitionService.findAll(true);

        assertEquals(1, result.size());
        assertEquals("Copa", result.get(0).getName());
        verify(competitionRepository, never()).findAll();
    }

    @Test
    void findAll_withActiveNull_shouldReturnAllCompetitions() {
        Competition competition = Competition.builder().id(1L).name("Copa").build();
        CompetitionResponseDTO dto = new CompetitionResponseDTO();
        dto.setId(1L);
        dto.setName("Copa");

        when(competitionRepository.findAll()).thenReturn(List.of(competition));
        when(competitionMapper.toResponseList(List.of(competition))).thenReturn(List.of(dto));

        List<CompetitionResponseDTO> result = competitionService.findAll(null);

        assertEquals(1, result.size());
        assertEquals("Copa", result.get(0).getName());
        verify(competitionRepository, never()).findByActiveTrue();
    }

    @Test
    void findById_shouldReturnMappedCompetition() {
        Competition competition = Competition.builder().id(5L).name("World Cup").build();
        CompetitionResponseDTO dto = new CompetitionResponseDTO();
        dto.setId(5L);
        dto.setName("World Cup");

        when(competitionRepository.findById(5L)).thenReturn(Optional.of(competition));
        when(competitionMapper.toResponse(competition)).thenReturn(dto);

        CompetitionResponseDTO result = competitionService.findById(5L);

        assertEquals(5L, result.getId());
        assertEquals("World Cup", result.getName());
    }

    @Test
    void findById_whenNotFound_shouldThrowResourceNotFoundException() {
        when(competitionRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> competitionService.findById(999L));

        assertTrue(ex.getMessage().contains("Competition"));
    }

    @Test
    void toggleActive_shouldFlipAndReturnResponse() {
        Competition competition = Competition.builder().id(1L).name("Copa").active(false).build();
        Competition updated = Competition.builder().id(1L).name("Copa").active(true).build();
        CompetitionResponseDTO dto = new CompetitionResponseDTO();
        dto.setId(1L);
        dto.setActive(true);

        when(competitionRepository.findById(1L)).thenReturn(Optional.of(competition));
        when(competitionRepository.save(any(Competition.class))).thenReturn(updated);
        when(competitionMapper.toResponse(updated)).thenReturn(dto);

        CompetitionResponseDTO result = competitionService.toggleActive(1L);

        assertTrue(competition.getActive());
        assertTrue(result.getActive());
        verify(competitionRepository).save(competition);
    }

    @Test
    void toggleActive_whenNotFound_shouldThrowResourceNotFoundException() {
        when(competitionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> competitionService.toggleActive(999L));

        verify(competitionRepository, never()).save(any());
    }

    @Test
    void toggleActive_whenNotAdmin_shouldThrowUnauthorized() {
        doThrow(new UnauthorizedException("Not authorized")).when(currentUserContext).requireAdmin();

        assertThrows(UnauthorizedException.class,
                () -> competitionService.toggleActive(1L));

        verify(competitionRepository, never()).findById(any());
        verify(competitionRepository, never()).save(any());
    }
}
