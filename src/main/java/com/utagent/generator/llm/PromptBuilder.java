package com.utagent.generator.llm;

import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageInfo;
import com.utagent.model.MethodInfo;
import com.utagent.parser.FrameworkType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PromptBuilder {

    public String buildTestGenerationPrompt(ClassInfo classInfo, Set<FrameworkType> frameworks) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Generate comprehensive JUnit 5 unit tests for the following Java class.\n\n");
        
        prompt.append("## Framework Context\n");
        for (FrameworkType framework : frameworks) {
            prompt.append("- ").append(framework.getDisplayName()).append("\n");
        }
        prompt.append("\n");
        
        prompt.append("## Class Information\n");
        prompt.append("- Package: ").append(classInfo.packageName()).append("\n");
        prompt.append("- Class Name: ").append(classInfo.className()).append("\n");
        prompt.append("- Full Name: ").append(classInfo.fullyQualifiedName()).append("\n");
        
        if (classInfo.superClass() != null) {
            prompt.append("- Extends: ").append(classInfo.superClass()).append("\n");
        }
        
        if (!classInfo.interfaces().isEmpty()) {
            prompt.append("- Implements: ").append(String.join(", ", classInfo.interfaces())).append("\n");
        }
        prompt.append("\n");
        
        prompt.append("## Annotations\n");
        for (var annotation : classInfo.annotations()) {
            prompt.append("- @").append(annotation.name());
            if (!annotation.attributes().isEmpty()) {
                prompt.append("(").append(annotation.attributes()).append(")");
            }
            prompt.append("\n");
        }
        prompt.append("\n");
        
        prompt.append("## Fields (Dependencies)\n");
        for (var field : classInfo.fields()) {
            prompt.append("- ").append(field.type()).append(" ").append(field.name());
            if (field.isDependencyInjection()) {
                prompt.append(" (injected)");
            }
            prompt.append("\n");
        }
        prompt.append("\n");
        
        prompt.append("## Methods to Test\n");
        for (MethodInfo method : classInfo.methods()) {
            if (!method.isPrivate() && !method.isAbstract()) {
                prompt.append("- ").append(method.getSignature());
                prompt.append(" : ").append(method.returnType());
                if (!method.annotations().isEmpty()) {
                    prompt.append(" ").append(method.annotations().stream()
                        .map(a -> "@" + a.name())
                        .collect(Collectors.joining(" ")));
                }
                prompt.append("\n");
            }
        }
        prompt.append("\n");
        
        prompt.append("## Requirements\n");
        prompt.append("1. Use JUnit 5 (@Test, @DisplayName, @BeforeEach, etc.)\n");
        prompt.append("2. Use Mockito for mocking dependencies\n");
        prompt.append("3. Follow Given-When-Then structure\n");
        prompt.append("4. Include positive and negative test cases\n");
        prompt.append("5. Test edge cases and boundary conditions\n");
        prompt.append("6. Use appropriate assertions\n");
        
        if (frameworks.contains(FrameworkType.SPRING_BOOT)) {
            prompt.append("7. Use @SpringBootTest or @WebMvcTest as appropriate\n");
            prompt.append("8. Use @MockBean for Spring Boot mocking\n");
        }
        
        if (frameworks.contains(FrameworkType.MYBATIS) || frameworks.contains(FrameworkType.MYBATIS_PLUS)) {
            prompt.append("7. Use @MybatisTest for MyBatis mapper tests\n");
            prompt.append("8. Test SQL operations with appropriate test data\n");
        }
        
        prompt.append("\n## Output Format\n");
        prompt.append("Generate only the Java test class code, no explanations.\n");
        prompt.append("The test class should be named ").append(classInfo.className()).append("Test.\n");
        
        return prompt.toString();
    }

    public String buildCoverageImprovementPrompt(ClassInfo classInfo, List<CoverageInfo> uncoveredInfo) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Generate additional JUnit 5 test cases to improve code coverage.\n\n");
        
        prompt.append("## Class: ").append(classInfo.fullyQualifiedName()).append("\n\n");
        
        prompt.append("## Uncovered Code Areas\n");
        for (CoverageInfo info : uncoveredInfo) {
            prompt.append("- Method: ").append(info.methodName());
            prompt.append(", Line: ").append(info.lineNumber());
            prompt.append(", Coverage: ").append(String.format("%.1f%%", info.getLineCoverageRate() * 100));
            prompt.append("\n");
        }
        prompt.append("\n");
        
        prompt.append("## Requirements\n");
        prompt.append("1. Focus on the uncovered lines and branches\n");
        prompt.append("2. Generate test cases that exercise those specific paths\n");
        prompt.append("3. Consider edge cases and error conditions\n");
        prompt.append("4. Use appropriate test data to reach uncovered code\n\n");
        
        prompt.append("## Output Format\n");
        prompt.append("Generate only the additional test methods, no class structure needed.\n");
        
        return prompt.toString();
    }

    public String buildMethodTestPrompt(ClassInfo classInfo, MethodInfo method, Set<FrameworkType> frameworks) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("Generate a comprehensive JUnit 5 test method for the following Java method.\n\n");
        
        prompt.append("## Class Context\n");
        prompt.append("- Class: ").append(classInfo.className()).append("\n");
        prompt.append("- Dependencies: ").append(classInfo.fields().stream()
            .filter(f -> f.isDependencyInjection())
            .map(f -> f.type() + " " + f.name())
            .collect(Collectors.joining(", "))).append("\n\n");
        
        prompt.append("## Method to Test\n");
        prompt.append("- Signature: ").append(method.getSignature()).append("\n");
        prompt.append("- Return Type: ").append(method.returnType()).append("\n");
        prompt.append("- Annotations: ").append(method.annotations().stream()
            .map(a -> "@" + a.name())
            .collect(Collectors.joining(" "))).append("\n");
        
        if (!method.thrownExceptions().isEmpty()) {
            prompt.append("- Throws: ").append(String.join(", ", method.thrownExceptions())).append("\n");
        }
        prompt.append("\n");
        
        prompt.append("## Requirements\n");
        prompt.append("1. Generate a single test method\n");
        prompt.append("2. Use @Test and @DisplayName annotations\n");
        prompt.append("3. Follow Given-When-Then structure\n");
        prompt.append("4. Mock dependencies using Mockito\n");
        prompt.append("5. Test the main success scenario\n");
        prompt.append("6. Include assertions to verify the result\n\n");
        
        prompt.append("## Output Format\n");
        prompt.append("Generate only the test method code.\n");
        
        return prompt.toString();
    }

    public String buildSystemPrompt() {
        return """
            You are an expert Java developer specializing in writing high-quality unit tests.
            
            Your expertise includes:
            - JUnit 5 testing framework
            - Mockito mocking framework
            - Spring Boot testing (@SpringBootTest, @WebMvcTest, @DataJpaTest)
            - MyBatis and MyBatis Plus testing
            - Test-driven development (TDD)
            - Code coverage optimization
            
            Guidelines for test generation:
            1. Write clean, readable, and maintainable test code
            2. Use descriptive test names and @DisplayName annotations
            3. Follow the Given-When-Then pattern
            4. Test one concept per test method
            5. Use appropriate assertions (assertEquals, assertNotNull, assertThrows, etc.)
            6. Mock external dependencies
            7. Handle edge cases and boundary conditions
            8. Consider null inputs and empty collections
            9. Test both success and failure scenarios
            10. Avoid code duplication in tests
            
            Output only valid Java code without explanations.
            """;
    }
}
