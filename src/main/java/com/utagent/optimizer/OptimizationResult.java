package com.utagent.optimizer;

import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageReport;
import com.utagent.model.ParsedTestFile;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OptimizationResult {

    private File sourceFile;
    private ClassInfo classInfo;
    private File generatedTestFile;
    private File existingTestFile;
    private ParsedTestFile parsedTestFile;
    private Map<Integer, CoverageReport> coverageHistory = new HashMap<>();
    private CoverageReport finalCoverage;
    private boolean success;
    private String errorMessage;
    private int iterations;
    private List<String> generatedTestMethods = new ArrayList<>();
    private List<String> addedTestMethods = new ArrayList<>();
    private boolean incremental;

    public File getSourceFile() {
        return sourceFile;
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    public void setClassInfo(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }

    public File getGeneratedTestFile() {
        return generatedTestFile;
    }

    public void setGeneratedTestFile(File generatedTestFile) {
        this.generatedTestFile = generatedTestFile;
    }

    public Map<Integer, CoverageReport> getCoverageHistory() {
        return coverageHistory;
    }

    public void addCoverageReport(int iteration, CoverageReport report) {
        this.coverageHistory.put(iteration, report);
    }

    public CoverageReport getFinalCoverage() {
        return finalCoverage;
    }

    public void setFinalCoverage(CoverageReport finalCoverage) {
        this.finalCoverage = finalCoverage;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public List<String> getGeneratedTestMethods() {
        return generatedTestMethods;
    }

    public void addGeneratedTestMethod(String methodName) {
        this.generatedTestMethods.add(methodName);
    }

    public File getExistingTestFile() {
        return existingTestFile;
    }

    public void setExistingTestFile(File existingTestFile) {
        this.existingTestFile = existingTestFile;
    }

    public ParsedTestFile getParsedTestFile() {
        return parsedTestFile;
    }

    public void setParsedTestFile(ParsedTestFile parsedTestFile) {
        this.parsedTestFile = parsedTestFile;
    }

    public List<String> getAddedTestMethods() {
        return addedTestMethods;
    }

    public void setAddedTestMethods(List<String> addedTestMethods) {
        this.addedTestMethods = addedTestMethods != null ? new ArrayList<>(addedTestMethods) : new ArrayList<>();
    }

    public void addAddedTestMethod(String methodName) {
        this.addedTestMethods.add(methodName);
    }

    public boolean isIncremental() {
        return incremental;
    }

    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }

    public int getExistingTestMethodCount() {
        return parsedTestFile != null ? parsedTestFile.getTestMethodCount() : 0;
    }

    public int getAddedTestMethodCount() {
        return addedTestMethods.size();
    }

    public int getTotalTestMethodCount() {
        return getExistingTestMethodCount() + getAddedTestMethodCount();
    }

    public double getCoverageImprovement() {
        if (coverageHistory.isEmpty()) {
            return 0.0;
        }
        
        CoverageReport initial = coverageHistory.get(0);
        CoverageReport final_ = finalCoverage != null ? finalCoverage : coverageHistory.get(coverageHistory.size() - 1);
        
        if (initial == null || final_ == null) {
            return 0.0;
        }
        
        return final_.overallLineCoverage() - initial.overallLineCoverage();
    }

    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Optimization Result ===%n");
        sb.append("Source File: ").append(sourceFile != null ? sourceFile.getName() : "N/A").append("%n");
        sb.append("Class: ").append(classInfo != null ? classInfo.className() : "N/A").append("%n");
        
        if (incremental) {
            sb.append("Mode: Incremental%n");
            sb.append("Existing Test File: ").append(existingTestFile != null ? existingTestFile.getName() : "N/A").append("%n");
            sb.append("Existing Test Methods: ").append(getExistingTestMethodCount()).append("%n");
            sb.append("Added Test Methods: ").append(getAddedTestMethodCount()).append("%n");
        }
        
        sb.append("Generated Test: ").append(generatedTestFile != null ? generatedTestFile.getName() : "N/A").append("%n");
        sb.append("Success: ").append(success ? "Yes" : "No").append("%n");
        sb.append("Iterations: ").append(iterations).append("%n");
        
        if (finalCoverage != null) {
            sb.append("Final Coverage:%n");
            sb.append(String.format("  Line: %.1f%%%n", finalCoverage.overallLineCoverage() * 100));
            sb.append(String.format("  Branch: %.1f%%%n", finalCoverage.overallBranchCoverage() * 100));
        }
        
        sb.append(String.format("Coverage Improvement: %.1f%%%n", getCoverageImprovement() * 100));
        
        if (errorMessage != null) {
            sb.append("Error: ").append(errorMessage).append("%n");
        }
        
        return sb.toString();
    }
}
