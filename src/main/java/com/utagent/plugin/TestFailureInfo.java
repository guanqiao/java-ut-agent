package com.utagent.plugin;

public class TestFailureInfo {
    private final String testClass;
    private final String testMethod;
    private final String failureMessage;
    private final int lineNumber;

    public TestFailureInfo(String testClass, String testMethod, String failureMessage, int lineNumber) {
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.failureMessage = failureMessage;
        this.lineNumber = lineNumber;
    }

    public String getTestClass() {
        return testClass;
    }

    public String getTestMethod() {
        return testMethod;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public boolean isAssertionFailure() {
        return failureMessage != null && 
            (failureMessage.contains("expected:") || failureMessage.contains("AssertionFailed"));
    }

    public boolean isNullPointer() {
        return failureMessage != null && failureMessage.contains("NullPointerException");
    }
}
