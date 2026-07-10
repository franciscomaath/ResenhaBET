package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.controller.dto.request.PlayerRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.PlayerUpdateRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Group;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.PlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.UserRepository;
import com.franciscomaath.resenhaapi.mapper.PlayerMapper;
import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.testsupport.MultiGroupFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceImplTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMapper playerMapper;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    @InjectMocks
    private PlayerServiceImpl playerService;

    @Test
    void create_shouldPersistActivePlayerAndReturnResponse() {
        PlayerRequestDTO request = new PlayerRequestDTO();
        request.setName("Francisco");
        Group group = MultiGroupFixtures.group(10L, "Group");

        Player saved = Player.builder().id(1L).name("Francisco").active(true).build();
        PlayerResponseDTO response = new PlayerResponseDTO();
        response.setId(1L);
        response.setName("Francisco");
        response.setActive(true);

        when(currentUserContext.getRequiredGroup()).thenReturn(group);
        when(playerRepository.save(any(Player.class))).thenReturn(saved);
        when(playerMapper.toResponse(saved)).thenReturn(response);

        PlayerResponseDTO result = playerService.create(request);

        ArgumentCaptor<Player> captor = ArgumentCaptor.forClass(Player.class);
        verify(playerRepository).save(captor.capture());
        Player persisted = captor.getValue();

        assertEquals("Francisco", persisted.getName());
        assertTrue(persisted.getActive());
        assertEquals(1L, result.getId());
        assertTrue(result.isActive());
    }

    @Test
    void findAll_shouldReturnMappedPlayers() {
        Player first = Player.builder().id(1L).name("One").active(true).build();
        Player second = Player.builder().id(2L).name("Two").active(false).build();

        PlayerResponseDTO firstDto = new PlayerResponseDTO();
        firstDto.setId(1L);
        firstDto.setName("One");
        firstDto.setActive(true);

        PlayerResponseDTO secondDto = new PlayerResponseDTO();
        secondDto.setId(2L);
        secondDto.setName("Two");
        secondDto.setActive(false);

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(playerRepository.findByGroupIdAndDeletedAtIsNullOrderByNameAsc(10L)).thenReturn(List.of(first, second));
        when(playerMapper.toResponse(first)).thenReturn(firstDto);
        when(playerMapper.toResponse(second)).thenReturn(secondDto);

        List<PlayerResponseDTO> result = playerService.findAll();

        assertEquals(2, result.size());
        assertEquals("One", result.get(0).getName());
        assertFalse(result.get(1).isActive());
    }

    @Test
    void findPlayerById_shouldReturnMappedPlayer() {
        Player player = Player.builder().id(15L).name("Target").active(true).build();
        PlayerResponseDTO dto = new PlayerResponseDTO();
        dto.setId(15L);
        dto.setName("Target");
        dto.setActive(true);

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(15L, 10L)).thenReturn(Optional.of(player));
        when(playerMapper.toResponse(player)).thenReturn(dto);

        PlayerResponseDTO result = playerService.findPlayerById(15L);

        assertEquals(15L, result.getId());
        assertEquals("Target", result.getName());
    }

    @Test
    void findPlayerById_whenNotFound_shouldThrowResourceNotFound() {
        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(404L, 10L)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> playerService.findPlayerById(404L));

        assertNotNull(ex.getMessage());
        assertTrue(ex.getMessage().contains("Player"));
        assertTrue(ex.getMessage().contains("404"));
    }

    @Test
    void update_shouldChangeNameAndStatus() {
        Player existing = Player.builder().id(2L).name("Old").active(true).build();
        Player saved = Player.builder().id(2L).name("Updated").active(false).build();

        PlayerUpdateRequestDTO request = new PlayerUpdateRequestDTO();
        request.setName("Updated");
        request.setActive(false);

        PlayerResponseDTO dto = new PlayerResponseDTO();
        dto.setId(2L);
        dto.setName("Updated");
        dto.setActive(false);

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(2L, 10L)).thenReturn(Optional.of(existing));
        when(playerRepository.save(existing)).thenReturn(saved);
        when(playerMapper.toResponse(saved)).thenReturn(dto);

        PlayerResponseDTO result = playerService.update(2L, request);

        assertEquals("Updated", existing.getName());
        assertFalse(existing.getActive());
        assertEquals("Updated", result.getName());
        assertFalse(result.isActive());
    }
}
