package com.franciscomaath.resenhaapi.listener;

import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.event.EventCompletedEvent;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.service.BetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class BetResolutionListener {

    private final BetService betService;
    private final EventRepository eventRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEventCompleted(EventCompletedEvent event) {
        log.info("Handling EventCompletedEvent for eventId={}, status={}", event.getEventId(), event.getStatus());

        if (event.getStatus() == EventStatus.COMPLETED) {
            log.info("Resolving bets for completed event {}", event.getEventId());
            betService.resolveBetsForEvent(event.getEventId());
        } else if (event.getStatus() == EventStatus.CANCELLED) {
            log.info("Cancelling bets for cancelled event {}", event.getEventId());
            betService.cancelBetsForEvent(event.getEventId());
        }
    }
}
