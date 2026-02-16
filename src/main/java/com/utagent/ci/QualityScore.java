package com.utagent.ci;

import java.util.Map;

public record QualityScore(
    double overallScore,
    Map<String, Double> dimensionScores
) {
    public String getGrade() {
        if (overallScore >= 90) return "A";
        if (overallScore >= 80) return "B";
        if (overallScore >= 70) return "C";
        if (overallScore >= 60) return "D";
        return "F";
    }

    public boolean isPassing() {
        return overallScore >= 60;
    }
}
