package com.franciscomaath.resenhaapi.service.dto;

import java.math.BigDecimal;

public class OddsResult {
    private final BigDecimal homeOdd;
    private final BigDecimal drawOdd;
    private final BigDecimal awayOdd;
    private final double pHome;
    private final double pAway;

    public OddsResult(BigDecimal homeOdd, BigDecimal drawOdd, BigDecimal awayOdd) {
        this(homeOdd, drawOdd, awayOdd, 0.0d, 0.0d);
    }

    public OddsResult(BigDecimal homeOdd, BigDecimal drawOdd, BigDecimal awayOdd, double pHome, double pAway) {
        this.homeOdd = homeOdd;
        this.drawOdd = drawOdd;
        this.awayOdd = awayOdd;
        this.pHome = pHome;
        this.pAway = pAway;
    }

    public BigDecimal getHomeOdd() {
        return homeOdd;
    }

    public BigDecimal getDrawOdd() {
        return drawOdd;
    }

    public BigDecimal getAwayOdd() {
        return awayOdd;
    }

    public double getPHome() {
        return pHome;
    }

    public double getPAway() {
        return pAway;
    }
}

