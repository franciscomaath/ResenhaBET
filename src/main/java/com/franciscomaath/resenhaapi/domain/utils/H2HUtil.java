package com.franciscomaath.resenhaapi.domain.utils;

import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.service.dto.H2HRecord;

import java.util.List;

public final class H2HUtil {

    private H2HUtil() {
    }

    public static H2HRecord buildH2HRecord(List<Event> h2hEvents, Long homePlayerId, Long awayPlayerId) {
        int homeWins = 0;
        int awayWins = 0;
        int draws = 0;

        for (Event pastEvent : h2hEvents) {
            boolean homeWasHome = pastEvent.getPlayerHome().getId().equals(homePlayerId);
            int pastHomeScore = pastEvent.getHomeScore();
            int pastAwayScore = pastEvent.getAwayScore();

            if (pastHomeScore == pastAwayScore) {
                draws++;
            } else if (homeWasHome) {
                if (pastHomeScore > pastAwayScore) {
                    homeWins++;
                } else {
                    awayWins++;
                }
            } else {
                if (pastAwayScore > pastHomeScore) {
                    homeWins++;
                } else {
                    awayWins++;
                }
            }
        }

        return new H2HRecord(homeWins, awayWins, draws);
    }
}
