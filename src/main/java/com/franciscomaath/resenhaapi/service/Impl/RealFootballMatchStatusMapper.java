package com.franciscomaath.resenhaapi.service.Impl;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
class RealFootballMatchStatusMapper {

    RealFootballMatchState map(String status) {
        if (status == null || status.isBlank()) {
            return RealFootballMatchState.NOT_STARTED;
        }

        String normalized = status.trim().toLowerCase(Locale.ROOT);
        if (normalized.equals("finished")
                || normalized.equals("after et")
                || normalized.equals("after pen.")
                || normalized.equals("after pen")) {
            return RealFootballMatchState.FINISHED;
        }

        if (normalized.equals("cancelled")
                || normalized.equals("canceled")
                || normalized.equals("postponed")
                || normalized.equals("abandoned")) {
            return RealFootballMatchState.CANCELLED;
        }

        if (normalized.matches("\\d+")
                || normalized.contains("'")
                || normalized.equals("half time")
                || normalized.equals("ht")
                || normalized.equals("break")
                || normalized.contains("half")
                || normalized.contains("extra time")
                || normalized.contains("penalty")) {
            return RealFootballMatchState.LIVE;
        }

        return RealFootballMatchState.UNKNOWN;
    }
}
