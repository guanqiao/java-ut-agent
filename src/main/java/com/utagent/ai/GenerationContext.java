package com.utagent.ai;

public class GenerationContext {
    private final String focusArea;
    private final double targetCoverage;
    private final boolean includeNegativeTests;
    private final boolean includeEdgeCases;

    private GenerationContext(Builder builder) {
        this.focusArea = builder.focusArea;
        this.targetCoverage = builder.targetCoverage;
        this.includeNegativeTests = builder.includeNegativeTests;
        this.includeEdgeCases = builder.includeEdgeCases;
    }

    public String getFocusArea() {
        return focusArea;
    }

    public double getTargetCoverage() {
        return targetCoverage;
    }

    public boolean isIncludeNegativeTests() {
        return includeNegativeTests;
    }

    public boolean isIncludeEdgeCases() {
        return includeEdgeCases;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String focusArea;
        private double targetCoverage = 0.8;
        private boolean includeNegativeTests = true;
        private boolean includeEdgeCases = true;

        public Builder focusArea(String focusArea) {
            this.focusArea = focusArea;
            return this;
        }

        public Builder targetCoverage(double targetCoverage) {
            this.targetCoverage = targetCoverage;
            return this;
        }

        public Builder includeNegativeTests(boolean include) {
            this.includeNegativeTests = include;
            return this;
        }

        public Builder includeEdgeCases(boolean include) {
            this.includeEdgeCases = include;
            return this;
        }

        public GenerationContext build() {
            return new GenerationContext(this);
        }
    }
}
