package com.utagent.plugin;

public class GenerationResult {
    private final boolean success;
    private final String generatedCode;
    private final String testFilePath;
    private final String errorMessage;
    private final double coverageEstimate;

    public GenerationResult(boolean success, String generatedCode, String testFilePath, 
                           String errorMessage, double coverageEstimate) {
        this.success = success;
        this.generatedCode = generatedCode;
        this.testFilePath = testFilePath;
        this.errorMessage = errorMessage;
        this.coverageEstimate = coverageEstimate;
    }

    public static GenerationResult success(String code, String testFilePath, double coverage) {
        return new GenerationResult(true, code, testFilePath, null, coverage);
    }

    public static GenerationResult failure(String errorMessage) {
        return new GenerationResult(false, null, null, errorMessage, 0.0);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getGeneratedCode() {
        return generatedCode;
    }

    public String getTestFilePath() {
        return testFilePath;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public double getCoverageEstimate() {
        return coverageEstimate;
    }
}
