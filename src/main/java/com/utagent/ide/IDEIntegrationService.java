package com.utagent.ide;

import com.utagent.assertion.SmartAssertionGenerator;
import com.utagent.generator.TestGenerator;
import com.utagent.model.ClassInfo;
import com.utagent.model.MethodInfo;
import com.utagent.testdata.TestDataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IDEIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(IDEIntegrationService.class);
    
    private final TestDataFactory testDataFactory;
    private final SmartAssertionGenerator assertionGenerator;

    public IDEIntegrationService() {
        this.testDataFactory = new TestDataFactory();
        this.assertionGenerator = new SmartAssertionGenerator();
    }

    public IDERequest createRequest(ClassInfo classInfo) {
        return new IDERequest(classInfo, null, IDEOptions.builder().build());
    }

    public IDERequest createRequest(ClassInfo classInfo, IDEOptions options) {
        return new IDERequest(classInfo, null, options);
    }

    public IDERequest createRequestForMethod(ClassInfo classInfo, MethodInfo method) {
        return new IDERequest(classInfo, method, IDEOptions.builder().build());
    }

    public IDEResponse generateTest(IDERequest request) {
        if (request == null || request.getTargetClass() == null) {
            return IDEResponse.failure("Invalid request: target class is required");
        }

        try {
            ClassInfo classInfo = request.getTargetClass();
            String testCode = generateTestCode(classInfo, request.getTargetMethod(), request.getOptions());
            double coverageEstimate = estimateCoverage(classInfo);
            String testClassName = classInfo.className() + "Test";

            return IDEResponse.success(testCode, coverageEstimate, testClassName);
        } catch (Exception e) {
            logger.error("Failed to generate test", e);
            return IDEResponse.failure("Generation failed: " + e.getMessage());
        }
    }

    public String formatForPreview(String testCode) {
        if (testCode == null || testCode.isEmpty()) {
            return "";
        }
        
        StringBuilder formatted = new StringBuilder();
        String[] lines = testCode.split("\n");
        int indentLevel = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.endsWith("}") || trimmed.startsWith("}")) {
                indentLevel = Math.max(0, indentLevel - 1);
            }
            
            formatted.append("    ".repeat(indentLevel)).append(trimmed).append("\n");
            
            if (trimmed.endsWith("{")) {
                indentLevel++;
            }
        }
        
        return formatted.toString();
    }

    public String highlightDiff(String oldCode, String newCode) {
        if (oldCode == null || newCode == null) {
            return "";
        }

        StringBuilder diff = new StringBuilder();
        diff.append("// Original:\n");
        diff.append("// ").append(oldCode).append("\n");
        diff.append("// Modified:\n");
        diff.append("// ").append(newCode).append("\n");
        
        return diff.toString();
    }

    public QuickFix suggestQuickFix(String testCode, String failureMessage) {
        if (failureMessage == null) {
            return new QuickFix("Unable to analyze failure", testCode, FixType.MODIFY_TEST_DATA, 0);
        }

        if (failureMessage.contains("expected:") || failureMessage.contains("AssertionFailed")) {
            return suggestAssertionFix(testCode, failureMessage);
        }

        if (failureMessage.contains("NullPointerException")) {
            return suggestNullPointerFix(testCode, failureMessage);
        }

        if (failureMessage.contains("Mockito") || failureMessage.contains("mock")) {
            return suggestMockFix(testCode, failureMessage);
        }

        return new QuickFix("Manual review required", testCode, FixType.MODIFY_TEST_DATA, 30);
    }

    public IDEContext extractContext(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return new IDEContext("", "", "", "");
        }

        File file = new File(filePath);
        String fileName = file.getName();
        String className = fileName.replace(".java", "");

        Pattern packagePattern = Pattern.compile("src[\\\\/]main[\\\\/]java[\\\\/](.+)[\\\\/]" + Pattern.quote(fileName));
        Matcher matcher = packagePattern.matcher(filePath.replace('\\', '/'));
        
        String packageName = "";
        if (matcher.find()) {
            packageName = matcher.group(1).replace('/', '.');
        }

        String projectRoot = filePath;
        int srcIndex = filePath.indexOf("src");
        if (srcIndex > 0) {
            projectRoot = filePath.substring(0, srcIndex);
        }

        return new IDEContext(packageName, className, filePath, projectRoot);
    }

    public String resolveTestLocation(String sourcePath) {
        if (sourcePath == null) {
            return "";
        }

        return sourcePath
            .replace("src/main/java", "src/test/java")
            .replace(".java", "Test.java");
    }

    private String generateTestCode(ClassInfo classInfo, MethodInfo targetMethod, IDEOptions options) {
        StringBuilder code = new StringBuilder();
        
        String packageName = classInfo.packageName();
        String className = classInfo.className();
        String testClassName = className + "Test";

        code.append("package ").append(packageName).append(";\n\n");
        code.append("import org.junit.jupiter.api.Test;\n");
        code.append("import org.junit.jupiter.api.BeforeEach;\n");
        code.append("import org.junit.jupiter.api.DisplayName;\n");
        code.append("import static org.assertj.core.api.Assertions.assertThat;\n\n");
        
        code.append("@DisplayName(\"").append(className).append(" Tests\")\n");
        code.append("class ").append(testClassName).append(" {\n\n");
        
        code.append("    private ").append(className).append(" ").append(toCamelCase(className)).append(";\n\n");
        
        code.append("    @BeforeEach\n");
        code.append("    void setUp() {\n");
        code.append("        ").append(toCamelCase(className)).append(" = new ").append(className).append("();\n");
        code.append("    }\n\n");

        if (targetMethod != null) {
            code.append(generateTestMethod(targetMethod, options));
        } else {
            for (MethodInfo method : classInfo.methods()) {
                if (options.isIncludePrivateMethods() || method.isPublic()) {
                    code.append(generateTestMethod(method, options));
                }
            }
        }

        code.append("}\n");
        
        return code.toString();
    }

    private String generateTestMethod(MethodInfo method, IDEOptions options) {
        StringBuilder code = new StringBuilder();
        
        String methodName = method.name();
        String displayName = generateDisplayName(methodName);
        
        code.append("    @Test\n");
        code.append("    @DisplayName(\"").append(displayName).append("\")\n");
        code.append("    void ").append(generateTestMethodName(methodName)).append("() {\n");
        
        if (!method.parameters().isEmpty()) {
            for (var param : method.parameters()) {
                code.append("        var ").append(param.name()).append(" = ")
                    .append(generateDefaultValue(param.type())).append(";\n");
            }
            code.append("\n");
        }
        
        code.append("        // Act\n");
        code.append("        var result = ").append(toCamelCase("test")).append(".").append(methodName).append("(");
        if (!method.parameters().isEmpty()) {
            code.append(String.join(", ", method.parameters().stream().map(p -> p.name()).toList()));
        }
        code.append(");\n\n");
        
        code.append("        // Assert\n");
        code.append(assertionGenerator.generateAssertion(method));
        
        code.append("    }\n\n");
        
        return code.toString();
    }

    private double estimateCoverage(ClassInfo classInfo) {
        if (classInfo.methods().isEmpty()) {
            return 1.0;
        }
        
        int totalMethods = classInfo.methods().size();
        int testableMethods = (int) classInfo.methods().stream()
            .filter(m -> !m.name().equals("<init>") && !m.name().equals("<clinit>"))
            .count();
        
        return testableMethods > 0 ? (double) testableMethods / totalMethods : 0.8;
    }

    private QuickFix suggestAssertionFix(String testCode, String failureMessage) {
        Pattern expectedPattern = Pattern.compile("expected:\\s*<([^>]+)>");
        Pattern actualPattern = Pattern.compile("but was:\\s*<([^>]+)>");
        
        String expected = null;
        String actual = null;
        
        Matcher expectedMatcher = expectedPattern.matcher(failureMessage);
        if (expectedMatcher.find()) {
            expected = expectedMatcher.group(1);
        }
        
        Matcher actualMatcher = actualPattern.matcher(failureMessage);
        if (actualMatcher.find()) {
            actual = actualMatcher.group(1);
        }
        
        if (expected != null && actual != null) {
            String fixedCode = testCode.replace(expected, actual);
            return new QuickFix(
                "Update expected value from " + expected + " to " + actual,
                fixedCode,
                FixType.UPDATE_ASSERTION,
                85
            );
        }
        
        return new QuickFix("Review assertion values", testCode, FixType.UPDATE_ASSERTION, 50);
    }

    private QuickFix suggestNullPointerFix(String testCode, String failureMessage) {
        Pattern fieldPattern = Pattern.compile("\"(\\w+)\" is null");
        Matcher matcher = fieldPattern.matcher(failureMessage);
        
        if (matcher.find()) {
            String fieldName = matcher.group(1);
            String fixedCode = "    @Mock\n    private Object " + fieldName + ";\n\n" + testCode;
            return new QuickFix(
                "Add @Mock annotation for " + fieldName,
                fixedCode,
                FixType.ADD_MOCK,
                75
            );
        }
        
        return new QuickFix(
            "Initialize the null field or add @Mock annotation",
            "@Mock\nprivate Object field;\n\n" + testCode,
            FixType.INITIALIZE_FIELD,
            60
        );
    }

    private QuickFix suggestMockFix(String testCode, String failureMessage) {
        return new QuickFix(
            "Review mock configuration and stubbing setup",
            "Mockito.when(mock.method()).thenReturn(value);\n\n" + testCode,
            FixType.ADD_STUBBING,
            70
        );
    }

    private String generateDisplayName(String methodName) {
        StringBuilder displayName = new StringBuilder();
        
        if (methodName.startsWith("get") || methodName.startsWith("is") || 
            methodName.startsWith("has") || methodName.startsWith("can")) {
            displayName.append("Should ");
        }
        
        for (int i = 0; i < methodName.length(); i++) {
            char c = methodName.charAt(i);
            if (Character.isUpperCase(c) && i > 0) {
                displayName.append(' ');
                displayName.append(Character.toLowerCase(c));
            } else if (i == 0) {
                displayName.append(Character.toLowerCase(c));
            } else {
                displayName.append(c);
            }
        }
        
        return displayName.toString();
    }

    private String generateTestMethodName(String methodName) {
        return "should" + Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
    }

    private String generateDefaultValue(String type) {
        return switch (type) {
            case "int", "Integer" -> "0";
            case "long", "Long" -> "0L";
            case "double", "Double" -> "0.0";
            case "float", "Float" -> "0.0f";
            case "boolean", "Boolean" -> "false";
            case "String" -> "\"test\"";
            default -> "null";
        };
    }

    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }
}
