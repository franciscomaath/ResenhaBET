package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.UserRequestDTO;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import com.franciscomaath.resenhaapi.domain.repository.UserRepository;
import com.franciscomaath.resenhaapi.mapper.UserMapper;
import com.franciscomaath.resenhaapi.testsupport.MultiGroupFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    private final UserMapper userMapper = new UserMapper() {};
    private final CurrentUserContext currentUserContext = new CurrentUserContext();

    @Test
    void create_shouldCreateRegularUserWithoutPin() {
        UserServiceImpl service = new UserServiceImpl(userRepository, userMapper, currentUserContext);
        UserRequestDTO request = new UserRequestDTO();
        request.setName("New User");

        when(userRepository.existsByName("New User")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(10L);
            return user;
        });
        var response = service.create(request);

        assertEquals(10L, response.getId());
        assertEquals("New User", response.getName());
        assertEquals(UserType.USER, response.getUserType());
        assertTrue(response.isFirstLogin());
        assertFalse(response.isHasPin());
    }

    @Test
    void resetPin_asAdmin_shouldClearPinAndSetFirstLogin() {
        UserServiceImpl service = new UserServiceImpl(userRepository, userMapper, currentUserContext);
        User admin = User.builder().id(1L).name("Admin").userType(UserType.ADMIN).build();
        User user = User.builder().id(2L).name("User").userType(UserType.USER).pinHash("hash").salt("salt").firstLogin(false).build();
        currentUserContext.set(admin, MultiGroupFixtures.group(1L, "Group"), UUID.randomUUID());

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        service.resetPin(2L);

        assertNull(user.getPinHash());
        assertNull(user.getSalt());
        assertTrue(user.isFirstLogin());
        verify(userRepository).save(user);
        currentUserContext.clear();
    }

    @Test
    void resetPin_asRegularUser_shouldThrowUnauthorized() {
        UserServiceImpl service = new UserServiceImpl(userRepository, userMapper, currentUserContext);
        User regular = User.builder().id(1L).name("User").userType(UserType.USER).build();
        currentUserContext.set(regular, MultiGroupFixtures.group(1L, "Group"), UUID.randomUUID());

        assertThrows(UnauthorizedException.class, () -> service.resetPin(2L));
        currentUserContext.clear();
    }
}
