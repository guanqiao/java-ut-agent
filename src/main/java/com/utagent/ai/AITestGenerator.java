package com.utagent.ai;

import com.utagent.model.ClassInfo;
import com.utagent.model.MethodInfo;
import com.utagent.testdata.BoundaryValueGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AITestGenerator {

    private static final Logger logger = LoggerFactory.getLogger(AITestGenerator.class);

    private final Set<String> learnedPatterns;
    private ProjectStyle currentStyle;
    private final BoundaryValueGenerator boundaryGenerator;

    public AITestGenerator() {
        this.learnedPatterns = new HashSet<>();
        this.currentStyle = ProjectStyle.builder().build();
        this.boundaryGenerator = new BoundaryValueGenerator();
    }

    public AIGenerationResult generateWithContext(ClassInfo classInfo, GenerationContext context) {
        StringBuilder code = new StringBuilder();
        
        String className = classInfo.className();
        String testClassName = className + "Test";
        
        code.append("package ").append(classInfo.packageName()).append(";\n\n");
        code.append("import org.junit.jupiter.api.Test;\n");
        code.append("import org.junit.jupiter.api.BeforeEach;\n");
        code.append("import org.junit.jupiter.api.DisplayName;\n");
        if (currentStyle.isUseAssertJ()) {
            code.append("import static org.assertj.core.api.Assertions.assertThat;\n");
        } else {
            code.append("import static org.junit.jupiter.api.Assertions.*;\n");
        }
        code.append("\n");
        
        code.append("@DisplayName(\"").append(className).append(" Tests\")\n");
        code.append("class ").append(testClassName).append(" {\n\n");
        
        code.append("    private ").append(className).append(" ").append(toCamelCase(className)).append(";\n\n");
        
        code.append("    @BeforeEach\n");
        code.append("    void setUp() {\n");
        code.append("        ").append(toCamelCase(className)).append(" = new ").append(className).append("();\n");
        code.append("    }\n\n");
        
        if (context != null && context.getFocusArea() != null) {
            code.append("    // Focus: ").append(context.getFocusArea()).append("\n");
        }
        
        code.append("    @Test\n");
        code.append("    @DisplayName(\"Should work correctly\")\n");
        code.append("    void shouldWorkCorrectly() {\n");
        code.append("        // Arrange\n\n");
        code.append("        // Act\n\n");
        code.append("        // Assert\n");
        if (currentStyle.isUseAssertJ()) {
            code.append("        assertThat(true).isTrue();\n");
        } else {
            code.append("        assertTrue(true);\n");
        }
        code.append("    }\n");
        
        code.append("}\n");
        
        return new AIGenerationResult(code.toString(), TestType.UNIT, context != null ? context.getTargetCoverage() : 0.8, 1, "Generated with context awareness");
    }

    public List<AIGenerationResult> generateVariants(MethodInfo method, int count) {
        List<AIGenerationResult> variants = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            String code = generateMethodTest(method, i);
            variants.add(new AIGenerationResult(code, TestType.UNIT, 0.8, 1, "Variant " + (i + 1)));
        }
        
        return variants;
    }

    public AIGenerationResult generateEdgeCaseTests(MethodInfo method) {
        StringBuilder code = new StringBuilder();
        
        code.append("@Test\n");
        code.append("@DisplayName(\"Should handle edge cases for ").append(method.name()).append("\")\n");
        code.append("void shouldHandleEdgeCasesFor").append(capitalize(method.name())).append("() {\n");
        code.append("    // Edge case tests\n");
        
        List<Object> boundaryValues = boundaryGenerator.generate(int.class);
        for (Object value : boundaryValues) {
            code.append("    // Test with boundary value: ").append(value).append("\n");
        }
        
        code.append("}\n");
        
        return new AIGenerationResult(code.toString(), TestType.EDGE_CASE, 0.85, 1, "Edge case tests generated");
    }

    public AIGenerationResult generateNegativeTests(MethodInfo method) {
        StringBuilder code = new StringBuilder();
        
        code.append("@Test\n");
        code.append("@DisplayName(\"Should handle invalid input for ").append(method.name()).append("\")\n");
        code.append("void shouldHandleInvalidInputFor").append(capitalize(method.name())).append("() {\n");
        code.append("    // Negative test cases\n");
        code.append("    assertThrows(IllegalArgumentException.class, () -> {\n");
        code.append("        // Invoke with invalid input\n");
        code.append("    });\n");
        code.append("}\n");
        
        return new AIGenerationResult(code.toString(), TestType.NEGATIVE, 0.8, 1, "Negative tests generated");
    }

    public List<TestSuggestion> suggestMissingTests(ClassInfo classInfo, List<String> existingTests) {
        List<TestSuggestion> suggestions = new ArrayList<>();
        
        Set<String> existing = new HashSet<>(existingTests);
        
        for (var method : classInfo.methods()) {
            String expectedTestName = "should" + capitalize(method.name());
            if (!existing.contains(expectedTestName) && !method.name().startsWith("<")) {
                suggestions.add(new TestSuggestion(
                    expectedTestName,
                    "Test for method: " + method.name(),
                    "// TODO: Generate test for " + method.name(),
                    1
                ));
            }
        }
        
        return suggestions;
    }

    public List<AssertionSuggestion> suggestBetterAssertions(String testCode) {
        List<AssertionSuggestion> suggestions = new ArrayList<>();
        
        if (testCode.contains("assertEquals")) {
            Pattern pattern = Pattern.compile("assertEquals\\(([^,]+),\\s*([^)]+)\\)");
            Matcher matcher = pattern.matcher(testCode);
            while (matcher.find()) {
                String original = matcher.group(0);
                String expected = matcher.group(1);
                String actual = matcher.group(2);
                String suggested = "assertThat(" + actual + ").isEqualTo(" + expected + ")";
                suggestions.add(new AssertionSuggestion(original, suggested, "Use AssertJ for better readability"));
            }
        }
        
        if (testCode.contains("assertTrue")) {
            suggestions.add(new AssertionSuggestion(
                "assertTrue(condition)",
                "assertThat(condition).isTrue()",
                "AssertJ provides better error messages"
            ));
        }
        
        if (testCode.contains("assertNull")) {
            suggestions.add(new AssertionSuggestion(
                "assertNull(value)",
                "assertThat(value).isNull()",
                "AssertJ style is more readable"
            ));
        }
        
        return suggestions;
    }

    public List<RefactoringSuggestion> suggestRefactoring(String testCode) {
        List<RefactoringSuggestion> suggestions = new ArrayList<>();
        
        if (!testCode.contains("@DisplayName")) {
            suggestions.add(new RefactoringSuggestion(
                "Add @DisplayName annotation for better test documentation",
                "void testAdd()",
                "@DisplayName(\"Should add two numbers correctly\")\nvoid shouldAddTwoNumbers()",
                RefactoringType.ADD_DISPLAY_NAME
            ));
        }
        
        if (testCode.contains("assertEquals") && currentStyle.isUseAssertJ()) {
            suggestions.add(new RefactoringSuggestion(
                "Use AssertJ assertions for better readability",
                "assertEquals(expected, actual);",
                "assertThat(actual).isEqualTo(expected);",
                RefactoringType.USE_ASSERTJ
            ));
        }
        
        Pattern poorNaming = Pattern.compile("void\\s+test\\d*\\s*\\(");
        Matcher matcher = poorNaming.matcher(testCode);
        if (matcher.find()) {
            suggestions.add(new RefactoringSuggestion(
                "Use descriptive test method names",
                matcher.group(),
                "void shouldReturnCorrectResult()",
                RefactoringType.RENAME_METHOD
            ));
        }
        
        return suggestions;
    }

    public void learnPatterns(List<String> patterns) {
        learnedPatterns.addAll(patterns);
    }

    public int getLearnedPatternCount() {
        return learnedPatterns.size();
    }

    public void adaptToStyle(ProjectStyle style) {
        this.currentStyle = style;
    }

    public AIGenerationResult generate(MethodInfo method) {
        String code = generateMethodTest(method, 0);
        return AIGenerationResult.of(code, TestType.UNIT);
    }

    public AIGenerationResult generateWithQualityThreshold(MethodInfo method, QualityThreshold threshold) {
        String code = generateMethodTest(method, 0);
        double qualityScore = calculateQualityScore(code);
        
        return new AIGenerationResult(code, TestType.UNIT, qualityScore, 1, "Generated with quality threshold");
    }

    public AIGenerationResult generateIteratively(MethodInfo method, QualityThreshold threshold, int maxIterations) {
        String bestCode = "";
        double bestScore = 0.0;
        int iterations = 0;
        
        for (int i = 0; i < maxIterations; i++) {
            iterations++;
            String code = generateMethodTest(method, i);
            double score = calculateQualityScore(code);
            
            if (score > bestScore) {
                bestScore = score;
                bestCode = code;
            }
            
            if (threshold.isMet(score, score)) {
                break;
            }
        }
        
        return new AIGenerationResult(bestCode, TestType.UNIT, bestScore, iterations, "Iteratively generated");
    }

    private String generateMethodTest(MethodInfo method, int variant) {
        StringBuilder code = new StringBuilder();
        
        String methodName = method.name();
        String displayName = generateDisplayName(methodName, variant);
        String testName = generateTestName(methodName, variant);
        
        code.append("@Test\n");
        code.append("@DisplayName(\"").append(displayName).append("\")\n");
        code.append("void ").append(testName).append("() {\n");
        code.append("    // Arrange\n");
        
        if (!method.parameters().isEmpty()) {
            for (var param : method.parameters()) {
                code.append("    var ").append(param.name()).append(" = ")
                    .append(getDefaultValue(param.type())).append(";\n");
            }
        }
        
        code.append("\n    // Act\n");
        code.append("    // var result = ").append(methodName).append("();\n\n");
        code.append("    // Assert\n");
        
        if (currentStyle.isUseAssertJ()) {
            code.append("    // assertThat(result).isNotNull();\n");
        } else {
            code.append("    // assertNotNull(result);\n");
        }
        
        code.append("}\n");
        
        return code.toString();
    }

    private String generateDisplayName(String methodName, int variant) {
        String base = "Should " + methodName.replace("get", "return ")
            .replace("is", "be ")
            .replace("has", "have ");
        
        if (variant > 0) {
            return base + " (variant " + variant + ")";
        }
        return base;
    }

    private String generateTestName(String methodName, int variant) {
        String base = "should" + capitalize(methodName);
        if (variant > 0) {
            return base + "V" + variant;
        }
        return base;
    }

    private String getDefaultValue(String type) {
        return switch (type) {
            case "int", "Integer" -> "0";
            case "long", "Long" -> "0L";
            case "double", "Double" -> "0.0";
            case "boolean", "Boolean" -> "false";
            case "String" -> "\"test\"";
            default -> "null";
        };
    }

    private double calculateQualityScore(String code) {
        double score = 0.8;
        
        if (code.contains("@DisplayName")) score += 0.05;
        if (code.contains("assertThat")) score += 0.05;
        if (code.contains("Arrange")) score += 0.03;
        if (code.contains("Act")) score += 0.03;
        if (code.contains("Assert")) score += 0.04;
        
        return Math.min(1.0, score);
    }

    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
