package com.utagent.monitoring;

public class PerformanceAlert {
    private final String testClass;
    private final String testName;
    private final long baselineTime;
    private final long actualTime;
    private final double threshold;

    public PerformanceAlert(String testClass, String testName, long baselineTime, long actualTime, double threshold) {
        this.testClass = testClass;
        this.testName = testName;
        this.baselineTime = baselineTime;
        this.actualTime = actualTime;
        this.threshold = threshold;
    }

    public String getTestClass() {
        return testClass;
    }

    public String getTestName() {
        return testName;
    }

    public long getBaselineTime() {
        return baselineTime;
    }

    public long getActualTime() {
        return actualTime;
    }

    public double getThreshold() {
        return threshold;
    }

    public double getRegressionRatio() {
        return (double) actualTime / baselineTime;
    }
}
