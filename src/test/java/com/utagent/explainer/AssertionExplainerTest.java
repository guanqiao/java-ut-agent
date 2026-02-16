package com.utagent.explainer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AssertionExplainer Tests")
class AssertionExplainerTest {

    private AssertionExplainer explainer;

    @BeforeEach
    void setUp() {
        explainer = new AssertionExplainer();
    }

    @Nested
    @DisplayName("Basic Assertion Explanation")
    class BasicAssertionExplanation {

        @Test
        @DisplayName("Should explain equality assertion")
        void shouldExplainEqualityAssertion() {
            String assertion = "assertEquals(5, result)";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).isNotEmpty();
            assertThat(explanation).containsAnyOf("期望", "等于", "expected", "equal");
        }

        @Test
        @DisplayName("Should explain true assertion")
        void shouldExplainTrueAssertion() {
            String assertion = "assertTrue(isValid)";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).containsAnyOf("真", "true", "有效", "valid");
        }

        @Test
        @DisplayName("Should explain false assertion")
        void shouldExplainFalseAssertion() {
            String assertion = "assertFalse(isEmpty)";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).containsAnyOf("假", "false", "空", "empty");
        }

        @Test
        @DisplayName("Should explain null assertion")
        void shouldExplainNullAssertion() {
            String assertion = "assertNull(result)";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).containsAnyOf("null", "空", "null");
        }

        @Test
        @DisplayName("Should explain not null assertion")
        void shouldExplainNotNullAssertion() {
            String assertion = "assertNotNull(user)";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).containsAnyOf("不为", "not null", "null");
        }
    }

    @Nested
    @DisplayName("AssertJ Assertion Explanation")
    class AssertJAssertionExplanation {

        @Test
        @DisplayName("Should explain assertThat isTrue")
        void shouldExplainAssertThatIsTrue() {
            String assertion = "assertThat(result).isTrue()";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).isNotEmpty();
        }

        @Test
        @DisplayName("Should explain assertThat isFalse")
        void shouldExplainAssertThatIsFalse() {
            String assertion = "assertThat(result).isFalse()";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).isNotEmpty();
        }

        @Test
        @DisplayName("Should explain assertThat isEqualTo")
        void shouldExplainAssertThatIsEqualTo() {
            String assertion = "assertThat(name).isEqualTo(\"John\")";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).isNotEmpty();
        }

        @Test
        @DisplayName("Should explain assertThat isNotNull")
        void shouldExplainAssertThatIsNotNull() {
            String assertion = "assertThat(user).isNotNull()";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).isNotEmpty();
        }

        @Test
        @DisplayName("Should explain collection assertions")
        void shouldExplainCollectionAssertions() {
            String assertion = "assertThat(list).hasSize(3)";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).isNotEmpty();
        }

        @Test
        @DisplayName("Should explain contains assertion")
        void shouldExplainContainsAssertion() {
            String assertion = "assertThat(list).contains(element)";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Complex Assertion Explanation")
    class ComplexAssertionExplanation {

        @Test
        @DisplayName("Should explain chained assertions")
        void shouldExplainChainedAssertions() {
            String assertion = "assertThat(result).isNotNull().isEqualTo(expected)";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).isNotEmpty();
        }

        @Test
        @DisplayName("Should explain exception assertion")
        void shouldExplainExceptionAssertion() {
            String assertion = "assertThrows(IllegalArgumentException.class, () -> service.process(null))";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).containsAnyOf("异常", "exception", "抛出", "throw");
        }

        @Test
        @DisplayName("Should explain timeout assertion")
        void shouldExplainTimeoutAssertion() {
            String assertion = "assertTimeout(Duration.ofSeconds(1), () -> task.run())";
            
            String explanation = explainer.explain(assertion);
            
            assertThat(explanation).containsAnyOf("完成", "内", "Duration");
        }
    }

    @Nested
    @DisplayName("Natural Language Generation")
    class NaturalLanguageGeneration {

        @Test
        @DisplayName("Should generate Chinese explanation")
        void shouldGenerateChineseExplanation() {
            String assertion = "assertEquals(5, result)";
            
            String explanation = explainer.explainInChinese(assertion);
            
            assertThat(explanation).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate English explanation")
        void shouldGenerateEnglishExplanation() {
            String assertion = "assertEquals(5, result)";
            
            String explanation = explainer.explainInEnglish(assertion);
            
            assertThat(explanation).isNotEmpty();
        }

        @Test
        @DisplayName("Should include context in explanation")
        void shouldIncludeContextInExplanation() {
            String assertion = "assertEquals(expected, actual)";
            String context = "测试用户年龄计算";
            
            String explanation = explainer.explainWithContext(assertion, context);
            
            assertThat(explanation).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Assertion Documentation")
    class AssertionDocumentation {

        @Test
        @DisplayName("Should generate documentation for test method")
        void shouldGenerateDocumentationForTestMethod() {
            String testMethod = """
                @Test
                void shouldCalculateTotalPrice() {
                    Cart cart = new Cart();
                    cart.addItem(new Item("Apple", 10));
                    int total = cart.getTotal();
                    assertEquals(10, total);
                }
                """;
            
            String documentation = explainer.generateDocumentation(testMethod);
            
            assertThat(documentation).isNotEmpty();
        }

        @Test
        @DisplayName("Should extract assertion purpose")
        void shouldExtractAssertionPurpose() {
            String assertion = "assertEquals(100, account.getBalance(), \"Balance should be 100 after deposit\")";
            
            String purpose = explainer.extractPurpose(assertion);
            
            assertThat(purpose).contains("Balance");
        }
    }
}
