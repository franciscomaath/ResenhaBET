package com.franciscomaath.resenhaapi.domain.event;

import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import org.springframework.context.ApplicationEvent;

public class EventCompletedEvent extends ApplicationEvent {

    private final Long eventId;
    private final EventStatus status;

    public EventCompletedEvent(Object source, Long eventId, EventStatus status) {
        super(source);
        this.eventId = eventId;
        this.status = status;
    }

    public Long getEventId() {
        return eventId;
    }

    public EventStatus getStatus() {
        return status;
    }
}
