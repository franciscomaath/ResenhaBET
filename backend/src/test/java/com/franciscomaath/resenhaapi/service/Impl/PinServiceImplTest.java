package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import com.franciscomaath.resenhaapi.domain.exception.ValidationException;
import com.franciscomaath.resenhaapi.domain.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PinServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void savePin_shouldHashPinAndValidateCorrectInput() {
        PinServiceImpl service = new PinServiceImpl(userRepository);
        User user = User.builder().id(1L).name("Francisco").build();

        service.savePin(user, "1234");

        assertNotNull(user.getSalt());
        assertNotNull(user.getPinHash());
        assertNotEquals("1234", user.getPinHash());
        assertTrue(service.verifyPin(user, "1234"));
        assertFalse(service.verifyPin(user, "9999"));
    }

    @Test
    void savePin_shouldGenerateDifferentHashesForDifferentUsers() {
        PinServiceImpl service = new PinServiceImpl(userRepository);
        User first = User.builder().id(1L).name("One").build();
        User second = User.builder().id(2L).name("Two").build();

        service.savePin(first, "1234");
        service.savePin(second, "1234");

        assertNotEquals(first.getSalt(), second.getSalt());
        assertNotEquals(first.getPinHash(), second.getPinHash());
    }

    @Test
    void savePin_whenInvalid_shouldThrowValidationException() {
        PinServiceImpl service = new PinServiceImpl(userRepository);
        User user = User.builder().id(1L).name("Francisco").build();

        assertThrows(ValidationException.class, () -> service.savePin(user, "12a4"));
        assertThrows(ValidationException.class, () -> service.savePin(user, "12345"));
    }

    @Test
    void verifyPin_whenStoredPinExistsAndInputIsMissing_shouldThrowUnauthorized() {
        PinServiceImpl service = new PinServiceImpl(userRepository);
        User user = User.builder().id(1L).name("Francisco").build();
        service.savePin(user, "1234");

        assertThrows(UnauthorizedException.class, () -> service.verifyPin(user, null));
    }
}
