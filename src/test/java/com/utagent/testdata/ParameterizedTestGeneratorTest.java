package com.utagent.testdata;

import com.utagent.model.MethodInfo;
import com.utagent.model.ParameterInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ParameterizedTestGenerator Tests")
class ParameterizedTestGeneratorTest {

    private ParameterizedTestGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ParameterizedTestGenerator();
    }

    @Nested
    @DisplayName("Parameterized Test Generation")
    class ParameterizedTestGeneration {

        @Test
        @DisplayName("Should generate parameterized test method")
        void shouldGenerateParameterizedTestMethod() {
            MethodInfo method = createTestMethod("add", "int", 
                List.of(
                    new ParameterInfo("a", "int", false),
                    new ParameterInfo("b", "int", false)
                ));
            
            ParameterizedTestCase testCase = generator.generate(method);
            
            assertThat(testCase).isNotNull();
            assertThat(testCase.getTestMethodName()).isEqualTo("shouldAddWithVariousInputs");
        }

        @Test
        @DisplayName("Should generate test data sources")
        void shouldGenerateTestDataSources() {
            MethodInfo method = createTestMethod("calculate", "int",
                List.of(new ParameterInfo("value", "int", false)));
            
            ParameterizedTestCase testCase = generator.generate(method);
            
            assertThat(testCase.getDataSource()).isNotEmpty();
            assertThat(testCase.getDataSource()).contains("@ValueSource");
        }

        @Test
        @DisplayName("Should generate CsvSource for multiple simple types")
        void shouldGenerateCsvSourceForMultipleSimpleTypes() {
            MethodInfo method = createTestMethod("concat", "String",
                List.of(
                    new ParameterInfo("a", "String", false),
                    new ParameterInfo("b", "String", false)
                ));
            
            ParameterizedTestCase testCase = generator.generate(method);
            
            assertThat(testCase.getDataSource()).contains("@CsvSource");
        }

        @Test
        @DisplayName("Should generate ValueSource for single parameter")
        void shouldGenerateValueSourceForSingleParameter() {
            MethodInfo method = createTestMethod("isPositive", "boolean",
                List.of(new ParameterInfo("value", "int", false)));
            
            ParameterizedTestCase testCase = generator.generate(method);
            
            assertThat(testCase.getDataSource()).contains("@ValueSource");
        }
    }

    @Nested
    @DisplayName("Test Data Generation")
    class TestDataGeneration {

        @Test
        @DisplayName("Should generate boundary values for numeric parameters")
        void shouldGenerateBoundaryValuesForNumericParameters() {
            MethodInfo method = createTestMethod("process", "int",
                List.of(new ParameterInfo("value", "int", false)));
            
            ParameterizedTestCase testCase = generator.generate(method);
            String testCode = testCase.generateTestCode();
            
            assertThat(testCode).contains("Integer.MIN_VALUE", "Integer.MAX_VALUE");
        }

        @Test
        @DisplayName("Should generate test values for String parameters")
        void shouldGenerateTestValuesForStringParameters() {
            MethodInfo method = createTestMethod("process", "String",
                List.of(new ParameterInfo("value", "String", false)));
            
            ParameterizedTestCase testCase = generator.generate(method);
            String testCode = testCase.generateTestCode();
            
            assertThat(testCode).contains("strings");
        }

        @Test
        @DisplayName("Should generate both true and false for boolean parameters")
        void shouldGenerateBothTrueAndFalseForBooleanParameters() {
            MethodInfo method = createTestMethod("process", "void",
                List.of(new ParameterInfo("flag", "boolean", false)));
            
            ParameterizedTestCase testCase = generator.generate(method);
            String testCode = testCase.generateTestCode();
            
            assertThat(testCode).contains("booleans = {true, false}");
        }
    }

    @Nested
    @DisplayName("Code Generation")
    class CodeGeneration {

        @Test
        @DisplayName("Should generate complete test method")
        void shouldGenerateCompleteTestMethod() {
            MethodInfo method = createTestMethod("add", "int",
                List.of(
                    new ParameterInfo("a", "int", false),
                    new ParameterInfo("b", "int", false)
                ));
            
            ParameterizedTestCase testCase = generator.generate(method);
            String testCode = testCase.generateTestCode();
            
            assertThat(testCode).contains("@ParameterizedTest");
            assertThat(testCode).contains("void shouldAddWithVariousInputs");
        }

        @Test
        @DisplayName("Should generate stream provider method for MethodSource")
        void shouldGenerateStreamProviderMethodForMethodSource() {
            MethodInfo method = createTestMethod("complexProcess", "void",
                List.of(
                    new ParameterInfo("data", "java.util.List", false),
                    new ParameterInfo("config", "java.util.Map", false)
                ));
            
            ParameterizedTestCase testCase = generator.generate(method);
            String providerCode = testCase.generateProviderMethod();
            
            assertThat(providerCode).contains("static Stream<Arguments>");
            assertThat(providerCode).contains("arguments(");
        }

        @Test
        @DisplayName("Should generate display name pattern")
        void shouldGenerateDisplayNamePattern() {
            MethodInfo method = createTestMethod("add", "int",
                List.of(
                    new ParameterInfo("a", "int", false),
                    new ParameterInfo("b", "int", false)
                ));
            
            ParameterizedTestCase testCase = generator.generate(method);
            String testCode = testCase.generateTestCode();
            
            assertThat(testCode).contains("@DisplayName");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle method with no parameters")
        void shouldHandleMethodWithNoParameters() {
            MethodInfo method = createTestMethod("getValue", "String", new ArrayList<>());
            
            ParameterizedTestCase testCase = generator.generate(method);
            
            assertThat(testCase).isNotNull();
            assertThat(testCase.isParameterized()).isFalse();
        }

        @Test
        @DisplayName("Should handle multiple parameters of different types")
        void shouldHandleMultipleParametersOfDifferentTypes() {
            MethodInfo method = createTestMethod("process", "void",
                List.of(
                    new ParameterInfo("name", "String", false),
                    new ParameterInfo("value", "int", false),
                    new ParameterInfo("enabled", "boolean", false)
                ));
            
            ParameterizedTestCase testCase = generator.generate(method);
            String testCode = testCase.generateTestCode();
            
            assertThat(testCode).isNotEmpty();
        }
    }

    private MethodInfo createTestMethod(String name, String returnType, List<ParameterInfo> params) {
        return new MethodInfo(
            name,
            returnType,
            params,
            new ArrayList<>(),
            null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false
        );
    }
}
