package com.utagent.ide;

public class IDEOptions {
    private final boolean includePrivateMethods;
    private final double targetCoverage;
    private final boolean generateParameterizedTests;
    private final boolean includeNegativeTests;
    private final String outputFormat;

    private IDEOptions(Builder builder) {
        this.includePrivateMethods = builder.includePrivateMethods;
        this.targetCoverage = builder.targetCoverage;
        this.generateParameterizedTests = builder.generateParameterizedTests;
        this.includeNegativeTests = builder.includeNegativeTests;
        this.outputFormat = builder.outputFormat;
    }

    public boolean isIncludePrivateMethods() {
        return includePrivateMethods;
    }

    public double getTargetCoverage() {
        return targetCoverage;
    }

    public boolean isGenerateParameterizedTests() {
        return generateParameterizedTests;
    }

    public boolean isIncludeNegativeTests() {
        return includeNegativeTests;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean includePrivateMethods = false;
        private double targetCoverage = 0.8;
        private boolean generateParameterizedTests = true;
        private boolean includeNegativeTests = true;
        private String outputFormat = "junit5";

        public Builder includePrivateMethods(boolean include) {
            this.includePrivateMethods = include;
            return this;
        }

        public Builder targetCoverage(double coverage) {
            this.targetCoverage = coverage;
            return this;
        }

        public Builder generateParameterizedTests(boolean generate) {
            this.generateParameterizedTests = generate;
            return this;
        }

        public Builder includeNegativeTests(boolean include) {
            this.includeNegativeTests = include;
            return this;
        }

        public Builder outputFormat(String format) {
            this.outputFormat = format;
            return this;
        }

        public IDEOptions build() {
            return new IDEOptions(this);
        }
    }
}
