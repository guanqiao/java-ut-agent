package com.utagent.monitoring;

public enum GenerationPhase {
    INITIALIZING("Initializing", "🔧", 0),
    PARSING("Parsing source code", "📖", 1),
    FRAMEWORK_DETECTION("Detecting frameworks", "🔍", 2),
    TEST_GENERATION("Generating tests", "✨", 3),
    LLM_CALL("Calling LLM", "🤖", 4),
    WRITING_TEST("Writing test file", "📝", 5),
    RUNNING_TESTS("Running tests", "▶️", 6),
    COVERAGE_ANALYSIS("Analyzing coverage", "📊", 7),
    OPTIMIZATION("Optimizing coverage", "🔄", 8),
    COMPLETED("Completed", "✅", 9),
    FAILED("Failed", "❌", 10);

    private final String displayName;
    private final String icon;
    private final int order;

    GenerationPhase(String displayName, String icon, int order) {
        this.displayName = displayName;
        this.icon = icon;
        this.order = order;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    public int getOrder() {
        return order;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    public double getProgressPercentage() {
        return (double) order / (values().length - 1) * 100;
    }
}
