package com.utagent.quality;

import java.util.Collections;
import java.util.List;

public final class TestQualityMetrics {
    private final double lineCoverage;
    private final double branchCoverage;
    private final double mutationScore;
    private final double readabilityScore;
    private final double assertionDensity;
    private final double maintainabilityScore;

    private TestQualityMetrics(Builder builder) {
        this.lineCoverage = builder.lineCoverage;
        this.branchCoverage = builder.branchCoverage;
        this.mutationScore = builder.mutationScore;
        this.readabilityScore = builder.readabilityScore;
        this.assertionDensity = builder.assertionDensity;
        this.maintainabilityScore = builder.maintainabilityScore;
    }

    public double lineCoverage() { return lineCoverage; }
    public double branchCoverage() { return branchCoverage; }
    public double mutationScore() { return mutationScore; }
    public double readabilityScore() { return readabilityScore; }
    public double assertionDensity() { return assertionDensity; }
    public double maintainabilityScore() { return maintainabilityScore; }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private double lineCoverage;
        private double branchCoverage;
        private double mutationScore;
        private double readabilityScore;
        private double assertionDensity;
        private double maintainabilityScore;

        public Builder lineCoverage(double lineCoverage) {
            this.lineCoverage = lineCoverage;
            return this;
        }

        public Builder branchCoverage(double branchCoverage) {
            this.branchCoverage = branchCoverage;
            return this;
        }

        public Builder mutationScore(double mutationScore) {
            this.mutationScore = mutationScore;
            return this;
        }

        public Builder readabilityScore(double readabilityScore) {
            this.readabilityScore = readabilityScore;
            return this;
        }

        public Builder assertionDensity(double assertionDensity) {
            this.assertionDensity = assertionDensity;
            return this;
        }

        public Builder maintainabilityScore(double maintainabilityScore) {
            this.maintainabilityScore = maintainabilityScore;
            return this;
        }

        public TestQualityMetrics build() {
            return new TestQualityMetrics(this);
        }
    }
}
