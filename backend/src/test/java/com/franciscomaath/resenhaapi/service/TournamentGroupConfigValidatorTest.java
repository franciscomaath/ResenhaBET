package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.response.TournamentGroupConfigResponseDTO;
import com.franciscomaath.resenhaapi.service.validator.TournamentGroupConfigValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TournamentGroupConfigValidatorTest {

    private final TournamentGroupConfigValidator validator = new TournamentGroupConfigValidator();

    @Test
    void validate_6Players1Group_shouldPass() {
        validator.validate(6, 1);
    }

    @Test
    void validate_6Players2Groups_shouldPass() {
        validator.validate(6, 2);
    }

    @Test
    void validate_6Players3Groups_shouldPass() {
        validator.validate(6, 3);
    }

    @Test
    void validate_6Players4Groups_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(6, 4));
        assertTrue(ex.getMessage().contains("Invalid configuration"));
    }

    @Test
    void validate_3Players2Groups_shouldThrow() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(3, 2));
        assertTrue(ex.getMessage().contains("Invalid configuration"));
    }

    @Test
    void validate_1Player1Group_shouldPass() {
        validator.validate(2, 1);
    }

    @Test
    void getValidConfigs_6Players_shouldReturnValidOptions() {
        TournamentGroupConfigResponseDTO result = validator.getValidConfigs(6);
        assertEquals(6, result.getPlayerCount());
        assertEquals(3, result.getValidOptions().size()); // 1 group, 2 groups and 3 groups
        assertEquals(1, result.getValidOptions().get(0).getGroupCount());
        assertEquals(6, result.getValidOptions().get(0).getPlayersPerGroup());
        assertEquals(0, result.getValidOptions().get(0).getRemainder());
        assertEquals(2, result.getValidOptions().get(1).getGroupCount());
        assertEquals(3, result.getValidOptions().get(1).getPlayersPerGroup());
        assertEquals(0, result.getValidOptions().get(1).getRemainder());
        assertEquals(3, result.getValidOptions().get(2).getGroupCount());
        assertEquals(2, result.getValidOptions().get(2).getPlayersPerGroup());
        assertEquals(0, result.getValidOptions().get(2).getRemainder());
    }

    @Test
    void getValidConfigs_7Players_shouldReturnCorrectRemainder() {
        TournamentGroupConfigResponseDTO result = validator.getValidConfigs(7);
        assertEquals(7, result.getPlayerCount());
        assertEquals(3, result.getValidOptions().size()); // 1 group, 2 groups and 3 groups
        assertEquals(1, result.getValidOptions().get(0).getGroupCount());
        assertEquals(7, result.getValidOptions().get(0).getPlayersPerGroup());
        assertEquals(0, result.getValidOptions().get(0).getRemainder());
        assertEquals(2, result.getValidOptions().get(1).getGroupCount());
        assertEquals(3, result.getValidOptions().get(1).getPlayersPerGroup());
        assertEquals(1, result.getValidOptions().get(1).getRemainder());
    }

    @Test
    void getValidConfigs_1Player_shouldReturnEmptyOptions() {
        TournamentGroupConfigResponseDTO result = validator.getValidConfigs(1);
        assertEquals(1, result.getPlayerCount());
        assertTrue(result.getValidOptions().isEmpty());
    }
}
