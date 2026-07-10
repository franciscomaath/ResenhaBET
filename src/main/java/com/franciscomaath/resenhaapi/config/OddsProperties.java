package com.franciscomaath.resenhaapi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "resenhabet.odds")
public class OddsProperties {
    private BigDecimal drawFactor;
    private BigDecimal maxH2hWeight;
    private BigDecimal minOdd;
    private int h2hMatchLimit;
    private int eloScale;

}

