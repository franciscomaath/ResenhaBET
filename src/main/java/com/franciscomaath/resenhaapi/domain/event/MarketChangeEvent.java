package com.franciscomaath.resenhaapi.domain.event;

import com.franciscomaath.resenhaapi.controller.dto.response.MarketResponseDTO;
import lombok.Getter;

/**
 * Spring ApplicationEvent published whenever a market changes and should be
 * broadcast to WebSocket subscribers.
 */
@Getter
public class MarketChangeEvent extends org.springframework.context.ApplicationEvent {

    private final Long eventId;
    private final MarketResponseDTO dto;

    public MarketChangeEvent(Object source, Long eventId, MarketResponseDTO dto) {
        super(source);
        this.eventId = eventId;
        this.dto = dto;
    }

}
