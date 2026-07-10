package com.franciscomaath.resenhaapi.config.websocket;

import com.franciscomaath.resenhaapi.controller.dto.response.EventResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.MarketResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.WalletResponseDTO;
import com.franciscomaath.resenhaapi.domain.event.EventChangeEvent;
import com.franciscomaath.resenhaapi.domain.event.MarketChangeEvent;
import com.franciscomaath.resenhaapi.domain.event.WalletChangeEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventChange(EventChangeEvent event) {
        EventResponseDTO dto = event.getDto();
        String destination = "/topic/events/" + event.getEventId();
        log.debug("Broadcasting event change to {}: {}", destination, dto);
        messagingTemplate.convertAndSend(destination, dto);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMarketChange(MarketChangeEvent event) {
        MarketResponseDTO dto = event.getDto();
        String destination = "/topic/markets/" + event.getEventId();
        log.debug("Broadcasting market change to {}: {}", destination, dto);
        messagingTemplate.convertAndSend(destination, dto);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWalletChange(WalletChangeEvent event) {
        WalletResponseDTO dto = event.getDto();
        String destination = "/topic/wallet/" + event.getUserId();
        log.debug("Broadcasting wallet change to {}: {}", destination, dto);
        messagingTemplate.convertAndSend(destination, dto);
    }

    @EventListener
    public void onEventChangeRollback(EventChangeEvent event) {
        // This is a fallback; TransactionalEventListener only fires after commit.
        // No action needed on rollback.
    }
}
