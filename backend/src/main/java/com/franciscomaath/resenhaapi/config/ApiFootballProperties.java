package com.franciscomaath.resenhaapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "resenhabet.apifootball")
@Getter
@Setter
public class ApiFootballProperties {
    private String key;
    private String baseUrl;
    private int connectTimeoutMs = 3000;
    private int readTimeoutMs = 10000;
}
