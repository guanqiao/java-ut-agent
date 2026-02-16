package com.utagent.plugin;

public class CoverageInfo {
    private final double lineCoverage;
    private final double branchCoverage;
    private final int coveredLines;
    private final int totalLines;

    public CoverageInfo(double lineCoverage, double branchCoverage, int coveredLines, int totalLines) {
        this.lineCoverage = lineCoverage;
        this.branchCoverage = branchCoverage;
        this.coveredLines = coveredLines;
        this.totalLines = totalLines;
    }

    public static CoverageInfo empty() {
        return new CoverageInfo(0.0, 0.0, 0, 0);
    }

    public double getLineCoverage() {
        return lineCoverage;
    }

    public double getBranchCoverage() {
        return branchCoverage;
    }

    public int getCoveredLines() {
        return coveredLines;
    }

    public int getTotalLines() {
        return totalLines;
    }

    public String getLineCoveragePercent() {
        return String.format("%.0f%%", lineCoverage * 100);
    }
}
