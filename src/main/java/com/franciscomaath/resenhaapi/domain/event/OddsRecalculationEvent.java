package com.franciscomaath.resenhaapi.domain.event;

import org.springframework.context.ApplicationEvent;

public class OddsRecalculationEvent extends ApplicationEvent {

    private final Long tournamentId;

    public OddsRecalculationEvent(Object source, Long tournamentId) {
        super(source);
        this.tournamentId = tournamentId;
    }

    public Long getTournamentId() {
        return tournamentId;
    }
}
