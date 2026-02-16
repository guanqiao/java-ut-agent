package com.utagent.maintenance;

public record TestExecution(
    String testName,
    boolean passed,
    long durationMs
) {}
