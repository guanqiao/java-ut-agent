package com.utagent.plugin;

public class QuickFixAction {
    private final String description;
    private final String fixedCode;
    private final int confidence;
    private final FixCategory category;

    public QuickFixAction(String description, String fixedCode, int confidence, FixCategory category) {
        this.description = description;
        this.fixedCode = fixedCode;
        this.confidence = confidence;
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public String getFixedCode() {
        return fixedCode;
    }

    public int getConfidence() {
        return confidence;
    }

    public FixCategory getCategory() {
        return category;
    }
}
