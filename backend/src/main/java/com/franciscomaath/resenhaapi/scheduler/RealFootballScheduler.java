package com.franciscomaath.resenhaapi.scheduler;

import com.franciscomaath.resenhaapi.config.SchedulerProperties;
import com.franciscomaath.resenhaapi.service.RealFootballLiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RealFootballScheduler {

    private final RealFootballLiveService realFootballLiveService;
    private final SchedulerProperties schedulerProperties;

    @Scheduled(fixedDelayString = "${resenhabet.scheduler.live-poll-fixed-delay-ms:60000}")
    public void tick() {
        if (!schedulerProperties.isLivePollEnabled()) {
            return;
        }

        try {
            realFootballLiveService.tick();
        } catch (RuntimeException ex) {
            log.error("REAL_FOOTBALL scheduler tick failed", ex);
        }
    }
}
