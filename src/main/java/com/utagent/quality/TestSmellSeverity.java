package com.utagent.quality;

public enum TestSmellSeverity {
    CRITICAL(5),
    HIGH(4),
    MEDIUM(3),
    LOW(2),
    INFO(1);

    private final int level;

    TestSmellSeverity(int level) {
        this.level = level;
    }

    public int level() { return level; }
}
