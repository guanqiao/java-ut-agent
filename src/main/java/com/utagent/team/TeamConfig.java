package com.utagent.team;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TeamConfig {
    private final String teamName;
    private final double targetCoverage;
    private final String namingConvention;
    private final List<String> testPatterns;
    private final String version;
    private final Map<String, Object> customSettings;

    private TeamConfig(Builder builder) {
        this.teamName = builder.teamName;
        this.targetCoverage = builder.targetCoverage;
        this.namingConvention = builder.namingConvention;
        this.testPatterns = builder.testPatterns;
        this.version = builder.version;
        this.customSettings = builder.customSettings;
    }

    public String getTeamName() {
        return teamName;
    }

    public double getTargetCoverage() {
        return targetCoverage;
    }

    public String getNamingConvention() {
        return namingConvention;
    }

    public List<String> getTestPatterns() {
        return testPatterns != null ? testPatterns : new ArrayList<>();
    }

    public String getVersion() {
        return version;
    }

    public Map<String, Object> getCustomSettings() {
        return customSettings;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String teamName;
        private double targetCoverage = 0.8;
        private String namingConvention;
        private List<String> testPatterns = new ArrayList<>();
        private String version = "1.0.0";
        private Map<String, Object> customSettings = new java.util.HashMap<>();

        public Builder teamName(String teamName) {
            this.teamName = teamName;
            return this;
        }

        public Builder targetCoverage(double targetCoverage) {
            this.targetCoverage = targetCoverage;
            return this;
        }

        public Builder namingConvention(String namingConvention) {
            this.namingConvention = namingConvention;
            return this;
        }

        public Builder testPatterns(List<String> testPatterns) {
            this.testPatterns = testPatterns;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder customSettings(Map<String, Object> customSettings) {
            this.customSettings = customSettings;
            return this;
        }

        public TeamConfig build() {
            return new TeamConfig(this);
        }
    }
}
