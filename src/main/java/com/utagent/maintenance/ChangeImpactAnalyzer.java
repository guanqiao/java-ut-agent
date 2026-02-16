package com.utagent.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChangeImpactAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(ChangeImpactAnalyzer.class);

    private final File projectRoot;
    private static final Pattern METHOD_PATTERN = Pattern.compile(
        "(public|private|protected)\\s+\\w+\\s+(\\w+)\\s*\\([^)]*\\)"
    );

    public ChangeImpactAnalyzer(File projectRoot) {
        this.projectRoot = projectRoot;
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public ImpactAnalysisResult analyzeChange(File sourceFile, String oldContent, String newContent) {
        Set<String> oldMethods = extractMethods(oldContent);
        Set<String> newMethods = extractMethods(newContent);

        Set<String> changedMethods = new HashSet<>();
        Set<String> addedMethods = new HashSet<>(newMethods);
        addedMethods.removeAll(oldMethods);

        Set<String> deletedMethods = new HashSet<>(oldMethods);
        deletedMethods.removeAll(newMethods);

        for (String method : oldMethods) {
            if (newMethods.contains(method)) {
                if (hasMethodChanged(oldContent, newContent, method)) {
                    changedMethods.add(method);
                }
            }
        }

        Set<String> affectedTests = findAffectedTests(sourceFile, changedMethods);

        boolean hasSignatureChange = detectSignatureChange(oldContent, newContent);
        ImpactLevel level = determineImpactLevel(changedMethods, addedMethods, deletedMethods, hasSignatureChange);

        return ImpactAnalysisResult.builder()
            .sourceFile(sourceFile.getName())
            .changedMethods(changedMethods)
            .addedMethods(addedMethods)
            .deletedMethods(deletedMethods)
            .affectedTests(affectedTests)
            .impactLevel(level)
            .hasSignatureChange(hasSignatureChange)
            .build();
    }

    private Set<String> extractMethods(String content) {
        Set<String> methods = new HashSet<>();
        Matcher matcher = METHOD_PATTERN.matcher(content);
        while (matcher.find()) {
            methods.add(matcher.group(2));
        }
        return methods;
    }

    private boolean hasMethodChanged(String oldContent, String newContent, String method) {
        Pattern methodBodyPattern = Pattern.compile(
            "(public|private|protected)\\s+\\w+\\s+" + method + "\\s*\\([^)]*\\)\\s*\\{",
            Pattern.DOTALL
        );

        Matcher oldMatcher = methodBodyPattern.matcher(oldContent);
        Matcher newMatcher = methodBodyPattern.matcher(newContent);

        if (oldMatcher.find() && newMatcher.find()) {
            String oldMethodBody = extractMethodBody(oldContent, oldMatcher.end() - 1);
            String newMethodBody = extractMethodBody(newContent, newMatcher.end() - 1);
            return !oldMethodBody.equals(newMethodBody);
        }

        return false;
    }

    private String extractMethodBody(String content, int startIndex) {
        int braceCount = 0;
        int pos = startIndex;
        boolean started = false;

        while (pos < content.length()) {
            char c = content.charAt(pos);
            if (c == '{') {
                braceCount++;
                started = true;
            } else if (c == '}') {
                braceCount--;
                if (started && braceCount == 0) {
                    return content.substring(startIndex, pos + 1);
                }
            }
            pos++;
        }

        return "";
    }

    private Set<String> findAffectedTests(File sourceFile, Set<String> changedMethods) {
        Set<String> affectedTests = new HashSet<>();

        String className = sourceFile.getName().replace(".java", "");
        File testDir = new File(projectRoot, "src/test/java");
        File testFile = findTestFile(testDir, className + "Test.java");

        if (testFile != null && testFile.exists()) {
            affectedTests.add(className + "Test");
        }

        return affectedTests;
    }

    private File findTestFile(File dir, String testFileName) {
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                File found = findTestFile(file, testFileName);
                if (found != null) {
                    return found;
                }
            } else if (file.getName().equals(testFileName)) {
                return file;
            }
        }

        return null;
    }

    private boolean detectSignatureChange(String oldContent, String newContent) {
        Pattern signaturePattern = Pattern.compile(
            "(public|private|protected)\\s+\\w+\\s+\\w+\\s*\\([^)]*\\)"
        );

        Set<String> oldSignatures = new HashSet<>();
        Matcher oldMatcher = signaturePattern.matcher(oldContent);
        while (oldMatcher.find()) {
            oldSignatures.add(oldMatcher.group());
        }

        Set<String> newSignatures = new HashSet<>();
        Matcher newMatcher = signaturePattern.matcher(newContent);
        while (newMatcher.find()) {
            newSignatures.add(newMatcher.group());
        }

        for (String sig : oldSignatures) {
            if (!newSignatures.contains(sig)) {
                return true;
            }
        }

        return false;
    }

    private ImpactLevel determineImpactLevel(Set<String> changed, Set<String> added, 
                                              Set<String> deleted, boolean signatureChange) {
        if (signatureChange || !deleted.isEmpty()) {
            return ImpactLevel.HIGH;
        }
        if (!changed.isEmpty() && changed.size() > 2) {
            return ImpactLevel.HIGH;
        }
        if (!changed.isEmpty()) {
            return ImpactLevel.MEDIUM;
        }
        if (!added.isEmpty()) {
            return ImpactLevel.LOW;
        }
        return ImpactLevel.NONE;
    }

    public String generateImpactReport(ImpactAnalysisResult result) {
        StringBuilder report = new StringBuilder();

        report.append("=" .repeat(50)).append("\n");
        report.append("Impact Analysis Report\n");
        report.append("=" .repeat(50)).append("\n\n");

        report.append("Source File: ").append(result.sourceFile()).append("\n");
        report.append("Impact Level: ").append(result.impactLevel().label()).append("\n");
        report.append("Signature Change: ").append(result.hasSignatureChange() ? "Yes" : "No").append("\n\n");

        if (!result.changedMethods().isEmpty()) {
            report.append("Changed Methods:\n");
            for (String method : result.changedMethods()) {
                report.append("  - ").append(method).append("\n");
            }
            report.append("\n");
        }

        if (!result.addedMethods().isEmpty()) {
            report.append("Added Methods:\n");
            for (String method : result.addedMethods()) {
                report.append("  + ").append(method).append("\n");
            }
            report.append("\n");
        }

        if (!result.deletedMethods().isEmpty()) {
            report.append("Deleted Methods:\n");
            for (String method : result.deletedMethods()) {
                report.append("  - ").append(method).append("\n");
            }
            report.append("\n");
        }

        if (!result.affectedTests().isEmpty()) {
            report.append("Affected Tests:\n");
            for (String test : result.affectedTests()) {
                report.append("  * ").append(test).append("\n");
            }
            report.append("\n");
        }

        report.append("=" .repeat(50)).append("\n");
        return report.toString();
    }

    public List<String> prioritizeTests(List<ImpactAnalysisResult> impacts) {
        List<ImpactAnalysisResult> sorted = new ArrayList<>(impacts);
        sorted.sort(Comparator.comparingInt((ImpactAnalysisResult r) -> r.impactLevel().priority()).reversed());

        List<String> prioritized = new ArrayList<>();
        for (ImpactAnalysisResult result : sorted) {
            prioritized.addAll(result.affectedTests());
        }

        return prioritized;
    }

    public List<String> suggestTestUpdates(ImpactAnalysisResult result) {
        List<String> suggestions = new ArrayList<>();

        if (result.hasSignatureChange()) {
            suggestions.add("Method signature changed - update test method calls accordingly");
        }

        for (String method : result.changedMethods()) {
            suggestions.add("Review tests for method: " + method);
        }

        for (String method : result.deletedMethods()) {
            suggestions.add("Remove tests for deleted method: " + method);
        }

        for (String method : result.addedMethods()) {
            suggestions.add("Add tests for new method: " + method);
        }

        if (suggestions.isEmpty()) {
            suggestions.add("No test updates required");
        }

        return suggestions;
    }

    public List<ImpactAnalysisResult> analyzeChanges(List<FileChange> changes) {
        List<ImpactAnalysisResult> results = new ArrayList<>();

        for (FileChange change : changes) {
            ImpactAnalysisResult result = analyzeChange(change.file(), change.oldContent(), change.newContent());
            results.add(result);
        }

        return results;
    }

    public double calculateCoverageImpact(Set<String> existingTests, Set<String> changedMethods) {
        if (changedMethods.isEmpty()) {
            return 1.0;
        }

        int coveredCount = 0;
        for (String method : changedMethods) {
            for (String test : existingTests) {
                if (test.toLowerCase().contains(method.toLowerCase())) {
                    coveredCount++;
                    break;
                }
            }
        }

        return (double) coveredCount / changedMethods.size();
    }

    public Set<String> findOrphanedTests(File projectRoot) {
        Set<String> orphanedTests = new HashSet<>();

        File testDir = new File(projectRoot, "src/test/java");
        File srcDir = new File(projectRoot, "src/main/java");

        Set<String> sourceMethods = new HashSet<>();
        collectMethods(srcDir, sourceMethods);

        scanForOrphanedTests(testDir, sourceMethods, orphanedTests);

        return orphanedTests;
    }

    private void collectMethods(File dir, Set<String> methods) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                collectMethods(file, methods);
            } else if (file.getName().endsWith(".java")) {
                try {
                    String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    methods.addAll(extractMethods(content));
                } catch (IOException e) {
                    logger.warn("Failed to read file: {}", file.getAbsolutePath());
                }
            }
        }
    }

    private void scanForOrphanedTests(File dir, Set<String> sourceMethods, Set<String> orphanedTests) {
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanForOrphanedTests(file, sourceMethods, orphanedTests);
            } else if (file.getName().endsWith(".java")) {
                try {
                    String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
                    Pattern testPattern = Pattern.compile("@Test\\s+void\\s+(\\w+)");
                    Matcher matcher = testPattern.matcher(content);

                    while (matcher.find()) {
                        String testName = matcher.group(1);
                        String testedMethod = extractTestedMethodName(testName);

                        if (!sourceMethods.contains(testedMethod)) {
                            orphanedTests.add(testName);
                        }
                    }
                } catch (IOException e) {
                    logger.warn("Failed to read test file: {}", file.getAbsolutePath());
                }
            }
        }
    }

    private String extractTestedMethodName(String testName) {
        if (testName.startsWith("test")) {
            String name = testName.substring(4);
            if (!name.isEmpty()) {
                return Character.toLowerCase(name.charAt(0)) + name.substring(1);
            }
        }
        if (testName.startsWith("should")) {
            return testName.substring(6).toLowerCase();
        }
        return testName;
    }
}
