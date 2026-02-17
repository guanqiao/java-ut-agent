package com.utagent.service;

public record GenerationResult(
    boolean success,
    String testCode,
    int iterations,
    double coverage,
    String errorMessage
) {
    public static GenerationResult success(String testCode, int iterations, double coverage) {
        return new GenerationResult(true, testCode, iterations, coverage, null);
    }

    public static GenerationResult failure(String errorMessage) {
        return new GenerationResult(false, null, 0, 0.0, errorMessage);
    }
}
