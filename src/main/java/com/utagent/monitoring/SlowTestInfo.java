package com.utagent.monitoring;

public class SlowTestInfo {
    private final String testClass;
    private final String testName;
    private final long executionTime;

    public SlowTestInfo(String testClass, String testName, long executionTime) {
        this.testClass = testClass;
        this.testName = testName;
        this.executionTime = executionTime;
    }

    public String getTestClass() {
        return testClass;
    }

    public String getTestName() {
        return testName;
    }

    public long getExecutionTime() {
        return executionTime;
    }
}
