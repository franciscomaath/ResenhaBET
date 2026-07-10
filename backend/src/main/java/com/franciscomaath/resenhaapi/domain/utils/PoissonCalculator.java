package com.franciscomaath.resenhaapi.domain.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PoissonCalculator {

    private static final int MAX_GOALS = 9;

    private PoissonCalculator() {
    }

    public static double probability(int k, double lambda) {
        if (k < 0 || lambda <= 0) {
            return 0.0d;
        }
        return Math.pow(lambda, k) * Math.exp(-lambda) / factorial(k);
    }

    public static double[][] scoreMatrix(double lambdaHome, double lambdaAway) {
        double[][] matrix = new double[MAX_GOALS + 1][MAX_GOALS + 1];
        for (int home = 0; home <= MAX_GOALS; home++) {
            for (int away = 0; away <= MAX_GOALS; away++) {
                matrix[home][away] = probability(home, lambdaHome) * probability(away, lambdaAway);
            }
        }
        return matrix;
    }

    public static double over(double[][] matrix, int threshold) {
        double probability = 0.0d;
        for (int home = 0; home <= MAX_GOALS; home++) {
            for (int away = 0; away <= MAX_GOALS; away++) {
                if (home + away >= threshold) {
                    probability += matrix[home][away];
                }
            }
        }
        return probability;
    }

    public static double bttsYes(double lambdaHome, double lambdaAway, double[][] matrix) {
        return 1.0d - probability(0, lambdaHome) - probability(0, lambdaAway) + matrix[0][0];
    }

    public static List<ScoreEntry> topScores(double[][] matrix, int topN) {
        List<ScoreEntry> all = new ArrayList<>();
        for (int home = 0; home <= MAX_GOALS; home++) {
            for (int away = 0; away <= MAX_GOALS; away++) {
                all.add(new ScoreEntry(home + "-" + away, matrix[home][away]));
            }
        }

        all.sort(Comparator.comparingDouble(ScoreEntry::probability).reversed());

        List<ScoreEntry> top = new ArrayList<>(all.subList(0, Math.min(topN, all.size())));
        double residual = Math.max(0.0d, 1.0d - top.stream().mapToDouble(ScoreEntry::probability).sum());
        top.add(new ScoreEntry("Outro Placar", residual));
        return top;
    }

    private static double factorial(int n) {
        double result = 1.0d;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }

    public record ScoreEntry(String label, double probability) {
    }
}
