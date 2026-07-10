package com.franciscomaath.resenhaapi.listener;

import com.franciscomaath.resenhaapi.domain.event.EventChangeEvent;
import com.franciscomaath.resenhaapi.domain.event.OddsRecalculationEvent;
import com.franciscomaath.resenhaapi.service.OddsUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OddsRecalculationListener {

    private final OddsUpdateService oddsUpdateService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOddsRecalculation(EventChangeEvent event) {
        if("REAL_FOOTBALL".equals(event.getDto().getTournamentType())){
            log.info("No need to recalculate odds on REAL FOOTBALL tournaments.");
            return;
        }
        log.info("Received OddsRecalculationEvent for tournament {}", event.getDto().getTournamentId());
        oddsUpdateService.recalculateFutureOdds(event.getDto().getTournamentId());
    }
}
