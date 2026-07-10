package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.utils.PoissonCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class GoalMarketsOddsCalculator {

    private final EventRepository eventRepository;

    @Value("${resenhabet.odds.avg-goals-per-side:2.0}")
    private double avgGoalsPerSide;

    @Value("${resenhabet.odds.elo-scale:400}")
    private double eloScale;

    @Value("${resenhabet.odds.elo-lambda-alpha:0.40}")
    private double eloLambdaAlpha;

    @Value("${resenhabet.odds.hist-lambda-threshold:10}")
    private int histLambdaThreshold;

    @Value("${resenhabet.odds.max-hist-lambda-weight:0.40}")
    private double maxHistLambdaWeight;

    @Value("${resenhabet.odds.min-odd:1.05}")
    private double minOdd;

    @Value("${resenhabet.odds.exact-score-top-n:8}")
    private int exactScoreTopN;

    public GoalMarketsOdds calculate(Player home, Player away) {
        double lambdaHome = computeBlendedLambda(home, away);
        double lambdaAway = computeBlendedLambda(away, home);

        double[][] matrix = PoissonCalculator.scoreMatrix(lambdaHome, lambdaAway);

        double pOver25 = PoissonCalculator.over(matrix, 3);
        double pOver35 = PoissonCalculator.over(matrix, 4);
        double pBtts = PoissonCalculator.bttsYes(lambdaHome, lambdaAway, matrix);

        List<PoissonCalculator.ScoreEntry> exactScores = PoissonCalculator.topScores(matrix, exactScoreTopN);
        Map<String, BigDecimal> exactScoreOdds = exactScores.stream().collect(Collectors.toMap(
                PoissonCalculator.ScoreEntry::label,
                entry -> toOdd(entry.probability()),
                (left, right) -> left,
                LinkedHashMap::new
        ));

        return new GoalMarketsOdds(
                toOdd(pOver25), toOdd(1.0d - pOver25),
                toOdd(pOver35), toOdd(1.0d - pOver35),
                toOdd(pBtts), toOdd(1.0d - pBtts),
                exactScoreOdds
        );
    }

    private double computeBlendedLambda(Player self, Player opponent) {
        double ratio = Math.pow(10.0d, (self.getCurrentElo().doubleValue() - opponent.getCurrentElo().doubleValue()) / (2.0d * eloScale));
        double lambdaElo = avgGoalsPerSide * Math.pow(ratio, eloLambdaAlpha);

        long selfMatches = eventRepository.countCompletedMatchesByPlayer(self.getId());
        long opponentMatches = eventRepository.countCompletedMatchesByPlayer(opponent.getId());
        long sampleSize = Math.min(selfMatches, opponentMatches);

        double weight = Math.min((double) sampleSize / histLambdaThreshold, maxHistLambdaWeight);
        if (weight <= 0.0d) {
            return lambdaElo;
        }

        double leagueAvg = Optional.ofNullable(eventRepository.findGlobalAvgGoalsPerSide())
                .orElse(avgGoalsPerSide);
        double attack = Optional.ofNullable(eventRepository.findAvgGoalsScoredByPlayer(self.getId()))
                .map(value -> value / leagueAvg)
                .orElse(1.0d);
        double defense = Optional.ofNullable(eventRepository.findAvgGoalsConcededByPlayer(opponent.getId()))
                .map(value -> value / leagueAvg)
                .orElse(1.0d);
        double lambdaHist = attack * defense * leagueAvg;

        return (1.0d - weight) * lambdaElo + weight * lambdaHist;
    }

    private BigDecimal toOdd(double probability) {
        if (probability <= 0.0d) {
            return BigDecimal.valueOf(minOdd);
        }
        return BigDecimal.valueOf(Math.max(1.0d / probability, minOdd));
    }

    public record GoalMarketsOdds(
            BigDecimal over25,
            BigDecimal under25,
            BigDecimal over35,
            BigDecimal under35,
            BigDecimal bttsYes,
            BigDecimal bttsNo,
            Map<String, BigDecimal> exactScoreOdds
    ) {
    }
}
