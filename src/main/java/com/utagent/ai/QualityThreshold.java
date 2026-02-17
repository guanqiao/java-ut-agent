package com.utagent.ai;

public class QualityThreshold {
    private final double minCoverage;
    private final double minReadability;

    public QualityThreshold(double minCoverage, double minReadability) {
        this.minCoverage = minCoverage;
        this.minReadability = minReadability;
    }

    public double getMinCoverage() {
        return minCoverage;
    }

    public double getMinReadability() {
        return minReadability;
    }

    public boolean isMet(double coverage, double readability) {
        return coverage >= minCoverage && readability >= minReadability;
    }
}
