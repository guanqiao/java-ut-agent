package com.utagent.maintenance;

public enum FailureType {
    COMPILATION_ERROR("Compilation Error"),
    ASSERTION_FAILURE("Assertion Failure"),
    RUNTIME_ERROR("Runtime Error"),
    TIMEOUT("Timeout"),
    DEPENDENCY_ERROR("Dependency Error"),
    CONFIGURATION_ERROR("Configuration Error");

    private final String displayName;

    FailureType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() { return displayName; }
}
