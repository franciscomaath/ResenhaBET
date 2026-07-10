package com.franciscomaath.resenhaapi.service.validator;

import com.franciscomaath.resenhaapi.controller.dto.response.TournamentGroupConfigResponseDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class TournamentGroupConfigValidator {

    private static final int MIN_PLAYERS_PER_GROUP = 2;

    public void validate(int playerCount, int groupCount) {
        if (playerCount < 1) {
            throw new IllegalArgumentException("Player count must be at least 1");
        }
        if (groupCount < 1) {
            throw new IllegalArgumentException("Group count must be at least 1");
        }
        int playersPerGroup = playerCount / groupCount;
        if (playersPerGroup < MIN_PLAYERS_PER_GROUP) {
            throw new IllegalArgumentException(
                    String.format("Invalid configuration: %d players in %d groups = %d players per group. Minimum is %d.",
                            playerCount, groupCount, playersPerGroup, MIN_PLAYERS_PER_GROUP));
        }
    }

    public TournamentGroupConfigResponseDTO getValidConfigs(int playerCount) {
        if (playerCount < 1) {
            throw new IllegalArgumentException("Player count must be at least 1");
        }

        TournamentGroupConfigResponseDTO response = new TournamentGroupConfigResponseDTO();
        response.setPlayerCount(playerCount);
        response.setValidOptions(new ArrayList<>());

        int maxGroups = playerCount / MIN_PLAYERS_PER_GROUP;

        for (int g = 1; g <= maxGroups; g++) {
            int playersPerGroup = playerCount / g;
            int remainder = playerCount % g;

            if (playersPerGroup >= MIN_PLAYERS_PER_GROUP) {
                TournamentGroupConfigResponseDTO.TournamentGroupOptionDTO option =
                        new TournamentGroupConfigResponseDTO.TournamentGroupOptionDTO();
                option.setGroupCount(g);
                option.setPlayersPerGroup(playersPerGroup);
                option.setRemainder(remainder);
                response.getValidOptions().add(option);
            }
        }

        return response;
    }

    public int getPlayersPerGroup(int playerCount, int groupCount) {
        validate(playerCount, groupCount);
        return playerCount / groupCount;
    }

    public int getRemainder(int playerCount, int groupCount) {
        validate(playerCount, groupCount);
        return playerCount % groupCount;
    }
}