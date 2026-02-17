package com.utagent.report;

import java.util.Map;

public class CoverageData {
    private final Map<String, Double> metrics;
    private final double overall;

    public CoverageData(Map<String, Double> metrics, double overall) {
        this.metrics = metrics;
        this.overall = overall;
    }

    public Map<String, Double> getMetrics() {
        return metrics;
    }

    public double getOverall() {
        return overall;
    }

    public String getOverallPercent() {
        return String.format("%.0f%%", overall * 100);
    }
}
