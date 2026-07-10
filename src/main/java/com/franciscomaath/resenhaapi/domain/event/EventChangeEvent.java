package com.franciscomaath.resenhaapi.domain.event;

import com.franciscomaath.resenhaapi.controller.dto.response.EventResponseDTO;
import org.springframework.context.ApplicationEvent;

/**
 * Spring ApplicationEvent published whenever an event changes and should be
 * broadcast to WebSocket subscribers.
 */
public class EventChangeEvent extends ApplicationEvent {

    private final Long eventId;
    private final EventResponseDTO dto;

    public EventChangeEvent(Object source, Long eventId, EventResponseDTO dto) {
        super(source);
        this.eventId = eventId;
        this.dto = dto;
    }

    public Long getEventId() {
        return eventId;
    }

    public EventResponseDTO getDto() {
        return dto;
    }
}
