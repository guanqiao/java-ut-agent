package com.utagent.monitoring;

import java.time.Duration;
import java.time.Instant;

public class PhaseTiming {

    private final GenerationPhase phase;
    private final Instant startTime;
    private Instant endTime;
    private long durationMs;
    private boolean completed;

    public PhaseTiming(GenerationPhase phase) {
        this.phase = phase;
        this.startTime = Instant.now();
        this.completed = false;
    }

    public void complete() {
        this.endTime = Instant.now();
        this.durationMs = Duration.between(startTime, endTime).toMillis();
        this.completed = true;
    }

    public GenerationPhase getPhase() {
        return phase;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public long getDurationMs() {
        if (completed) {
            return durationMs;
        }
        return Duration.between(startTime, Instant.now()).toMillis();
    }

    public double getDurationSeconds() {
        return getDurationMs() / 1000.0;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getFormattedDuration() {
        long ms = getDurationMs();
        if (ms < 1000) {
            return ms + "ms";
        } else if (ms < 60000) {
            return String.format("%.1fs", ms / 1000.0);
        } else {
            long minutes = ms / 60000;
            long seconds = (ms % 60000) / 1000;
            return String.format("%dm %ds", minutes, seconds);
        }
    }

    public double getPercentageOfTotal(long totalMs) {
        if (totalMs <= 0) return 0;
        return (double) getDurationMs() / totalMs * 100;
    }

    @Override
    public String toString() {
        return String.format("%s: %s", phase.getDisplayName(), getFormattedDuration());
    }
}
