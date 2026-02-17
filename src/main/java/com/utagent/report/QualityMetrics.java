package com.utagent.report;

public class QualityMetrics {
    private final double coverage;
    private final double mutation;
    private final double maintainability;
    private final double readability;
    private final double performance;

    public QualityMetrics(double coverage, double mutation, double maintainability, 
                         double readability, double performance) {
        this.coverage = coverage;
        this.mutation = mutation;
        this.maintainability = maintainability;
        this.readability = readability;
        this.performance = performance;
    }

    public double getCoverage() {
        return coverage;
    }

    public double getMutation() {
        return mutation;
    }

    public double getMaintainability() {
        return maintainability;
    }

    public double getReadability() {
        return readability;
    }

    public double getPerformance() {
        return performance;
    }

    public double getOverall() {
        return (coverage + mutation + maintainability + readability + performance) / 5;
    }
}
