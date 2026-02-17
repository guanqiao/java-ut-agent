package com.utagent.ai;

import com.utagent.model.ClassInfo;
import com.utagent.model.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AITestGenerator Tests")
class AITestGeneratorTest {

    private AITestGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new AITestGenerator();
    }

    @Nested
    @DisplayName("Test Generation")
    class TestGeneration {

        @Test
        @DisplayName("Should generate test with context awareness")
        void shouldGenerateTestWithContextAwareness() {
            ClassInfo classInfo = createTestClassInfo();
            GenerationContext context = GenerationContext.builder()
                .focusArea("business logic")
                .targetCoverage(0.9)
                .build();
            
            AIGenerationResult result = generator.generateWithContext(classInfo, context);
            
            assertThat(result).isNotNull();
            assertThat(result.getGeneratedCode()).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate multiple test variants")
        void shouldGenerateMultipleTestVariants() {
            MethodInfo method = createTestMethodInfo();
            
            List<AIGenerationResult> variants = generator.generateVariants(method, 3);
            
            assertThat(variants).hasSize(3);
            assertThat(variants.get(0).getGeneratedCode()).isNotEqualTo(variants.get(1).getGeneratedCode());
        }

        @Test
        @DisplayName("Should generate edge case tests")
        void shouldGenerateEdgeCaseTests() {
            MethodInfo method = createTestMethodInfo();
            
            AIGenerationResult result = generator.generateEdgeCaseTests(method);
            
            assertThat(result).isNotNull();
            assertThat(result.getTestType()).isEqualTo(TestType.EDGE_CASE);
        }

        @Test
        @DisplayName("Should generate negative tests")
        void shouldGenerateNegativeTests() {
            MethodInfo method = createTestMethodInfo();
            
            AIGenerationResult result = generator.generateNegativeTests(method);
            
            assertThat(result).isNotNull();
            assertThat(result.getTestType()).isEqualTo(TestType.NEGATIVE);
        }
    }

    @Nested
    @DisplayName("Smart Suggestions")
    class SmartSuggestions {

        @Test
        @DisplayName("Should suggest missing test cases")
        void shouldSuggestMissingTestCases() {
            ClassInfo classInfo = createTestClassInfo();
            List<String> existingTests = List.of("testAdd", "testSubtract");
            
            List<TestSuggestion> suggestions = generator.suggestMissingTests(classInfo, existingTests);
            
            assertThat(suggestions).isNotEmpty();
        }

        @Test
        @DisplayName("Should suggest improved assertions")
        void shouldSuggestImprovedAssertions() {
            String testCode = "assertEquals(5, result);";
            
            List<AssertionSuggestion> suggestions = generator.suggestBetterAssertions(testCode);
            
            assertThat(suggestions).isNotEmpty();
        }

        @Test
        @DisplayName("Should suggest test refactoring")
        void shouldSuggestTestRefactoring() {
            String testCode = """
                @Test
                void test() {
                    Calculator calc = new Calculator();
                    int result = calc.add(1, 2);
                    assertEquals(3, result);
                }
                """;
            
            List<RefactoringSuggestion> suggestions = generator.suggestRefactoring(testCode);
            
            assertThat(suggestions).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Learning and Adaptation")
    class LearningAndAdaptation {

        @Test
        @DisplayName("Should learn from test patterns")
        void shouldLearnFromTestPatterns() {
            List<String> patterns = List.of(
                "should{MethodName}",
                "given{Input}_when{Action}_then{Result}"
            );
            
            generator.learnPatterns(patterns);
            
            assertThat(generator.getLearnedPatternCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should adapt to project style")
        void shouldAdaptToProjectStyle() {
            ProjectStyle style = ProjectStyle.builder()
                .namingConvention("should{MethodName}")
                .useAssertJ(true)
                .useNestedTests(true)
                .build();
            
            generator.adaptToStyle(style);
            
            AIGenerationResult result = generator.generate(createTestMethodInfo());
            assertThat(result.getGeneratedCode()).contains("assertThat");
        }
    }

    @Nested
    @DisplayName("Quality Integration")
    class QualityIntegration {

        @Test
        @DisplayName("Should generate tests meeting quality threshold")
        void shouldGenerateTestsMeetingQualityThreshold() {
            MethodInfo method = createTestMethodInfo();
            QualityThreshold threshold = new QualityThreshold(0.8, 0.7);
            
            AIGenerationResult result = generator.generateWithQualityThreshold(method, threshold);
            
            assertThat(result.getQualityScore()).isGreaterThanOrEqualTo(0.8);
        }

        @Test
        @DisplayName("Should iterate until quality met")
        void shouldIterateUntilQualityMet() {
            MethodInfo method = createTestMethodInfo();
            QualityThreshold threshold = new QualityThreshold(0.9, 0.8);
            
            AIGenerationResult result = generator.generateIteratively(method, threshold, 5);
            
            assertThat(result.getIterations()).isGreaterThan(0);
        }
    }

    private ClassInfo createTestClassInfo() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("add", "int", new ArrayList<>(), new ArrayList<>(), null, 0, 0, new ArrayList<>(), false, false, false, true, false, false));
        methods.add(new MethodInfo("subtract", "int", new ArrayList<>(), new ArrayList<>(), null, 0, 0, new ArrayList<>(), false, false, false, true, false, false));
        methods.add(new MethodInfo("multiply", "int", new ArrayList<>(), new ArrayList<>(), null, 0, 0, new ArrayList<>(), false, false, false, true, false, false));
        
        return new ClassInfo(
            "com.example",
            "Calculator",
            "com.example.Calculator",
            methods,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );
    }

    private MethodInfo createTestMethodInfo() {
        return new MethodInfo(
            "add",
            "int",
            new ArrayList<>(),
            new ArrayList<>(),
            null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false
        );
    }
}
