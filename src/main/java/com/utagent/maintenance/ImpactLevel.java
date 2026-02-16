package com.utagent.maintenance;

public enum ImpactLevel {
    HIGH("High", 3),
    MEDIUM("Medium", 2),
    LOW("Low", 1),
    NONE("None", 0);

    private final String label;
    private final int priority;

    ImpactLevel(String label, int priority) {
        this.label = label;
        this.priority = priority;
    }

    public String label() { return label; }
    public int priority() { return priority; }
}
