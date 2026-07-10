package com.franciscomaath.resenhaapi.domain.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EventMarketsCloseRequestedEvent extends ApplicationEvent {

    private final Long eventId;

    public EventMarketsCloseRequestedEvent(Object source, Long eventId) {
        super(source);
        this.eventId = eventId;
    }

}
