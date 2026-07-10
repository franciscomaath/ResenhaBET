package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import com.franciscomaath.resenhaapi.domain.exception.ValidationException;
import com.franciscomaath.resenhaapi.domain.repository.UserRepository;
import com.franciscomaath.resenhaapi.service.PinService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PinServiceImpl implements PinService {

    private static final String PIN_PATTERN = "\\d{4}";

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void savePin(User user, String rawPin) {
        validatePin(rawPin);

        String salt = generateSalt();
        String hashedPin = hashPin(rawPin, salt);

        user.setSalt(salt);
        user.setPinHash(hashedPin);
        userRepository.save(user);
    }

    @Override
    public boolean verifyPin(User user, String rawPin) {
        if (user.getPinHash() == null) {
            return rawPin == null || rawPin.isBlank();
        }

        if (rawPin == null || !rawPin.matches(PIN_PATTERN)) {
            throw new UnauthorizedException("PIN invalido.");
        }

        String inputHashedPin = hashPin(rawPin, user.getSalt());
        return inputHashedPin.equals(user.getPinHash());
    }

    private void validatePin(String rawPin) {
        if (rawPin == null || !rawPin.matches(PIN_PATTERN)) {
            throw new ValidationException("O PIN deve conter exatamente 4 digitos numericos.");
        }
    }

    private String hashPin(String rawPin, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = md.digest((salt + rawPin).getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 nao encontrado.", e);
        }
    }

    private String generateSalt() {
        byte[] salt = new byte[16];
        secureRandom.nextBytes(salt);
        return bytesToHex(salt);
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
