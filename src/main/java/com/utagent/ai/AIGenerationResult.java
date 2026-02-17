package com.utagent.ai;

public class AIGenerationResult {
    private final String generatedCode;
    private final TestType testType;
    private final double qualityScore;
    private final int iterations;
    private final String explanation;

    public AIGenerationResult(String generatedCode, TestType testType, double qualityScore, 
                              int iterations, String explanation) {
        this.generatedCode = generatedCode;
        this.testType = testType;
        this.qualityScore = qualityScore;
        this.iterations = iterations;
        this.explanation = explanation;
    }

    public static AIGenerationResult of(String code, TestType type) {
        return new AIGenerationResult(code, type, 0.8, 1, "");
    }

    public String getGeneratedCode() {
        return generatedCode;
    }

    public TestType getTestType() {
        return testType;
    }

    public double getQualityScore() {
        return qualityScore;
    }

    public int getIterations() {
        return iterations;
    }

    public String getExplanation() {
        return explanation;
    }
}
