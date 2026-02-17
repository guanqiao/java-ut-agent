package com.utagent.monitoring;

public class PerformanceTrend {
    private final TrendDirection direction;
    private final double slope;
    private final double confidence;

    public PerformanceTrend(TrendDirection direction, double slope, double confidence) {
        this.direction = direction;
        this.slope = slope;
        this.confidence = confidence;
    }

    public TrendDirection getDirection() {
        return direction;
    }

    public double getSlope() {
        return slope;
    }

    public double getConfidence() {
        return confidence;
    }
}
