package com.franciscomaath.resenhaapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "resenhabet.scheduler")
public class SchedulerProperties {
    private boolean livePollEnabled;
    private long livePollFixedDelayMs;
    private long autoCloseGraceMinutes;
    private long finishFallbackAfterMinutes;
}
