package com.utagent.mutation;

public enum MutationQualityLevel {
    EXCELLENT("Excellent", 0.90),
    GOOD("Good", 0.80),
    MODERATE("Moderate", 0.60),
    POOR("Poor", 0.40),
    CRITICAL("Critical", 0.0);

    private final String label;
    private final double threshold;

    MutationQualityLevel(String label, double threshold) {
        this.label = label;
        this.threshold = threshold;
    }

    public String label() { return label; }
    public double threshold() { return threshold; }
}
