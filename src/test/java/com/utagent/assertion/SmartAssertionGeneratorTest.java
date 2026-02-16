package com.utagent.assertion;

import com.utagent.model.MethodInfo;
import com.utagent.model.ParameterInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SmartAssertionGenerator Tests")
class SmartAssertionGeneratorTest {

    private SmartAssertionGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new SmartAssertionGenerator();
    }

    @Nested
    @DisplayName("Basic Assertion Generation")
    class BasicAssertionGeneration {

        @Test
        @DisplayName("Should generate assertion for boolean return type")
        void shouldGenerateAssertionForBooleanReturnType() {
            MethodInfo method = createMethod("isValid", "boolean");
            
            String assertion = generator.generateAssertion(method);
            
            assertThat(assertion).contains("isTrue()");
        }

        @Test
        @DisplayName("Should generate assertion for String return type")
        void shouldGenerateAssertionForStringReturnType() {
            MethodInfo method = createMethod("getName", "String");
            
            String assertion = generator.generateAssertion(method);
            
            assertThat(assertion).contains("isNotNull()");
        }

        @Test
        @DisplayName("Should generate assertion for numeric return type")
        void shouldGenerateAssertionForNumericReturnType() {
            MethodInfo method = createMethod("getCount", "int");
            
            String assertion = generator.generateAssertion(method);
            
            assertThat(assertion).contains("assertThat(result)");
        }

        @Test
        @DisplayName("Should generate assertion for void return type")
        void shouldGenerateAssertionForVoidReturnType() {
            MethodInfo method = createMethod("save", "void");
            
            String assertion = generator.generateAssertion(method);
            
            assertThat(assertion).isEmpty();
        }
    }

    @Nested
    @DisplayName("Collection Assertion Generation")
    class CollectionAssertionGeneration {

        @Test
        @DisplayName("Should generate assertion for List return type")
        void shouldGenerateAssertionForListReturnType() {
            MethodInfo method = createMethod("getAll", "java.util.List");
            
            String assertion = generator.generateAssertion(method);
            
            assertThat(assertion).contains("assertThat(result)");
            assertThat(assertion).containsAnyOf("isNotEmpty", "hasSize", "isNotNull");
        }

        @Test
        @DisplayName("Should generate assertion for Map return type")
        void shouldGenerateAssertionForMapReturnType() {
            MethodInfo method = createMethod("getMappings", "java.util.Map");
            
            String assertion = generator.generateAssertion(method);
            
            assertThat(assertion).contains("assertThat(result)");
        }
    }

    @Nested
    @DisplayName("Custom Object Assertion Generation")
    class CustomObjectAssertionGeneration {

        @Test
        @DisplayName("Should generate assertion for custom object return type")
        void shouldGenerateAssertionForCustomObjectReturnType() {
            MethodInfo method = createMethod("getUser", "com.example.User");
            
            String assertion = generator.generateAssertion(method);
            
            assertThat(assertion).contains("isNotNull()");
        }

        @Test
        @DisplayName("Should generate field assertions for known types")
        void shouldGenerateFieldAssertionsForKnownTypes() {
            MethodInfo method = createMethod("getUser", "com.example.User");
            
            List<String> assertions = generator.generateFieldAssertions(method, "result");
            
            assertThat(assertions).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Exception Assertion Generation")
    class ExceptionAssertionGeneration {

        @Test
        @DisplayName("Should generate exception assertion for throwing method")
        void shouldGenerateExceptionAssertionForThrowingMethod() {
            MethodInfo method = new MethodInfo(
                "process",
                "void",
                new ArrayList<>(),
                new ArrayList<>(),
                null, 0, 0, List.of("java.lang.IllegalArgumentException"),
                false, false, false, true, false, false
            );
            
            String assertion = generator.generateExceptionAssertion(method);
            
            assertThat(assertion).contains("assertThrows");
            assertThat(assertion).contains("IllegalArgumentException");
        }

        @Test
        @DisplayName("Should return empty for non-throwing method")
        void shouldReturnEmptyForNonThrowingMethod() {
            MethodInfo method = createMethod("process", "void");
            
            String assertion = generator.generateExceptionAssertion(method);
            
            assertThat(assertion).isEmpty();
        }
    }

    @Nested
    @DisplayName("Assertion Message Enhancement")
    class AssertionMessageEnhancement {

        @Test
        @DisplayName("Should add descriptive message to assertion")
        void shouldAddDescriptiveMessageToAssertion() {
            MethodInfo method = createMethod("getCount", "int");
            
            String assertion = generator.generateAssertionWithMessage(method, "Count should be positive");
            
            assertThat(assertion).contains("Count should be positive");
        }

        @Test
        @DisplayName("Should generate auto message from method name")
        void shouldGenerateAutoMessageFromMethodName() {
            MethodInfo method = createMethod("getUserName", "String");
            
            String message = generator.generateAutoMessage(method);
            
            assertThat(message).contains("user name");
        }
    }

    @Nested
    @DisplayName("Behavior Verification")
    class BehaviorVerification {

        @Test
        @DisplayName("Should generate behavior verification for mock interactions")
        void shouldGenerateBehaviorVerificationForMockInteractions() {
            MethodInfo method = createMethod("process", "void");
            
            String verification = generator.generateBehaviorVerification(method, "service");
            
            assertThat(verification).contains("verify");
        }
    }

    private MethodInfo createMethod(String name, String returnType) {
        return new MethodInfo(
            name,
            returnType,
            new ArrayList<>(),
            new ArrayList<>(),
            null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false
        );
    }
}
