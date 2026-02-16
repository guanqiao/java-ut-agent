package com.utagent.maintenance;

public enum FailureType {
    ASSERTION_FAILURE("Assertion Failure", 1),
    NULL_POINTER("Null Pointer", 3),
    MOCK_CONFIGURATION("Mock Configuration", 2),
    TIMEOUT("Timeout", 1),
    COMPILATION_ERROR("Compilation Error", 4),
    RUNTIME_ERROR("Runtime Error", 2),
    DEPENDENCY_MISSING("Dependency Missing", 3),
    UNKNOWN("Unknown", 0);

    private final String displayName;
    private final int level;

    FailureType(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    public String displayName() {
        return displayName;
    }

    public int level() {
        return level;
    }
}
