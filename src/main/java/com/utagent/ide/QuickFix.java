package com.utagent.ide;

public class QuickFix {
    private final String description;
    private final String fixedCode;
    private final FixType fixType;
    private final int confidence;

    public QuickFix(String description, String fixedCode, FixType fixType, int confidence) {
        this.description = description;
        this.fixedCode = fixedCode;
        this.fixType = fixType;
        this.confidence = confidence;
    }

    public String getDescription() {
        return description;
    }

    public String getFixedCode() {
        return fixedCode;
    }

    public FixType getFixType() {
        return fixType;
    }

    public int getConfidence() {
        return confidence;
    }
}
