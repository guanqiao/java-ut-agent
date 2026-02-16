package com.utagent.maintenance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestFailureAutoFixer {

    private static final Logger logger = LoggerFactory.getLogger(TestFailureAutoFixer.class);
    
    private static final Pattern EXPECTED_PATTERN = Pattern.compile("expected:\\s*<([^>]+)>");
    private static final Pattern ACTUAL_PATTERN = Pattern.compile("but was:\\s*<([^>]+)>");
    private static final Pattern NULL_FIELD_PATTERN = Pattern.compile("Cannot invoke.*because \"(\\w+)\" is null");

    public FailureAnalysis analyzeFailure(TestFailure failure) {
        FailureType type = determineFailureType(failure);
        String rootCause = determineRootCause(failure);
        String affectedCode = extractAffectedCode(failure);
        int lineNumber = extractLineNumber(failure);
        
        return new FailureAnalysis(type, rootCause, affectedCode, lineNumber);
    }

    public FixSuggestion suggestFix(TestFailure failure) {
        FailureAnalysis analysis = analyzeFailure(failure);
        
        return switch (analysis.getFailureType()) {
            case ASSERTION_FAILURE -> suggestAssertionFix(failure, analysis);
            case NULL_POINTER -> suggestNullPointerFix(failure, analysis);
            case MOCK_CONFIGURATION -> suggestMockFix(failure, analysis);
            case TIMEOUT -> suggestTimeoutFix(failure, analysis);
            default -> new FixSuggestion(
                "Manual review required for: " + failure.errorMessage(),
                "",
                0.3,
                FixType.MANUAL_REVIEW_REQUIRED
            );
        };
    }

    public List<FixSuggestion> suggestFixes(List<TestFailure> failures) {
        List<FixSuggestion> suggestions = new ArrayList<>();
        for (TestFailure failure : failures) {
            suggestions.add(suggestFix(failure));
        }
        return suggestions;
    }

    public List<TestFailure> prioritizeFailures(List<TestFailure> failures) {
        return failures.stream()
            .sorted(Comparator.comparingInt(this::getFailureSeverity).reversed())
            .toList();
    }

    public String generateFixedCode(String originalTest, TestFailure failure) {
        FailureAnalysis analysis = analyzeFailure(failure);
        
        return switch (analysis.getFailureType()) {
            case ASSERTION_FAILURE -> fixAssertionFailure(originalTest, failure);
            case NULL_POINTER -> fixNullPointer(originalTest, failure);
            case MOCK_CONFIGURATION -> fixMockConfiguration(originalTest, failure);
            default -> originalTest;
        };
    }

    private FailureType determineFailureType(TestFailure failure) {
        if (failure.isAssertionFailure()) {
            return FailureType.ASSERTION_FAILURE;
        }
        if (failure.isNullPointerException()) {
            return FailureType.NULL_POINTER;
        }
        if (failure.isMockException()) {
            return FailureType.MOCK_CONFIGURATION;
        }
        if (failure.isTimeoutException()) {
            return FailureType.TIMEOUT;
        }
        if (failure.errorMessage() != null && failure.errorMessage().contains("Compilation")) {
            return FailureType.COMPILATION_ERROR;
        }
        return FailureType.UNKNOWN;
    }

    private String determineRootCause(TestFailure failure) {
        if (failure.isAssertionFailure()) {
            return "Test expectation does not match actual behavior";
        }
        if (failure.isNullPointerException()) {
            Matcher matcher = NULL_FIELD_PATTERN.matcher(failure.errorMessage());
            if (matcher.find()) {
                return "Field '" + matcher.group(1) + "' is not initialized";
            }
            return "Null reference encountered";
        }
        if (failure.isMockException()) {
            return "Mock configuration is incomplete or incorrect";
        }
        return "Unknown root cause";
    }

    private String extractAffectedCode(TestFailure failure) {
        if (failure.stackTrace() != null && !failure.stackTrace().isEmpty()) {
            String[] lines = failure.stackTrace().split("\n");
            if (lines.length > 0) {
                return lines[0];
            }
        }
        return "";
    }

    private int extractLineNumber(TestFailure failure) {
        if (failure.stackTrace() != null) {
            Pattern linePattern = Pattern.compile(":(\\d+)\\)");
            Matcher matcher = linePattern.matcher(failure.stackTrace());
            if (matcher.find()) {
                try {
                    return Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private FixSuggestion suggestAssertionFix(TestFailure failure, FailureAnalysis analysis) {
        String expected = extractExpected(failure.errorMessage());
        String actual = extractActual(failure.errorMessage());
        
        String description = String.format(
            "Assertion mismatch: expected %s but got %s. Consider updating the expected value or verifying the implementation.",
            expected, actual
        );
        
        String suggestedCode = generateAssertionFixCode(expected, actual);
        
        return new FixSuggestion(description, suggestedCode, 0.85, FixType.UPDATE_ASSERTION);
    }

    private FixSuggestion suggestNullPointerFix(TestFailure failure, FailureAnalysis analysis) {
        String fieldName = extractNullFieldName(failure.errorMessage());
        
        String description = fieldName != null 
            ? String.format("Field '%s' is null. Add @Mock annotation or initialize it in @BeforeEach.", fieldName)
            : "Null pointer encountered. Ensure all dependencies are properly mocked or initialized.";
        
        String suggestedCode = fieldName != null
            ? String.format("@Mock\nprivate %s %s;", inferFieldType(failure), fieldName)
            : "// Initialize the null field\n";
        
        return new FixSuggestion(description, suggestedCode, 0.75, FixType.ADD_MOCK);
    }

    private FixSuggestion suggestMockFix(TestFailure failure, FailureAnalysis analysis) {
        return new FixSuggestion(
            "Mock configuration issue detected. Review stubbing setup and ensure mocks are properly configured.",
            "Mockito.when(mock.method()).thenReturn(value);",
            0.7,
            FixType.ADD_STUBBING
        );
    }

    private FixSuggestion suggestTimeoutFix(TestFailure failure, FailureAnalysis analysis) {
        return new FixSuggestion(
            "Test timed out. Consider increasing timeout or optimizing the test.",
            "@Timeout(value = 5, unit = TimeUnit.SECONDS)",
            0.6,
            FixType.MANUAL_REVIEW_REQUIRED
        );
    }

    private String fixAssertionFailure(String originalTest, TestFailure failure) {
        String expected = extractExpected(failure.errorMessage());
        String actual = extractActual(failure.errorMessage());
        
        if (expected != null && actual != null) {
            return originalTest.replaceAll(
                Pattern.quote(expected),
                actual
            );
        }
        
        return originalTest;
    }

    private String fixNullPointer(String originalTest, TestFailure failure) {
        String fieldName = extractNullFieldName(failure.errorMessage());
        
        if (fieldName != null) {
            if (!originalTest.contains("@Mock")) {
                String mockAnnotation = "    @Mock\n    private Object " + fieldName + ";\n";
                int classBodyStart = originalTest.indexOf('{');
                if (classBodyStart > 0) {
                    return originalTest.substring(0, classBodyStart + 1) + 
                           "\n" + mockAnnotation + 
                           originalTest.substring(classBodyStart + 1);
                }
            }
            
            if (!originalTest.contains("MockitoAnnotations.openMocks")) {
                String setupCode = "\n    @BeforeEach\n" +
                    "    void setUp() {\n" +
                    "        MockitoAnnotations.openMocks(this);\n" +
                    "    }\n";
                int classBodyStart = originalTest.indexOf('{');
                if (classBodyStart > 0) {
                    return originalTest.substring(0, classBodyStart + 1) + 
                           setupCode + 
                           originalTest.substring(classBodyStart + 1);
                }
            }
        }
        
        return originalTest;
    }

    private String fixMockConfiguration(String originalTest, TestFailure failure) {
        return originalTest;
    }

    private String extractExpected(String errorMessage) {
        if (errorMessage == null) return null;
        Matcher matcher = EXPECTED_PATTERN.matcher(errorMessage);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractActual(String errorMessage) {
        if (errorMessage == null) return null;
        Matcher matcher = ACTUAL_PATTERN.matcher(errorMessage);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractNullFieldName(String errorMessage) {
        if (errorMessage == null) return null;
        Matcher matcher = NULL_FIELD_PATTERN.matcher(errorMessage);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String inferFieldType(TestFailure failure) {
        return "Object";
    }

    private String generateAssertionFixCode(String expected, String actual) {
        if (expected != null && actual != null) {
            return String.format("// Change expected value from %s to %s\nassertEquals(%s, result);", 
                expected, actual, actual);
        }
        return "// Review and update assertion";
    }

    private int getFailureSeverity(TestFailure failure) {
        FailureType type = determineFailureType(failure);
        return switch (type) {
            case NULL_POINTER -> 3;
            case MOCK_CONFIGURATION -> 2;
            case ASSERTION_FAILURE -> 1;
            case TIMEOUT -> 1;
            default -> 0;
        };
    }
}
