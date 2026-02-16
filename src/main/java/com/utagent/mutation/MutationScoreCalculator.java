package com.utagent.mutation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MutationScoreCalculator {

    public double calculate(MutationReport report) {
        if (report == null || report.getTotalMutants() == 0) {
            return 0.0;
        }
        return (double) report.getKilledMutants() / report.getTotalMutants();
    }

    public double calculateAverage(List<MutationReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return 0.0;
        }

        int totalMutants = 0;
        int totalKilled = 0;

        for (MutationReport report : reports) {
            totalMutants += report.getTotalMutants();
            totalKilled += report.getKilledMutants();
        }

        if (totalMutants == 0) {
            return 0.0;
        }

        return (double) totalKilled / totalMutants;
    }

    public double calculateWeighted(List<MutationReport> reports) {
        return calculateAverage(reports);
    }

    public Map<String, Double> calculateByMutator(MutationReport report) {
        if (report == null || report.getMutants().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> totalByMutator = new HashMap<>();
        Map<String, Integer> killedByMutator = new HashMap<>();

        for (Mutant mutant : report.getMutants()) {
            String mutator = extractMutatorName(mutant.mutator());
            totalByMutator.merge(mutator, 1, Integer::sum);
            if (mutant.isKilled()) {
                killedByMutator.merge(mutator, 1, Integer::sum);
            }
        }

        Map<String, Double> scores = new HashMap<>();
        for (String mutator : totalByMutator.keySet()) {
            int total = totalByMutator.get(mutator);
            int killed = killedByMutator.getOrDefault(mutator, 0);
            scores.put(mutator, (double) killed / total);
        }

        return scores;
    }

    public Map<String, Double> calculateByMethod(MutationReport report) {
        if (report == null || report.getMutants().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> totalByMethod = new HashMap<>();
        Map<String, Integer> killedByMethod = new HashMap<>();

        for (Mutant mutant : report.getMutants()) {
            String method = mutant.method();
            totalByMethod.merge(method, 1, Integer::sum);
            if (mutant.isKilled()) {
                killedByMethod.merge(method, 1, Integer::sum);
            }
        }

        Map<String, Double> scores = new HashMap<>();
        for (String method : totalByMethod.keySet()) {
            int total = totalByMethod.get(method);
            int killed = killedByMethod.getOrDefault(method, 0);
            scores.put(method, (double) killed / total);
        }

        return scores;
    }

    public List<String> identifyWeakAreas(MutationReport report, double threshold) {
        if (report == null || report.getMutants().isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Double> scoresByMethod = calculateByMethod(report);
        List<String> weakAreas = new ArrayList<>();

        for (Map.Entry<String, Double> entry : scoresByMethod.entrySet()) {
            if (entry.getValue() < threshold) {
                weakAreas.add(entry.getKey());
            }
        }

        return weakAreas;
    }

    public double calculateImprovementPotential(MutationReport report) {
        if (report == null || report.getTotalMutants() == 0) {
            return 0.0;
        }
        return (double) report.getSurvivedMutants() / report.getTotalMutants();
    }

    public double calculateTrend(List<MutationReport> reports) {
        if (reports == null || reports.size() < 2) {
            return 0.0;
        }

        double firstScore = calculate(reports.get(0));
        double lastScore = calculate(reports.get(reports.size() - 1));

        return lastScore - firstScore;
    }

    public String formatAsPercentage(double score) {
        return String.format("%.2f%%", score * 100);
    }

    public MutationQualityLevel determineQualityLevel(double score) {
        if (score >= 0.90) {
            return MutationQualityLevel.EXCELLENT;
        } else if (score >= 0.80) {
            return MutationQualityLevel.GOOD;
        } else if (score >= 0.60) {
            return MutationQualityLevel.MODERATE;
        } else if (score >= 0.40) {
            return MutationQualityLevel.POOR;
        } else {
            return MutationQualityLevel.CRITICAL;
        }
    }

    private String extractMutatorName(String fullMutatorName) {
        if (fullMutatorName == null || fullMutatorName.isEmpty()) {
            return "Unknown";
        }
        int lastDot = fullMutatorName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < fullMutatorName.length() - 1) {
            return fullMutatorName.substring(lastDot + 1);
        }
        return fullMutatorName;
    }
}
