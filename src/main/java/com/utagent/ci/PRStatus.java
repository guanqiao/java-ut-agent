package com.utagent.ci;

public record PRStatus(
    boolean success,
    String message,
    double coverage,
    double qualityScore
) {
    public String getStatusEmoji() {
        return success ? "✅" : "❌";
    }

    public String getStatusText() {
        return success ? "passed" : "failed";
    }
}
