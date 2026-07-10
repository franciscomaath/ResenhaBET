package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.OddsProperties;
import com.franciscomaath.resenhaapi.service.OddsCalculatorService;
import com.franciscomaath.resenhaapi.service.dto.H2HRecord;
import com.franciscomaath.resenhaapi.service.dto.OddsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class OddsCalculatorServiceImpl implements OddsCalculatorService {
    private static final int ODDS_SCALE = 2;

    private final OddsProperties oddsProperties;

    @Override
    public OddsResult calculate(BigDecimal eloHome, BigDecimal eloAway, H2HRecord h2hRecord) {
        OddsProbabilities probabilities = calculateProbabilities(eloHome, eloAway, h2hRecord);
        BigDecimal minOdd = oddsProperties.getMinOdd().setScale(ODDS_SCALE, RoundingMode.HALF_UP);

        BigDecimal homeOdd = toOdd(probabilities.homeProbability, minOdd);
        BigDecimal drawOdd = toOdd(probabilities.drawProbability, minOdd);
        BigDecimal awayOdd = toOdd(probabilities.awayProbability, minOdd);

        log.info("Calculated odds - Home: {}, Draw: {}, Away: {} for Elos - Home: {}, Away: {} with H2H - {}",
                homeOdd, drawOdd, awayOdd, eloHome, eloAway, h2hRecord);
        return new OddsResult(homeOdd, drawOdd, awayOdd, probabilities.homeProbability, probabilities.awayProbability);
    }

    @Override
    public OddsResult calculateNoDraw(BigDecimal eloHome, BigDecimal eloAway, H2HRecord h2hRecord) {
        double eloScale = oddsProperties.getEloScale();
        double homeBase = 1.0d / (1.0d + Math.pow(10.0d, (eloAway.subtract(eloHome)).doubleValue() / eloScale));
        double awayBase = 1.0d - homeBase;

        double homeFinal;
        double awayFinal;

        if (h2hRecord == null || h2hRecord.getTotalMatches() == 0) {
            homeFinal = homeBase;
            awayFinal = awayBase;
        } else {
            int totalMatches = h2hRecord.getTotalMatches();
            double h2hWeight = Math.min(totalMatches / 10.0d, oddsProperties.getMaxH2hWeight().doubleValue());
            double eloWeight = 1.0d - h2hWeight;

            double homeH2h = h2hRecord.getHomeWins() / (double) totalMatches;
            double awayH2h = h2hRecord.getAwayWins() / (double) totalMatches;

            homeFinal = (homeBase * eloWeight) + (homeH2h * h2hWeight);
            awayFinal = (awayBase * eloWeight) + (awayH2h * h2hWeight);
        }

        // Normalize to sum to 1 (no draw)
        double total = homeFinal + awayFinal;
        homeFinal = homeFinal / total;
        awayFinal = awayFinal / total;

        BigDecimal minOdd = oddsProperties.getMinOdd().setScale(ODDS_SCALE, RoundingMode.HALF_UP);
        BigDecimal homeOdd = toOdd(homeFinal, minOdd);
        BigDecimal awayOdd = toOdd(awayFinal, minOdd);

        log.info("Calculated no-draw odds - Home: {}, Away: {} for Elos - Home: {}, Away: {} with H2H - {}",
                homeOdd, awayOdd, eloHome, eloAway, h2hRecord);
        return new OddsResult(homeOdd, BigDecimal.ZERO, awayOdd, homeFinal, awayFinal);
    }

    OddsProbabilities calculateProbabilities(BigDecimal eloHome, BigDecimal eloAway, H2HRecord h2hRecord) {
        double eloScale = oddsProperties.getEloScale();
        double homeBase = 1.0d / (1.0d + Math.pow(10.0d, (eloAway.subtract(eloHome)).doubleValue() / eloScale));
        double awayBase = 1.0d - homeBase;

        double drawFactor = oddsProperties.getDrawFactor().doubleValue();
        double drawProbability = drawFactor * (1.0d - Math.abs(homeBase - awayBase));
        double homeElo = homeBase - (drawProbability / 2.0d);
        double awayElo = awayBase - (drawProbability / 2.0d);

        if (h2hRecord == null || h2hRecord.getTotalMatches() == 0) {
            return normalize(homeElo, drawProbability, awayElo);
        }

        int totalMatches = h2hRecord.getTotalMatches();
        double h2hWeight = Math.min(totalMatches / 10.0d, oddsProperties.getMaxH2hWeight().doubleValue());
        double eloWeight = 1.0d - h2hWeight;

        double homeH2h = h2hRecord.getHomeWins() / (double) totalMatches;
        double drawH2h = h2hRecord.getDraws() / (double) totalMatches;
        double awayH2h = h2hRecord.getAwayWins() / (double) totalMatches;

        double homeFinal = (homeElo * eloWeight) + (homeH2h * h2hWeight);
        double drawFinal = (drawProbability * eloWeight) + (drawH2h * h2hWeight);
        double awayFinal = (awayElo * eloWeight) + (awayH2h * h2hWeight);

        return normalize(homeFinal, drawFinal, awayFinal);
    }

    private OddsProbabilities normalize(double home, double draw, double away) {
        double total = home + draw + away;
        return new OddsProbabilities(home / total, draw / total, away / total);
    }

    private BigDecimal toOdd(double probability, BigDecimal minOdd) {
        BigDecimal odd = BigDecimal.valueOf(1.0d / probability).setScale(ODDS_SCALE, RoundingMode.HALF_UP);
        return odd.compareTo(minOdd) < 0 ? minOdd : odd;
    }

    static class OddsProbabilities {
        private final double homeProbability;
        private final double drawProbability;
        private final double awayProbability;

        OddsProbabilities(double homeProbability, double drawProbability, double awayProbability) {
            this.homeProbability = homeProbability;
            this.drawProbability = drawProbability;
            this.awayProbability = awayProbability;
        }

        double getHomeProbability() {
            return homeProbability;
        }

        double getDrawProbability() {
            return drawProbability;
        }

        double getAwayProbability() {
            return awayProbability;
        }
    }
}
