package com.utagent.maintenance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TestFailureAutoFixer Tests")
class TestFailureAutoFixerTest {

    private TestFailureAutoFixer fixer;

    @BeforeEach
    void setUp() {
        fixer = new TestFailureAutoFixer();
    }

    @Nested
    @DisplayName("Failure Analysis")
    class FailureAnalysisTests {

        @Test
        @DisplayName("Should analyze assertion failure")
        void shouldAnalyzeAssertionFailure() {
            TestFailure failure = new TestFailure(
                "CalculatorTest",
                "shouldAddTwoNumbers",
                "org.opentest4j.AssertionFailedError: expected: <5> but was: <4>",
                null
            );
            
            FailureAnalysis analysis = fixer.analyzeFailure(failure);
            
            assertThat(analysis).isNotNull();
            assertThat(analysis.getFailureType()).isEqualTo(FailureType.ASSERTION_FAILURE);
        }

        @Test
        @DisplayName("Should analyze null pointer exception")
        void shouldAnalyzeNullPointerException() {
            TestFailure failure = new TestFailure(
                "ServiceTest",
                "shouldProcessData",
                "java.lang.NullPointerException",
                "at com.example.Service.process(Service.java:42)"
            );
            
            FailureAnalysis analysis = fixer.analyzeFailure(failure);
            
            assertThat(analysis).isNotNull();
            assertThat(analysis.getFailureType()).isEqualTo(FailureType.NULL_POINTER);
        }

        @Test
        @DisplayName("Should analyze mock exception")
        void shouldAnalyzeMockException() {
            TestFailure failure = new TestFailure(
                "ControllerTest",
                "shouldHandleRequest",
                "org.mockito.exceptions.misusing.UnfinishedStubbingException",
                null
            );
            
            FailureAnalysis analysis = fixer.analyzeFailure(failure);
            
            assertThat(analysis).isNotNull();
            assertThat(analysis.getFailureType()).isEqualTo(FailureType.MOCK_CONFIGURATION);
        }

        @Test
        @DisplayName("Should analyze timeout exception")
        void shouldAnalyzeTimeoutException() {
            TestFailure failure = new TestFailure(
                "IntegrationTest",
                "shouldCompleteWithinTimeout",
                "org.junit.jupiter.api.timeout.TimeoutException",
                null
            );
            
            FailureAnalysis analysis = fixer.analyzeFailure(failure);
            
            assertThat(analysis).isNotNull();
            assertThat(analysis.getFailureType()).isEqualTo(FailureType.TIMEOUT);
        }
    }

    @Nested
    @DisplayName("Fix Suggestion Generation")
    class FixSuggestionTests {

        @Test
        @DisplayName("Should suggest fix for assertion failure")
        void shouldSuggestFixForAssertionFailure() {
            TestFailure failure = new TestFailure(
                "CalculatorTest",
                "shouldAddTwoNumbers",
                "org.opentest4j.AssertionFailedError: expected: <5> but was: <4>",
                null
            );
            
            FixSuggestion suggestion = fixer.suggestFix(failure);
            
            assertThat(suggestion).isNotNull();
            assertThat(suggestion.getDescription()).containsIgnoringCase("assertion");
            assertThat(suggestion.getConfidence()).isGreaterThan(0.0);
        }

        @Test
        @DisplayName("Should suggest fix for null pointer")
        void shouldSuggestFixForNullPointer() {
            TestFailure failure = new TestFailure(
                "ServiceTest",
                "shouldProcessData",
                "java.lang.NullPointerException",
                "at com.example.Service.process(Service.java:42)"
            );
            
            FixSuggestion suggestion = fixer.suggestFix(failure);
            
            assertThat(suggestion).isNotNull();
            assertThat(suggestion.getDescription()).containsAnyOf("null", "Null", "initialize", "Initialize");
        }

        @Test
        @DisplayName("Should suggest fix for missing mock setup")
        void shouldSuggestFixForMissingMockSetup() {
            TestFailure failure = new TestFailure(
                "RepositoryTest",
                "shouldFindById",
                "java.lang.NullPointerException: repository was null",
                null
            );
            
            FixSuggestion suggestion = fixer.suggestFix(failure);
            
            assertThat(suggestion).isNotNull();
            assertThat(suggestion.getSuggestedCode()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Auto Fix Generation")
    class AutoFixTests {

        @Test
        @DisplayName("Should generate fixed test code for assertion mismatch")
        void shouldGenerateFixedTestCodeForAssertionMismatch() {
            String originalTest = """
                @Test
                void shouldAddTwoNumbers() {
                    Calculator calc = new Calculator();
                    int result = calc.add(2, 2);
                    assertEquals(5, result);
                }
                """;
            
            TestFailure failure = new TestFailure(
                "CalculatorTest",
                "shouldAddTwoNumbers",
                "expected: <5> but was: <4>",
                null
            );
            
            String fixedCode = fixer.generateFixedCode(originalTest, failure);
            
            assertThat(fixedCode).isNotEmpty();
        }

        @Test
        @DisplayName("Should add missing mock initialization")
        void shouldAddMissingMockInitialization() {
            String originalTest = """
                class TestClass {
                @Test
                void shouldFindById() {
                    Optional<User> result = repository.findById(1L);
                    assertTrue(result.isPresent());
                }
                }
                """;
            
            TestFailure failure = new TestFailure(
                "RepositoryTest",
                "shouldFindById",
                "java.lang.NullPointerException: repository",
                null
            );
            
            String fixedCode = fixer.generateFixedCode(originalTest, failure);
            
            assertThat(fixedCode).isNotEmpty();
        }

        @Test
        @DisplayName("Should add missing stub setup")
        void shouldAddMissingStubSetup() {
            String originalTest = """
                @Test
                void shouldGetUserName() {
                    User user = service.getUser(1L);
                    assertEquals("John", user.getName());
                }
                """;
            
            TestFailure failure = new TestFailure(
                "ServiceTest",
                "shouldGetUserName",
                "java.lang.NullPointerException: user is null",
                null
            );
            
            String fixedCode = fixer.generateFixedCode(originalTest, failure);
            
            assertThat(fixedCode).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Batch Processing")
    class BatchProcessingTests {

        @Test
        @DisplayName("Should process multiple failures")
        void shouldProcessMultipleFailures() {
            List<TestFailure> failures = List.of(
                new TestFailure("Test1", "test1", "AssertionFailedError", null),
                new TestFailure("Test2", "test2", "NullPointerException", null)
            );
            
            List<FixSuggestion> suggestions = fixer.suggestFixes(failures);
            
            assertThat(suggestions).hasSize(2);
        }

        @Test
        @DisplayName("Should prioritize failures by severity")
        void shouldPrioritizeFailuresBySeverity() {
            List<TestFailure> failures = List.of(
                new TestFailure("Test1", "test1", "AssertionFailedError", null),
                new TestFailure("Test2", "test2", "OutOfMemoryError", null),
                new TestFailure("Test3", "test3", "NullPointerException", null)
            );
            
            List<TestFailure> prioritized = fixer.prioritizeFailures(failures);
            
            assertThat(prioritized).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Confidence Scoring")
    class ConfidenceScoringTests {

        @Test
        @DisplayName("Should have high confidence for simple assertion fix")
        void shouldHaveHighConfidenceForSimpleAssertionFix() {
            TestFailure failure = new TestFailure(
                "CalculatorTest",
                "shouldAddTwoNumbers",
                "expected: <5> but was: <4>",
                null
            );
            
            FixSuggestion suggestion = fixer.suggestFix(failure);
            
            assertThat(suggestion.getConfidence()).isGreaterThanOrEqualTo(0.7);
        }

        @Test
        @DisplayName("Should have lower confidence for complex failures")
        void shouldHaveLowerConfidenceForComplexFailures() {
            TestFailure failure = new TestFailure(
                "IntegrationTest",
                "shouldProcessComplexFlow",
                "java.lang.IllegalStateException: Complex error",
                null
            );
            
            FixSuggestion suggestion = fixer.suggestFix(failure);
            
            assertThat(suggestion.getConfidence()).isLessThan(0.9);
        }
    }
}
