package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.LinkUserRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.PlayerResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import com.franciscomaath.resenhaapi.domain.exception.BusinessException;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.PlayerRepository;
import com.franciscomaath.resenhaapi.domain.repository.UserRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentPlayerRepository;
import com.franciscomaath.resenhaapi.mapper.PlayerMapper;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.testsupport.MultiGroupFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerLinkUserServiceImplTest {

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMapper playerMapper;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    @Mock
    private TournamentPlayerRepository tournamentPlayerRepository;

    private final CurrentUserContext currentUserContext = new CurrentUserContext();

    @Test
    void linkUser_whenBothAreFree_shouldLinkAndReturnPlayer() {
        PlayerServiceImpl service = service();
        User admin = User.builder().id(1L).name("Admin").userType(UserType.ADMIN).build();
        User user = User.builder().id(2L).name("User").userType(UserType.USER).build();
        Player player = Player.builder().id(5L).name("Player").active(true).build();
        LinkUserRequestDTO request = new LinkUserRequestDTO();
        request.setUserId(2L);
        PlayerResponseDTO response = new PlayerResponseDTO();
        response.setId(5L);
        response.setUserId(2L);
        currentUserContext.set(admin, MultiGroupFixtures.group(10L, "Group"), UUID.randomUUID());

        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(5L, 10L)).thenReturn(Optional.of(player));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(playerRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(false);
        when(playerRepository.save(player)).thenReturn(player);
        when(playerMapper.toResponse(player)).thenReturn(response);

        var result = service.linkUser(5L, request);

        assertEquals(user, player.getUser());
        assertEquals(2L, result.getUserId());
        currentUserContext.clear();
    }

    @Test
    void linkUser_whenPlayerAlreadyHasUser_shouldThrowBusinessException() {
        PlayerServiceImpl service = service();
        User admin = User.builder().id(1L).name("Admin").userType(UserType.ADMIN).build();
        User linkedUser = User.builder().id(3L).name("Linked").userType(UserType.USER).build();
        Player player = Player.builder().id(5L).name("Player").active(true).user(linkedUser).build();
        LinkUserRequestDTO request = new LinkUserRequestDTO();
        request.setUserId(2L);
        currentUserContext.set(admin, MultiGroupFixtures.group(10L, "Group"), UUID.randomUUID());

        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(5L, 10L)).thenReturn(Optional.of(player));

        assertThrows(BusinessException.class, () -> service.linkUser(5L, request));
        currentUserContext.clear();
    }

    @Test
    void linkUser_whenUserAlreadyLinkedToAnotherPlayer_shouldThrowBusinessException() {
        PlayerServiceImpl service = service();
        User admin = User.builder().id(1L).name("Admin").userType(UserType.ADMIN).build();
        User user = User.builder().id(2L).name("User").userType(UserType.USER).build();
        Player player = Player.builder().id(5L).name("Player").active(true).build();
        LinkUserRequestDTO request = new LinkUserRequestDTO();
        request.setUserId(2L);
        currentUserContext.set(admin, MultiGroupFixtures.group(10L, "Group"), UUID.randomUUID());

        when(playerRepository.findByIdAndGroupIdAndDeletedAtIsNull(5L, 10L)).thenReturn(Optional.of(player));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(playerRepository.existsByGroupIdAndUserId(10L, 2L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.linkUser(5L, request));
        currentUserContext.clear();
    }

    private PlayerServiceImpl service() {
        return new PlayerServiceImpl(
                playerRepository,
                playerMapper,
                userRepository,
                currentUserContext,
                eventRepository,
                groupAuthorizationService,
                tournamentPlayerRepository
        );
    }
}
