package com.utagent.maintenance;

public record TestFailure(
    String testClass,
    String testMethod,
    String errorMessage,
    String stackTrace,
    FailureType type,
    int severity
) {
    public TestFailure(String testClass, String testMethod, String errorMessage, String stackTrace) {
        this(testClass, testMethod, errorMessage, stackTrace, inferType(errorMessage), inferSeverity(errorMessage));
    }

    public String message() {
        return errorMessage;
    }

    public boolean isAssertionFailure() {
        return errorMessage != null && 
            (errorMessage.contains("AssertionFailed") || 
             errorMessage.contains("AssertionError") ||
             errorMessage.contains("expected:"));
    }

    public boolean isNullPointerException() {
        return errorMessage != null && errorMessage.contains("NullPointerException");
    }

    public boolean isMockException() {
        return errorMessage != null && 
            (errorMessage.contains("Mockito") || 
             errorMessage.contains("mock") ||
             errorMessage.contains("stubbing"));
    }

    public boolean isTimeoutException() {
        return errorMessage != null && errorMessage.contains("Timeout");
    }

    private static FailureType inferType(String errorMessage) {
        if (errorMessage == null) {
            return FailureType.UNKNOWN;
        }
        if (errorMessage.contains("AssertionFailed") || 
            errorMessage.contains("AssertionError") ||
            errorMessage.contains("expected:")) {
            return FailureType.ASSERTION_FAILURE;
        }
        if (errorMessage.contains("NullPointerException")) {
            return FailureType.NULL_POINTER;
        }
        if (errorMessage.contains("Mockito") || 
            errorMessage.contains("mock") ||
            errorMessage.contains("stubbing")) {
            return FailureType.MOCK_CONFIGURATION;
        }
        if (errorMessage.contains("Timeout")) {
            return FailureType.TIMEOUT;
        }
        if (errorMessage.contains("Compilation") || errorMessage.contains("cannot find symbol")) {
            return FailureType.COMPILATION_ERROR;
        }
        return FailureType.RUNTIME_ERROR;
    }

    private static int inferSeverity(String errorMessage) {
        if (errorMessage == null) {
            return 0;
        }
        if (errorMessage.contains("OutOfMemory") || errorMessage.contains("StackOverflow")) {
            return 5;
        }
        if (errorMessage.contains("NullPointerException")) {
            return 3;
        }
        if (errorMessage.contains("AssertionFailed")) {
            return 1;
        }
        return 2;
    }
}
