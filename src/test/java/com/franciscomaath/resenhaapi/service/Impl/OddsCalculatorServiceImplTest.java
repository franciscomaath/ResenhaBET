package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.OddsProperties;
import com.franciscomaath.resenhaapi.service.dto.H2HRecord;
import com.franciscomaath.resenhaapi.service.dto.OddsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OddsCalculatorServiceImplTest {

    private OddsCalculatorServiceImpl oddsCalculator;

    @BeforeEach
    void setUp() {
        OddsProperties oddsProperties = new OddsProperties();
        oddsProperties.setDrawFactor(new BigDecimal("0.28"));
        oddsProperties.setMaxH2hWeight(new BigDecimal("0.20"));
        oddsProperties.setMinOdd(new BigDecimal("1.05"));
        oddsProperties.setH2hMatchLimit(10);
        oddsProperties.setEloScale(600);
        oddsCalculator = new OddsCalculatorServiceImpl(oddsProperties);
    }

    @Test
    void equalElosNoH2h_shouldReturnSymmetricOdds() {
        OddsResult result = oddsCalculator.calculate(bd(1000), bd(1000), null);

        assertEquals(new BigDecimal("2.78"), result.getHomeOdd());
        assertEquals(new BigDecimal("3.57"), result.getDrawOdd());
        assertEquals(new BigDecimal("2.78"), result.getAwayOdd());
        assertTrue(result.getDrawOdd().compareTo(result.getHomeOdd()) > 0);
    }

    @Test
    void equalElosWithH2hFavoringHome_shouldLowerHomeOdd() {
        OddsResult baseline = oddsCalculator.calculate(bd(1000), bd(1000), null);
        OddsResult adjusted = oddsCalculator.calculate(bd(1000), bd(1000), new H2HRecord(8, 1, 1));

        assertTrue(adjusted.getHomeOdd().compareTo(baseline.getHomeOdd()) < 0);
        assertTrue(adjusted.getAwayOdd().compareTo(baseline.getAwayOdd()) > 0);
    }

    @Test
    void largeEloGapNoH2h_shouldApplyMinOddForFavorite() {
        OddsResult result = oddsCalculator.calculate(bd(2000), bd(1000), null);

        assertEquals(new BigDecimal("1.05"), result.getHomeOdd());
        assertTrue(result.getAwayOdd().compareTo(result.getHomeOdd()) > 0);
        assertTrue(result.getDrawOdd().compareTo(result.getHomeOdd()) > 0);
    }

    @Test
    void largeEloGapWithH2hFavoringUnderdog_shouldReduceUnderdogOdd() {
        OddsResult baseline = oddsCalculator.calculate(bd(2000), bd(1000), null);
        OddsResult adjusted = oddsCalculator.calculate(bd(2000), bd(1000), new H2HRecord(1, 8, 1));

        assertTrue(adjusted.getAwayOdd().compareTo(baseline.getAwayOdd()) < 0);
        assertTrue(adjusted.getHomeOdd().compareTo(baseline.getHomeOdd()) > 0);
    }

    @Test
    void h2hWithZeroMatches_shouldMatchNoH2hResult() {
        OddsResult baseline = oddsCalculator.calculate(bd(1200), bd(1100), null);
        OddsResult adjusted = oddsCalculator.calculate(bd(1200), bd(1100), new H2HRecord(0, 0, 0));

        assertEquals(baseline.getHomeOdd(), adjusted.getHomeOdd());
        assertEquals(baseline.getDrawOdd(), adjusted.getDrawOdd());
        assertEquals(baseline.getAwayOdd(), adjusted.getAwayOdd());
    }

    @Test
    void h2hWeightShouldCapAtMax() {
        OddsResult tenMatches = oddsCalculator.calculate(bd(1200), bd(1000), new H2HRecord(6, 2, 2));
        OddsResult twentyMatches = oddsCalculator.calculate(bd(1200), bd(1000), new H2HRecord(12, 4, 4));

        assertEquals(tenMatches.getHomeOdd(), twentyMatches.getHomeOdd());
        assertEquals(tenMatches.getDrawOdd(), twentyMatches.getDrawOdd());
        assertEquals(tenMatches.getAwayOdd(), twentyMatches.getAwayOdd());
    }

    @Test
    void probabilitiesShouldSumToOneAfterNormalization() {
        OddsCalculatorServiceImpl.OddsProbabilities probabilities = oddsCalculator.calculateProbabilities(
                bd(1250), bd(1100), new H2HRecord(3, 4, 2));

        double total = probabilities.getHomeProbability()
                + probabilities.getDrawProbability()
                + probabilities.getAwayProbability();

        assertEquals(1.0d, total, 1.0e-9d);
    }

    @Test
    void allOddsShouldRespectMinOdd() {
        OddsResult result = oddsCalculator.calculate(bd(3000), bd(1000), null);
        BigDecimal minOdd = new BigDecimal("1.05");

        assertTrue(result.getHomeOdd().compareTo(minOdd) >= 0);
        assertTrue(result.getDrawOdd().compareTo(minOdd) >= 0);
        assertTrue(result.getAwayOdd().compareTo(minOdd) >= 0);
    }

    @Test
    void customEloScale_shouldProduceDifferentOddsThanDefault() {
        OddsProperties customProps = new OddsProperties();
        customProps.setDrawFactor(new BigDecimal("0.28"));
        customProps.setMaxH2hWeight(new BigDecimal("0.20"));
        customProps.setMinOdd(new BigDecimal("1.05"));
        customProps.setH2hMatchLimit(10);
        customProps.setEloScale(400);
        OddsCalculatorServiceImpl customCalculator = new OddsCalculatorServiceImpl(customProps);

        OddsResult defaultResult = oddsCalculator.calculate(bd(1200), bd(1000), null);
        OddsResult customResult = customCalculator.calculate(bd(1200), bd(1000), null);

        assertTrue(!defaultResult.getHomeOdd().equals(customResult.getHomeOdd()));
    }

    @Test
    void workedExampleFromSpec_shouldMatchExpectedOdds() {
        OddsProperties specProps = new OddsProperties();
        specProps.setDrawFactor(new BigDecimal("0.28"));
        specProps.setMaxH2hWeight(new BigDecimal("0.20"));
        specProps.setMinOdd(new BigDecimal("1.05"));
        specProps.setH2hMatchLimit(10);
        specProps.setEloScale(400);
        OddsCalculatorServiceImpl specCalculator = new OddsCalculatorServiceImpl(specProps);

        OddsResult result = specCalculator.calculate(bd(1100), bd(1000), new H2HRecord(4, 1, 1));

        assertEquals(new BigDecimal("1.77"), result.getHomeOdd());
        assertEquals(new BigDecimal("5.14"), result.getDrawOdd());
        assertEquals(new BigDecimal("4.16"), result.getAwayOdd());
    }

    @Test
    void awayFavored_shouldProduceHigherHomeOdd() {
        OddsResult result = oddsCalculator.calculate(bd(900), bd(1200), null);

        assertTrue(result.getHomeOdd().compareTo(result.getAwayOdd()) > 0);
        assertTrue(result.getAwayOdd().compareTo(new BigDecimal("1.05")) >= 0);
        assertTrue(result.getHomeOdd().compareTo(new BigDecimal("1.05")) >= 0);
    }

    private BigDecimal bd(int value) {
        return BigDecimal.valueOf(value);
    }
}

