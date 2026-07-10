package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.UserLoginRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.request.UserUpdatePinRequestDTO;
import com.franciscomaath.resenhaapi.domain.entity.Session;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.enums.GroupRole;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import com.franciscomaath.resenhaapi.domain.repository.GroupMemberRepository;
import com.franciscomaath.resenhaapi.domain.repository.SessionRepository;
import com.franciscomaath.resenhaapi.domain.repository.UserRepository;
import com.franciscomaath.resenhaapi.mapper.UserMapper;
import com.franciscomaath.resenhaapi.service.PinService;
import com.franciscomaath.resenhaapi.testsupport.MultiGroupFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private PinService pinService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    private final CurrentUserContext currentUserContext = new CurrentUserContext();
    private final UserMapper userMapper = new UserMapper() {};

    @Test
    void login_withoutPin_shouldCreateSessionAndClearFirstLogin() {
        AuthServiceImpl service = service();
        User user = User.builder().id(1L).name("Francisco").userType(UserType.USER).firstLogin(true).build();
        UserLoginRequestDTO request = new UserLoginRequestDTO();
        request.setName("Francisco");

        when(userRepository.findByNameIgnoreCase("Francisco")).thenReturn(Optional.of(user));
        when(groupMemberRepository.findByUserIdOrderByGroupNameAsc(1L)).thenReturn(java.util.List.of());
        when(pinService.verifyPin(user, null)).thenReturn(true);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.login(request);

        assertNotNull(response.getToken());
        assertEquals(1L, response.getId());
        assertEquals("Francisco", response.getName());
        assertTrue(response.isFirstLogin());
        assertFalse(user.isFirstLogin());
        verify(userRepository).save(user);
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void login_withWrongPin_shouldThrowUnauthorized() {
        AuthServiceImpl service = service();
        User user = User.builder().id(1L).name("Francisco").userType(UserType.USER).firstLogin(false).pinHash("hash").build();

        UserLoginRequestDTO request = new UserLoginRequestDTO();
        request.setName("Francisco");
        request.setPin("9999");

        when(userRepository.findByNameIgnoreCase("Francisco")).thenReturn(Optional.of(user));
        when(pinService.verifyPin(user, "9999")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> service.login(request));
    }

    @Test
    void me_shouldReturnCurrentUserFromContext() {
        AuthServiceImpl service = service();
        UUID token = UUID.randomUUID();
        User user = User.builder().id(1L).name("Admin").userType(UserType.ADMIN).firstLogin(false).build();
        currentUserContext.set(user, MultiGroupFixtures.group(1L, "Group"), token);

        var response = service.me();

        assertEquals("Admin", response.getName());
        assertEquals(UserType.ADMIN, response.getUserType());
        currentUserContext.clear();
    }

    @Test
    void logout_shouldDeleteSessionByToken() {
        AuthServiceImpl service = service();
        UUID token = UUID.randomUUID();

        service.logout(token);

        verify(sessionRepository).deleteByToken(token);
    }

    @Test
    void updatePin_shouldUseAuthenticatedUser() {
        AuthServiceImpl service = service();
        User user = User.builder().id(1L).name("Francisco").build();
        currentUserContext.set(user, MultiGroupFixtures.group(1L, "Group"), UUID.randomUUID());
        UserUpdatePinRequestDTO request = new UserUpdatePinRequestDTO();
        request.setPin("1234");

        service.updatePin(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(pinService).savePin(userCaptor.capture(), org.mockito.Mockito.eq("1234"));
        assertEquals(1L, userCaptor.getValue().getId());
        currentUserContext.clear();
    }

    @Test
    void login_withMembership_shouldSetCurrentGroupInSessionAndResponse() {
        AuthServiceImpl service = service();
        User user = MultiGroupFixtures.user(1L, "Francisco", UserType.USER);
        var group = MultiGroupFixtures.group(2L, "Copa Gege");
        var member = MultiGroupFixtures.groupMember(group, user, GroupRole.MEMBER);
        UserLoginRequestDTO request = new UserLoginRequestDTO();
        request.setName("Francisco");

        when(userRepository.findByNameIgnoreCase("Francisco")).thenReturn(Optional.of(user));
        when(pinService.verifyPin(user, null)).thenReturn(true);
        when(groupMemberRepository.findByUserIdOrderByGroupNameAsc(1L)).thenReturn(java.util.List.of(member));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.login(request);

        assertEquals(2L, response.getCurrentGroupId());
        assertEquals("Copa Gege", response.getCurrentGroupName());
        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        assertEquals(group, sessionCaptor.getValue().getCurrentGroup());
    }

    private AuthServiceImpl service() {
        return new AuthServiceImpl(pinService, userRepository, sessionRepository, groupMemberRepository, currentUserContext, userMapper);
    }
}
