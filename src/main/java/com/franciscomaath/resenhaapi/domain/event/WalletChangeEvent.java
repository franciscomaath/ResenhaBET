package com.franciscomaath.resenhaapi.domain.event;

import com.franciscomaath.resenhaapi.controller.dto.response.WalletResponseDTO;

/**
 * Spring ApplicationEvent published whenever a wallet balance changes and should be
 * broadcast to WebSocket subscribers.
 */
public class WalletChangeEvent extends org.springframework.context.ApplicationEvent {

    private final Long userId;
    private final WalletResponseDTO dto;

    public WalletChangeEvent(Object source, Long userId, WalletResponseDTO dto) {
        super(source);
        this.userId = userId;
        this.dto = dto;
    }

    public Long getUserId() {
        return userId;
    }

    public WalletResponseDTO getDto() {
        return dto;
    }
}
