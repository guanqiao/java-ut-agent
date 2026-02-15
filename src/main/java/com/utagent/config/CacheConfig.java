package com.utagent.config;

/**
 * Configuration for parse result caching.
 */
public record CacheConfig(
    Boolean enabled,
    String cacheDirectory,
    Long maxAgeMinutes,
    Long maxSizeMB
) {
    public static final boolean DEFAULT_ENABLED = true;
    public static final String DEFAULT_CACHE_DIRECTORY = ".utagent-cache";
    public static final long DEFAULT_MAX_AGE_MINUTES = 60;
    public static final long DEFAULT_MAX_SIZE_MB = 100;

    public static CacheConfig defaults() {
        return new CacheConfig(DEFAULT_ENABLED, DEFAULT_CACHE_DIRECTORY, DEFAULT_MAX_AGE_MINUTES, DEFAULT_MAX_SIZE_MB);
    }

    public boolean getEnabledOrDefault() {
        return enabled != null ? enabled : DEFAULT_ENABLED;
    }

    public String getCacheDirectoryOrDefault() {
        return cacheDirectory != null ? cacheDirectory : DEFAULT_CACHE_DIRECTORY;
    }

    public long getMaxAgeMinutesOrDefault() {
        return maxAgeMinutes != null ? maxAgeMinutes : DEFAULT_MAX_AGE_MINUTES;
    }

    public long getMaxSizeMBOrDefault() {
        return maxSizeMB != null ? maxSizeMB : DEFAULT_MAX_SIZE_MB;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Boolean enabled = DEFAULT_ENABLED;
        private String cacheDirectory = DEFAULT_CACHE_DIRECTORY;
        private Long maxAgeMinutes = DEFAULT_MAX_AGE_MINUTES;
        private Long maxSizeMB = DEFAULT_MAX_SIZE_MB;

        public Builder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder cacheDirectory(String cacheDirectory) {
            this.cacheDirectory = cacheDirectory;
            return this;
        }

        public Builder maxAgeMinutes(Long maxAgeMinutes) {
            this.maxAgeMinutes = maxAgeMinutes;
            return this;
        }

        public Builder maxSizeMB(Long maxSizeMB) {
            this.maxSizeMB = maxSizeMB;
            return this;
        }

        public CacheConfig build() {
            return new CacheConfig(enabled, cacheDirectory, maxAgeMinutes, maxSizeMB);
        }
    }
}
