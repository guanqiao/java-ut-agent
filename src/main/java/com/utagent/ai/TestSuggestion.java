package com.utagent.ai;

public class TestSuggestion {
    private final String testName;
    private final String description;
    private final String suggestedCode;
    private final int priority;

    public TestSuggestion(String testName, String description, String suggestedCode, int priority) {
        this.testName = testName;
        this.description = description;
        this.suggestedCode = suggestedCode;
        this.priority = priority;
    }

    public String getTestName() {
        return testName;
    }

    public String getDescription() {
        return description;
    }

    public String getSuggestedCode() {
        return suggestedCode;
    }

    public int getPriority() {
        return priority;
    }
}
