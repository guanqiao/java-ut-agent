package com.utagent.maintenance;

public enum FailureSeverity {
    CRITICAL(5),
    MAJOR(4),
    MODERATE(3),
    MINOR(2),
    INFO(1);

    private final int level;

    FailureSeverity(int level) {
        this.level = level;
    }

    public int level() { return level; }
}
