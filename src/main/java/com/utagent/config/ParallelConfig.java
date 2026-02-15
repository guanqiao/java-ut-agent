package com.utagent.config;

/**
 * Configuration for parallel test generation.
 */
public record ParallelConfig(
    Boolean enabled,
    Integer threadPoolSize,
    Integer queueCapacity,
    Long timeoutSeconds
) {
    public static final boolean DEFAULT_ENABLED = true;
    public static final int DEFAULT_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    public static final int DEFAULT_QUEUE_CAPACITY = 100;
    public static final long DEFAULT_TIMEOUT_SECONDS = 300;

    public static ParallelConfig defaults() {
        return new ParallelConfig(DEFAULT_ENABLED, DEFAULT_THREAD_POOL_SIZE, DEFAULT_QUEUE_CAPACITY, DEFAULT_TIMEOUT_SECONDS);
    }

    public boolean getEnabledOrDefault() {
        return enabled != null ? enabled : DEFAULT_ENABLED;
    }

    public int getThreadPoolSizeOrDefault() {
        return threadPoolSize != null ? threadPoolSize : DEFAULT_THREAD_POOL_SIZE;
    }

    public int getQueueCapacityOrDefault() {
        return queueCapacity != null ? queueCapacity : DEFAULT_QUEUE_CAPACITY;
    }

    public long getTimeoutSecondsOrDefault() {
        return timeoutSeconds != null ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Boolean enabled = DEFAULT_ENABLED;
        private Integer threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        private Integer queueCapacity = DEFAULT_QUEUE_CAPACITY;
        private Long timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder threadPoolSize(Integer threadPoolSize) {
            this.threadPoolSize = threadPoolSize;
            return this;
        }

        public Builder queueCapacity(Integer queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Builder timeoutSeconds(Long timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public ParallelConfig build() {
            return new ParallelConfig(enabled, threadPoolSize, queueCapacity, timeoutSeconds);
        }
    }
}
