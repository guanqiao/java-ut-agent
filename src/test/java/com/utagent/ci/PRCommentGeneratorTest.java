package com.utagent.ci;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PRCommentGenerator Tests")
class PRCommentGeneratorTest {

    private PRCommentGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new PRCommentGenerator();
    }

    @Nested
    @DisplayName("Coverage Comment Generation")
    class CoverageCommentGeneration {

        @Test
        @DisplayName("Should generate coverage summary comment")
        void shouldGenerateCoverageSummaryComment() {
            CoverageSummary summary = new CoverageSummary(
                0.85, 0.72, 0.90, 0.88
            );
            
            String comment = generator.generateCoverageComment(summary);
            
            assertThat(comment).contains("Coverage Report");
            assertThat(comment).contains("85%");
            assertThat(comment).contains("Line");
            assertThat(comment).contains("Branch");
        }

        @Test
        @DisplayName("Should include coverage change indicator")
        void shouldIncludeCoverageChangeIndicator() {
            CoverageSummary current = new CoverageSummary(0.85, 0.72, 0.90, 0.88);
            CoverageSummary previous = new CoverageSummary(0.80, 0.70, 0.85, 0.82);
            
            String comment = generator.generateCoverageChangeComment(current, previous);
            
            assertThat(comment).contains("+5.0%");
            assertThat(comment).contains("improved");
        }

        @Test
        @DisplayName("Should show negative change indicator")
        void shouldShowNegativeChangeIndicator() {
            CoverageSummary current = new CoverageSummary(0.75, 0.65, 0.80, 0.78);
            CoverageSummary previous = new CoverageSummary(0.80, 0.70, 0.85, 0.82);
            
            String comment = generator.generateCoverageChangeComment(current, previous);
            
            assertThat(comment).contains("-5.0%");
            assertThat(comment).contains("decreased");
        }
    }

    @Nested
    @DisplayName("Test Quality Comment")
    class TestQualityComment {

        @Test
        @DisplayName("Should generate quality score comment")
        void shouldGenerateQualityScoreComment() {
            QualityScore score = new QualityScore(
                85.0, 
                Map.of("coverage", 25.0, "mutation", 30.0, "readability", 15.0, "maintainability", 15.0)
            );
            
            String comment = generator.generateQualityComment(score);
            
            assertThat(comment).contains("Quality Score");
            assertThat(comment).contains("85");
        }

        @Test
        @DisplayName("Should include improvement suggestions")
        void shouldIncludeImprovementSuggestions() {
            QualityScore score = new QualityScore(
                60.0,
                Map.of("coverage", 15.0, "mutation", 20.0, "readability", 12.0, "maintainability", 13.0)
            );
            
            String comment = generator.generateQualityComment(score);
            
            assertThat(comment).contains("Suggestions");
        }
    }

    @Nested
    @DisplayName("New Tests Comment")
    class NewTestsComment {

        @Test
        @DisplayName("Should list new test files")
        void shouldListNewTestFiles() {
            List<String> newTests = List.of(
                "CalculatorTest.java",
                "UserServiceTest.java"
            );
            
            String comment = generator.generateNewTestsComment(newTests);
            
            assertThat(comment).contains("New Tests Generated");
            assertThat(comment).contains("CalculatorTest.java");
            assertThat(comment).contains("UserServiceTest.java");
        }

        @Test
        @DisplayName("Should show test count")
        void shouldShowTestCount() {
            List<String> newTests = List.of("Test1.java", "Test2.java", "Test3.java");
            
            String comment = generator.generateNewTestsComment(newTests);
            
            assertThat(comment).contains("3 new test files");
        }
    }

    @Nested
    @DisplayName("Markdown Formatting")
    class MarkdownFormatting {

        @Test
        @DisplayName("Should format as markdown")
        void shouldFormatAsMarkdown() {
            CoverageSummary summary = new CoverageSummary(0.85, 0.72, 0.90, 0.88);
            
            String comment = generator.generateCoverageComment(summary);
            
            assertThat(comment).contains("##");
            assertThat(comment).contains("|");
            assertThat(comment).contains("---");
        }

        @Test
        @DisplayName("Should include badges")
        void shouldIncludeBadges() {
            CoverageSummary summary = new CoverageSummary(0.85, 0.72, 0.90, 0.88);
            
            String comment = generator.generateCoverageComment(summary);
            
            assertThat(comment).containsAnyOf("🟢", "🟡", "🔴");
        }

        @Test
        @DisplayName("Should format table correctly")
        void shouldFormatTableCorrectly() {
            CoverageSummary summary = new CoverageSummary(0.85, 0.72, 0.90, 0.88);
            
            String comment = generator.generateCoverageComment(summary);
            
            assertThat(comment).contains("| Type | Coverage |");
            assertThat(comment).contains("| Line |");
            assertThat(comment).contains("| Branch |");
        }
    }

    @Nested
    @DisplayName("PR Status Comment")
    class PRStatusComment {

        @Test
        @DisplayName("Should generate success status")
        void shouldGenerateSuccessStatus() {
            PRStatus status = new PRStatus(
                true, 
                "All tests passed",
                0.85,
                85.0
            );
            
            String comment = generator.generateStatusComment(status);
            
            assertThat(comment).contains("passed");
        }

        @Test
        @DisplayName("Should generate failure status")
        void shouldGenerateFailureStatus() {
            PRStatus status = new PRStatus(
                false,
                "2 tests failed",
                0.65,
                60.0
            );
            
            String comment = generator.generateStatusComment(status);
            
            assertThat(comment).contains("failed");
        }
    }

    @Nested
    @DisplayName("Comment Template")
    class CommentTemplate {

        @Test
        @DisplayName("Should use custom template")
        void shouldUseCustomTemplate() {
            String template = """
                ## Custom Report
                Coverage: {{coverage}}%
                """;
            
            generator.setTemplate(template);
            
            CoverageSummary summary = new CoverageSummary(0.85, 0.72, 0.90, 0.88);
            String comment = generator.generateFromTemplate(summary);
            
            assertThat(comment).contains("Custom Report");
            assertThat(comment).contains("84%");
        }
    }
}
