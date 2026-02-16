package com.utagent.quality;

public enum QualityGrade {
    A("Excellent", 0.90),
    B("Good", 0.80),
    C("Satisfactory", 0.65),
    D("Needs Improvement", 0.50),
    F("Poor", 0.0);

    private final String label;
    private final double threshold;

    QualityGrade(String label, double threshold) {
        this.label = label;
        this.threshold = threshold;
    }

    public String label() { return label; }
    public double threshold() { return threshold; }
}
