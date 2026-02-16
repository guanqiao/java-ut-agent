package com.utagent.quality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestSmellDetector {

    private static final Logger logger = LoggerFactory.getLogger(TestSmellDetector.class);

    private static final int MAX_TEST_LINES = 30;
    private static final int MAX_ASSERTIONS = 5;
    private static final int MAX_SETUP_LINES = 10;

    public List<TestSmell> detect(String testCode) {
        List<TestSmell> smells = new ArrayList<>();

        if (testCode == null || testCode.isEmpty()) {
            return smells;
        }

        smells.addAll(detectDuplicateAssertions(testCode));
        smells.addAll(detectMagicNumbers(testCode));
        smells.addAll(detectLongTestMethods(testCode));
        smells.addAll(detectEmptyTests(testCode));
        smells.addAll(detectMissingAssertions(testCode));
        smells.addAll(detectMultipleAssertions(testCode));
        smells.addAll(detectPrintStatements(testCode));
        smells.addAll(detectIgnoredTests(testCode));
        smells.addAll(detectComplexSetup(testCode));
        smells.addAll(detectAssertionRoulette(testCode));
        smells.addAll(detectEagerTests(testCode));
        smells.addAll(detectGeneralFixture(testCode));

        return smells;
    }

    private List<TestSmell> detectDuplicateAssertions(String code) {
        List<TestSmell> smells = new ArrayList<>();
        String[] lines = code.split("\n");

        Set<String> seenAssertions = new HashSet<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (isAssertion(line)) {
                if (seenAssertions.contains(line)) {
                    smells.add(TestSmell.builder()
                        .type(TestSmellType.DUPLICATE_ASSERTION)
                        .severity(TestSmellSeverity.MEDIUM)
                        .lineNumber(i + 1)
                        .message("Duplicate assertion found: " + line)
                        .suggestion("Remove duplicate assertion or verify different behavior")
                        .build());
                }
                seenAssertions.add(line);
            }
        }

        return smells;
    }

    private List<TestSmell> detectMagicNumbers(String code) {
        List<TestSmell> smells = new ArrayList<>();
        String[] lines = code.split("\n");

        Pattern pattern = Pattern.compile("\\b\\d{2,}\\b");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (isAssertion(line) || line.contains("assertEquals") || line.contains("assertThat")) {
                Matcher matcher = pattern.matcher(line);
                if (matcher.find()) {
                    smells.add(TestSmell.builder()
                        .type(TestSmellType.MAGIC_NUMBER)
                        .severity(TestSmellSeverity.LOW)
                        .lineNumber(i + 1)
                        .message("Magic number detected: " + matcher.group())
                        .suggestion("Use named constants for better readability")
                        .build());
                }
            }
        }

        return smells;
    }

    private List<TestSmell> detectLongTestMethods(String code) {
        List<TestSmell> smells = new ArrayList<>();
        Pattern testMethodPattern = Pattern.compile("@Test[^@]*", Pattern.DOTALL);
        Matcher matcher = testMethodPattern.matcher(code);

        while (matcher.find()) {
            String testMethod = matcher.group();
            int lines = testMethod.split("\n").length;
            if (lines > MAX_TEST_LINES) {
                int startLine = countLines(code.substring(0, matcher.start())) + 1;
                smells.add(TestSmell.builder()
                    .type(TestSmellType.LONG_TEST_METHOD)
                    .severity(TestSmellSeverity.MEDIUM)
                    .lineNumber(startLine)
                    .message("Test method has " + lines + " lines (max: " + MAX_TEST_LINES + ")")
                    .suggestion("Split into smaller, focused test methods")
                    .build());
            }
        }

        return smells;
    }

    private List<TestSmell> detectEmptyTests(String code) {
        List<TestSmell> smells = new ArrayList<>();
        Pattern pattern = Pattern.compile("@Test\\s+void\\s+\\w+\\(\\)\\s*\\{\\s*\\}");
        Matcher matcher = pattern.matcher(code);

        while (matcher.find()) {
            int lineNum = countLines(code.substring(0, matcher.start())) + 1;
            smells.add(TestSmell.builder()
                .type(TestSmellType.EMPTY_TEST)
                .severity(TestSmellSeverity.HIGH)
                .lineNumber(lineNum)
                .message("Empty test method detected")
                .suggestion("Add test implementation or remove the test")
                .build());
        }

        return smells;
    }

    private List<TestSmell> detectMissingAssertions(String code) {
        List<TestSmell> smells = new ArrayList<>();
        String[] lines = code.split("\n");
        int testStart = -1;
        int braceCount = 0;
        boolean inTest = false;
        StringBuilder currentTest = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.contains("@Test")) {
                testStart = i + 1;
                inTest = true;
                currentTest = new StringBuilder();
            }
            if (inTest) {
                currentTest.append(line).append("\n");
                braceCount += countChar(line, '{');
                braceCount -= countChar(line, '}');
                if (braceCount == 0 && currentTest.length() > 0) {
                    if (!hasAssertion(currentTest.toString()) && !currentTest.toString().trim().endsWith("{}")) {
                        smells.add(TestSmell.builder()
                            .type(TestSmellType.MISSING_ASSERTION)
                            .severity(TestSmellSeverity.HIGH)
                            .lineNumber(testStart)
                            .message("Test method lacks assertions")
                            .suggestion("Add assertions to verify expected behavior")
                            .build());
                    }
                    inTest = false;
                    currentTest = new StringBuilder();
                }
            }
        }

        return smells;
    }

    private int countChar(String s, char c) {
        int count = 0;
        for (char ch : s.toCharArray()) {
            if (ch == c) count++;
        }
        return count;
    }

    private List<TestSmell> detectMultipleAssertions(String code) {
        List<TestSmell> smells = new ArrayList<>();
        Pattern testMethodPattern = Pattern.compile("@Test[^@]*", Pattern.DOTALL);
        Matcher matcher = testMethodPattern.matcher(code);

        while (matcher.find()) {
            String testMethod = matcher.group();
            int assertionCount = countAssertions(testMethod);
            if (assertionCount > MAX_ASSERTIONS) {
                int startLine = countLines(code.substring(0, matcher.start())) + 1;
                smells.add(TestSmell.builder()
                    .type(TestSmellType.MULTIPLE_ASSERTIONS)
                    .severity(TestSmellSeverity.MEDIUM)
                    .lineNumber(startLine)
                    .message("Test has " + assertionCount + " assertions (max: " + MAX_ASSERTIONS + ")")
                    .suggestion("Split into multiple focused tests")
                    .build());
            }
        }

        return smells;
    }

    private List<TestSmell> detectPrintStatements(String code) {
        List<TestSmell> smells = new ArrayList<>();
        String[] lines = code.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("System.out.print") || line.contains("System.err.print")) {
                smells.add(TestSmell.builder()
                    .type(TestSmellType.PRINT_STATEMENT)
                    .severity(TestSmellSeverity.LOW)
                    .lineNumber(i + 1)
                    .message("Print statement found in test")
                    .suggestion("Remove debug print statements or use proper logging")
                    .build());
            }
        }

        return smells;
    }

    private List<TestSmell> detectIgnoredTests(String code) {
        List<TestSmell> smells = new ArrayList<>();
        String[] lines = code.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("@Disabled") || line.startsWith("@Ignore")) {
                smells.add(TestSmell.builder()
                    .type(TestSmellType.IGNORED_TEST)
                    .severity(TestSmellSeverity.INFO)
                    .lineNumber(i + 1)
                    .message("Disabled test found")
                    .suggestion("Enable or remove disabled tests")
                    .build());
            }
        }

        return smells;
    }

    private List<TestSmell> detectComplexSetup(String code) {
        List<TestSmell> smells = new ArrayList<>();
        Pattern setupPattern = Pattern.compile("@(BeforeEach|Before)\\s*\\n[^@]*", Pattern.DOTALL);
        Matcher matcher = setupPattern.matcher(code);

        while (matcher.find()) {
            String setupMethod = matcher.group();
            int lines = setupMethod.split("\n").length;
            if (lines > MAX_SETUP_LINES) {
                int startLine = countLines(code.substring(0, matcher.start())) + 1;
                smells.add(TestSmell.builder()
                    .type(TestSmellType.COMPLEX_SETUP)
                    .severity(TestSmellSeverity.MEDIUM)
                    .lineNumber(startLine)
                    .message("Setup method has " + lines + " lines (max: " + MAX_SETUP_LINES + ")")
                    .suggestion("Simplify setup or use test fixtures")
                    .build());
            }
        }

        return smells;
    }

    private List<TestSmell> detectAssertionRoulette(String code) {
        List<TestSmell> smells = new ArrayList<>();
        Pattern testMethodPattern = Pattern.compile("@Test[^@]*", Pattern.DOTALL);
        Matcher matcher = testMethodPattern.matcher(code);

        while (matcher.find()) {
            String testMethod = matcher.group();
            if (hasAssertionWithoutMessage(testMethod)) {
                int startLine = countLines(code.substring(0, matcher.start())) + 1;
                smells.add(TestSmell.builder()
                    .type(TestSmellType.ASSERTION_ROULETTE)
                    .severity(TestSmellSeverity.LOW)
                    .lineNumber(startLine)
                    .message("Assertions without descriptive messages")
                    .suggestion("Add messages to assertions for better failure diagnostics")
                    .build());
            }
        }

        return smells;
    }

    private List<TestSmell> detectEagerTests(String code) {
        List<TestSmell> smells = new ArrayList<>();
        Pattern testMethodPattern = Pattern.compile("@Test[^@]*", Pattern.DOTALL);
        Matcher matcher = testMethodPattern.matcher(code);

        while (matcher.find()) {
            String testMethod = matcher.group();
            int differentMethods = countDifferentMethodCalls(testMethod);
            if (differentMethods > 2) {
                int startLine = countLines(code.substring(0, matcher.start())) + 1;
                smells.add(TestSmell.builder()
                    .type(TestSmellType.EAGER_TEST)
                    .severity(TestSmellSeverity.MEDIUM)
                    .lineNumber(startLine)
                    .message("Test verifies " + differentMethods + " different behaviors")
                    .suggestion("Split into separate tests for each behavior")
                    .build());
            }
        }

        return smells;
    }

    private List<TestSmell> detectGeneralFixture(String code) {
        List<TestSmell> smells = new ArrayList<>();

        if (code.contains("@BeforeAll") || code.contains("@BeforeClass")) {
            Pattern sharedFieldPattern = Pattern.compile("private\\s+static\\s+\\w+\\s+\\w+");
            Matcher matcher = sharedFieldPattern.matcher(code);
            if (matcher.find()) {
                int lineNum = countLines(code.substring(0, matcher.start())) + 1;
                smells.add(TestSmell.builder()
                    .type(TestSmellType.GENERAL_FIXTURE)
                    .severity(TestSmellSeverity.MEDIUM)
                    .lineNumber(lineNum)
                    .message("Shared fixture detected")
                    .suggestion("Use instance-level fixtures for test isolation")
                    .build());
            }
        }

        return smells;
    }

    private boolean isAssertion(String line) {
        return line.contains("assert") || line.contains("expect") || line.contains("verify") || line.contains("should");
    }

    private boolean hasAssertion(String testMethod) {
        String lower = testMethod.toLowerCase();
        boolean hasAssertKeyword = lower.contains("assert") || lower.contains("expect") || 
                                   lower.contains("verify") || lower.contains("fail(");
        return hasAssertKeyword;
    }

    private int countAssertions(String testMethod) {
        int count = 0;
        String[] lines = testMethod.split("\n");
        for (String line : lines) {
            if (isAssertion(line.trim())) {
                count++;
            }
        }
        return count;
    }

    private boolean hasAssertionWithoutMessage(String testMethod) {
        Pattern pattern = Pattern.compile("assertEquals\\([^,]+,\\s*[^)]+\\)");
        Matcher matcher = pattern.matcher(testMethod);
        return matcher.find();
    }

    private int countDifferentMethodCalls(String testMethod) {
        Set<String> methods = new HashSet<>();
        Pattern pattern = Pattern.compile("\\.(\\w+)\\(");
        Matcher matcher = pattern.matcher(testMethod);
        while (matcher.find()) {
            String method = matcher.group(1);
            if (!method.equals("assertEquals") && !method.equals("assertTrue") && 
                !method.equals("assertFalse") && !method.equals("assertNotNull")) {
                methods.add(method);
            }
        }
        return methods.size();
    }

    private int countLines(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return text.split("\n").length;
    }

    public double calculateSmellScore(String testCode) {
        List<TestSmell> smells = detect(testCode);
        if (smells.isEmpty()) {
            return 1.0;
        }

        double penalty = 0.0;
        for (TestSmell smell : smells) {
            penalty += smell.severity().level() * 0.05;
        }

        return Math.max(0.0, 1.0 - penalty);
    }

    public String generateSmellReport(String testCode) {
        List<TestSmell> smells = detect(testCode);
        StringBuilder report = new StringBuilder();

        report.append("=" .repeat(50)).append("\n");
        report.append("Test Smell Report\n");
        report.append("=" .repeat(50)).append("\n\n");

        if (smells.isEmpty()) {
            report.append("No test smells detected. Good job!\n");
        } else {
            report.append("Summary: ").append(smells.size()).append(" smell(s) detected\n\n");

            for (TestSmell smell : smells) {
                report.append(String.format("- [%s] %s (Line %d)%n",
                    smell.severity(), smell.type().displayName(), smell.lineNumber()));
                report.append(String.format("  Message: %s%n", smell.message()));
                report.append(String.format("  Suggestion: %s%n%n", smell.suggestion()));
            }
        }

        report.append("=" .repeat(50)).append("\n");
        report.append(String.format("Smell Score: %.2f%%%n", calculateSmellScore(testCode) * 100));

        return report.toString();
    }
}
