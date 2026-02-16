package com.utagent.plugin;

public class PluginSettings {
    private double targetCoverage = 0.8;
    private boolean includePrivateMethods = false;
    private boolean generateParameterizedTests = true;
    private boolean includeNegativeTests = true;
    private String outputFormat = "junit5";
    private boolean showCoverageGutter = true;
    private boolean autoSaveGeneratedTests = false;

    public double getTargetCoverage() {
        return targetCoverage;
    }

    public void setTargetCoverage(double targetCoverage) {
        this.targetCoverage = targetCoverage;
    }

    public boolean isIncludePrivateMethods() {
        return includePrivateMethods;
    }

    public void setIncludePrivateMethods(boolean includePrivateMethods) {
        this.includePrivateMethods = includePrivateMethods;
    }

    public boolean isGenerateParameterizedTests() {
        return generateParameterizedTests;
    }

    public void setGenerateParameterizedTests(boolean generateParameterizedTests) {
        this.generateParameterizedTests = generateParameterizedTests;
    }

    public boolean isIncludeNegativeTests() {
        return includeNegativeTests;
    }

    public void setIncludeNegativeTests(boolean includeNegativeTests) {
        this.includeNegativeTests = includeNegativeTests;
    }

    public String getOutputFormat() {
        return outputFormat;
    }

    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }

    public boolean isShowCoverageGutter() {
        return showCoverageGutter;
    }

    public void setShowCoverageGutter(boolean showCoverageGutter) {
        this.showCoverageGutter = showCoverageGutter;
    }

    public boolean isAutoSaveGeneratedTests() {
        return autoSaveGeneratedTests;
    }

    public void setAutoSaveGeneratedTests(boolean autoSaveGeneratedTests) {
        this.autoSaveGeneratedTests = autoSaveGeneratedTests;
    }
}
