package com.utagent.report;

import java.util.ArrayList;
import java.util.List;

public class DashboardConfig {
    private final boolean includeCoverage;
    private final boolean includeQuality;
    private final boolean includeTrends;
    private final List<String> customSections;

    private DashboardConfig(Builder builder) {
        this.includeCoverage = builder.includeCoverage;
        this.includeQuality = builder.includeQuality;
        this.includeTrends = builder.includeTrends;
        this.customSections = builder.customSections;
    }

    public boolean isIncludeCoverage() {
        return includeCoverage;
    }

    public boolean isIncludeQuality() {
        return includeQuality;
    }

    public boolean isIncludeTrends() {
        return includeTrends;
    }

    public List<String> getCustomSections() {
        return customSections != null ? customSections : new ArrayList<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean includeCoverage = true;
        private boolean includeQuality = true;
        private boolean includeTrends = true;
        private List<String> customSections = new ArrayList<>();

        public Builder includeCoverage(boolean include) {
            this.includeCoverage = include;
            return this;
        }

        public Builder includeQuality(boolean include) {
            this.includeQuality = include;
            return this;
        }

        public Builder includeTrends(boolean include) {
            this.includeTrends = include;
            return this;
        }

        public Builder customSections(List<String> sections) {
            this.customSections = sections;
            return this;
        }

        public DashboardConfig build() {
            return new DashboardConfig(this);
        }
    }
}
