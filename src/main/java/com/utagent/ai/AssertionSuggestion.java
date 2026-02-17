package com.utagent.ai;

public class AssertionSuggestion {
    private final String originalAssertion;
    private final String suggestedAssertion;
    private final String reason;

    public AssertionSuggestion(String originalAssertion, String suggestedAssertion, String reason) {
        this.originalAssertion = originalAssertion;
        this.suggestedAssertion = suggestedAssertion;
        this.reason = reason;
    }

    public String getOriginalAssertion() {
        return originalAssertion;
    }

    public String getSuggestedAssertion() {
        return suggestedAssertion;
    }

    public String getReason() {
        return reason;
    }
}
