package com.franciscomaath.resenhaapi.listener;

import com.franciscomaath.resenhaapi.domain.event.EventMarketsCloseRequestedEvent;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.service.MarketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventMarketsCloseRequestedListener {

    private final MarketService marketService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventMarketsCloseRequested(EventMarketsCloseRequestedEvent event) {
        log.info("Closing markets for event {}", event.getEventId());

        try {
            marketService.closeMarket(event.getEventId());
        } catch (ResourceNotFoundException ex) {
            log.warn("Could not close markets for event {}: {}", event.getEventId(), ex.getMessage());
        }
    }
}
