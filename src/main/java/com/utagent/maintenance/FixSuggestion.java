package com.utagent.maintenance;

public class FixSuggestion {

    private final String description;
    private final String suggestedCode;
    private final double confidence;
    private final FixType fixType;

    public FixSuggestion(String description, String suggestedCode, double confidence, FixType fixType) {
        this.description = description;
        this.suggestedCode = suggestedCode;
        this.confidence = confidence;
        this.fixType = fixType;
    }

    public String getDescription() {
        return description;
    }

    public String getSuggestedCode() {
        return suggestedCode;
    }

    public double getConfidence() {
        return confidence;
    }

    public FixType getFixType() {
        return fixType;
    }
}
