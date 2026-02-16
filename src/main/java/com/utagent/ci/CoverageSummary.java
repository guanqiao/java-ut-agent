package com.utagent.ci;

public record CoverageSummary(
    double lineCoverage,
    double branchCoverage,
    double instructionCoverage,
    double methodCoverage
) {
    public double getOverallCoverage() {
        return (lineCoverage + branchCoverage + instructionCoverage + methodCoverage) / 4;
    }

    public String getLineCoveragePercent() {
        return formatPercent(lineCoverage);
    }

    public String getBranchCoveragePercent() {
        return formatPercent(branchCoverage);
    }

    private String formatPercent(double value) {
        return String.format("%.0f%%", value * 100);
    }
}
