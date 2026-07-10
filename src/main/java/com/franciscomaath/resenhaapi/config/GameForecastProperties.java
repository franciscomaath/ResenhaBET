package com.franciscomaath.resenhaapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "resenhabet.gameforecast")
@Getter
@Setter
public class GameForecastProperties {
    private String rapidapiKey;
    private String baseUrl;
    private int minExactScoreProbability = 2;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 10000;
}
