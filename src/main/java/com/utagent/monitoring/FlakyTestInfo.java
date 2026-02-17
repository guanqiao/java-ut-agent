package com.utagent.monitoring;

public class FlakyTestInfo {
    private final String testClass;
    private final String testName;
    private final double varianceCoefficient;

    public FlakyTestInfo(String testClass, String testName, double varianceCoefficient) {
        this.testClass = testClass;
        this.testName = testName;
        this.varianceCoefficient = varianceCoefficient;
    }

    public String getTestClass() {
        return testClass;
    }

    public String getTestName() {
        return testName;
    }

    public double getVarianceCoefficient() {
        return varianceCoefficient;
    }
}
