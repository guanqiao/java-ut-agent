package com.utagent.quality;

import java.util.List;

public record QualityReport(
    double overallScore,
    QualityGrade grade,
    double lineCoverage,
    double branchCoverage,
    double mutationScore,
    double readabilityScore,
    List<String> recommendations
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private double overallScore;
        private QualityGrade grade;
        private double lineCoverage;
        private double branchCoverage;
        private double mutationScore;
        private double readabilityScore;
        private List<String> recommendations = List.of();

        public Builder overallScore(double overallScore) {
            this.overallScore = overallScore;
            return this;
        }

        public Builder grade(QualityGrade grade) {
            this.grade = grade;
            return this;
        }

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

        public Builder recommendations(List<String> recommendations) {
            this.recommendations = recommendations != null ? List.copyOf(recommendations) : List.of();
            return this;
        }

        public QualityReport build() {
            return new QualityReport(
                overallScore, grade, lineCoverage, branchCoverage,
                mutationScore, readabilityScore, recommendations
            );
        }
    }
}
