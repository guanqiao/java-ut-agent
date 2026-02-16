package com.utagent.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestFailureDetector {

    private static final Logger logger = LoggerFactory.getLogger(TestFailureDetector.class);

    private final File projectRoot;
    private final Map<String, Integer> failureHistory = new ConcurrentHashMap<>();

    public TestFailureDetector(File projectRoot) {
        this.projectRoot = projectRoot;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public List<TestFailure> detectFailures() {
        List<TestFailure> failures = new ArrayList<>();

        if (!projectRoot.exists() || !projectRoot.isDirectory()) {
            logger.warn("Project root does not exist: {}", projectRoot.getAbsolutePath());
            return failures;
        }

        logger.debug("Scanning for test failures in: {}", projectRoot.getAbsolutePath());

        return failures;
    }

    public String generateFailureReport(List<TestFailure> failures) {
        StringBuilder report = new StringBuilder();

        report.append("=" .repeat(50)).append("\n");
        report.append("Test Failure Report\n");
        report.append("=" .repeat(50)).append("\n\n");

        if (failures.isEmpty()) {
            report.append("No test failures detected.\n");
        } else {
            report.append("Total Failures: ").append(failures.size()).append("\n\n");

            Map<String, List<TestFailure>> grouped = groupByClass(failures);
            for (Map.Entry<String, List<TestFailure>> entry : grouped.entrySet()) {
                report.append("Class: ").append(entry.getKey()).append("\n");
                report.append("-" .repeat(40)).append("\n");
                for (TestFailure failure : entry.getValue()) {
                    report.append(String.format("  Method: %s%n", failure.testMethod()));
                    report.append(String.format("  Type: %s%n", failure.type().displayName()));
                    report.append(String.format("  Message: %s%n", failure.message()));
                    report.append("\n");
                }
            }
        }

        report.append("=" .repeat(50)).append("\n");
        return report.toString();
    }

    public Map<String, List<TestFailure>> groupByClass(List<TestFailure> failures) {
        Map<String, List<TestFailure>> grouped = new HashMap<>();
        for (TestFailure failure : failures) {
            grouped.computeIfAbsent(failure.testClass(), k -> new ArrayList<>()).add(failure);
        }
        return grouped;
    }

    public double calculateFailureRate(int totalTests, int failedTests) {
        if (totalTests == 0) {
            return 0.0;
        }
        return (double) failedTests / totalTests;
    }

    public List<String> identifyFlakyTests(List<TestExecution> executions) {
        Map<String, List<Boolean>> results = new HashMap<>();

        for (TestExecution execution : executions) {
            results.computeIfAbsent(execution.testName(), k -> new ArrayList<>())
                .add(execution.passed());
        }

        List<String> flakyTests = new ArrayList<>();
        for (Map.Entry<String, List<Boolean>> entry : results.entrySet()) {
            List<Boolean> testResults = entry.getValue();
            if (testResults.size() >= 2) {
                boolean hasPassed = testResults.contains(true);
                boolean hasFailed = testResults.contains(false);
                if (hasPassed && hasFailed) {
                    flakyTests.add(entry.getKey());
                }
            }
        }

        return flakyTests;
    }

    public List<TestFailure> prioritizeFailures(List<TestFailure> failures) {
        List<TestFailure> sorted = new ArrayList<>(failures);
        sorted.sort(Comparator.comparingInt((TestFailure f) -> f.severity()).reversed());
        return sorted;
    }

    public String suggestFix(TestFailure failure) {
        if (failure == null) {
            return "No failure to analyze.";
        }

        return switch (failure.type()) {
            case ASSERTION_FAILURE -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Assertion failure detected. Suggestions:\n");
                sb.append("1. Verify the expected value is correct\n");
                sb.append("2. Check if the implementation has changed\n");
                sb.append("3. Review test data and setup\n");
                if (failure.message() != null && failure.message().contains("Expected")) {
                    sb.append("4. Update expected value based on actual behavior\n");
                }
                yield sb.toString();
            }
            case COMPILATION_ERROR -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Compilation error detected. Suggestions:\n");
                sb.append("1. Check for syntax errors\n");
                sb.append("2. Verify all imports are correct\n");
                sb.append("3. Ensure dependencies are available\n");
                yield sb.toString();
            }
            case RUNTIME_ERROR -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Runtime error detected. Suggestions:\n");
                sb.append("1. Check for null pointer exceptions\n");
                sb.append("2. Verify test setup and initialization\n");
                sb.append("3. Review stack trace for root cause\n");
                yield sb.toString();
            }
            case TIMEOUT -> {
                StringBuilder sb = new StringBuilder();
                sb.append("Timeout detected. Suggestions:\n");
                sb.append("1. Optimize test performance\n");
                sb.append("2. Consider splitting long tests\n");
                sb.append("3. Mock slow dependencies\n");
                yield sb.toString();
            }
            default -> "Review the test failure and update accordingly.";
        };
    }

    public void recordFailure(String testName, FailureType type) {
        failureHistory.merge(testName, 1, Integer::sum);
    }

    public Map<String, Integer> getFailureHistory() {
        return Collections.unmodifiableMap(failureHistory);
    }
}
