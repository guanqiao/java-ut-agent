package com.utagent.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReportVisualizer Tests")
class ReportVisualizerTest {

    private ReportVisualizer visualizer;

    @BeforeEach
    void setUp() {
        visualizer = new ReportVisualizer();
    }

    @Nested
    @DisplayName("Coverage Visualization")
    class CoverageVisualization {

        @Test
        @DisplayName("Should generate coverage chart")
        void shouldGenerateCoverageChart() {
            CoverageData data = new CoverageData(
                Map.of("Line", 0.85, "Branch", 0.72, "Method", 0.90),
                0.82
            );
            
            String chart = visualizer.generateCoverageChart(data);
            
            assertThat(chart).isNotEmpty();
            assertThat(chart).contains("Line");
            assertThat(chart).contains("Branch");
        }

        @Test
        @DisplayName("Should generate coverage trend chart")
        void shouldGenerateCoverageTrendChart() {
            List<CoverageData> trend = List.of(
                new CoverageData(Map.of("Line", 0.75), 0.75),
                new CoverageData(Map.of("Line", 0.80), 0.80),
                new CoverageData(Map.of("Line", 0.85), 0.85)
            );
            
            String chart = visualizer.generateTrendChart(trend);
            
            assertThat(chart).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate coverage heatmap")
        void shouldGenerateCoverageHeatmap() {
            Map<String, Double> classCoverage = Map.of(
                "Calculator", 0.95,
                "UserService", 0.72,
                "OrderService", 0.45
            );
            
            String heatmap = visualizer.generateCoverageHeatmap(classCoverage);
            
            assertThat(heatmap).isNotEmpty();
            assertThat(heatmap).contains("Calculator");
            assertThat(heatmap).contains("UserService");
        }
    }

    @Nested
    @DisplayName("Test Quality Visualization")
    class TestQualityVisualization {

        @Test
        @DisplayName("Should generate quality radar chart")
        void shouldGenerateQualityRadarChart() {
            QualityMetrics metrics = new QualityMetrics(
                0.85, 0.72, 0.90, 0.88, 0.65
            );
            
            String chart = visualizer.generateQualityRadarChart(metrics);
            
            assertThat(chart).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate quality score gauge")
        void shouldGenerateQualityScoreGauge() {
            double score = 85.0;
            
            String gauge = visualizer.generateQualityGauge(score);
            
            assertThat(gauge).isNotEmpty();
            assertThat(gauge).contains("85");
        }
    }

    @Nested
    @DisplayName("Test Distribution Visualization")
    class TestDistributionVisualization {

        @Test
        @DisplayName("Should generate test type pie chart")
        void shouldGenerateTestTypePieChart() {
            Map<String, Integer> distribution = Map.of(
                "Unit Tests", 150,
                "Integration Tests", 25,
                "E2E Tests", 10
            );
            
            String chart = visualizer.generateTestTypePieChart(distribution);
            
            assertThat(chart).isNotEmpty();
            assertThat(chart).contains("Unit Tests");
        }

        @Test
        @DisplayName("Should generate test duration bar chart")
        void shouldGenerateTestDurationBarChart() {
            Map<String, Long> durations = Map.of(
                "CalculatorTest", 150L,
                "UserServiceTest", 320L,
                "OrderServiceTest", 580L
            );
            
            String chart = visualizer.generateDurationBarChart(durations);
            
            assertThat(chart).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Report Export")
    class ReportExport {

        @Test
        @DisplayName("Should export to HTML")
        void shouldExportToHtml() {
            TestReport report = TestReport.builder()
                .title("Test Report")
                .coverage(new CoverageData(Map.of("Line", 0.85), 0.85))
                .build();
            
            String html = visualizer.exportToHtml(report);
            
            assertThat(html).contains("<html");
            assertThat(html).contains("</html>");
            assertThat(html).contains("Test Report");
        }

        @Test
        @DisplayName("Should export to Markdown")
        void shouldExportToMarkdown() {
            TestReport report = TestReport.builder()
                .title("Test Report")
                .coverage(new CoverageData(Map.of("Line", 0.85), 0.85))
                .build();
            
            String markdown = visualizer.exportToMarkdown(report);
            
            assertThat(markdown).contains("# Test Report");
            assertThat(markdown).contains("85%");
        }

        @Test
        @DisplayName("Should export to JSON")
        void shouldExportToJson() {
            TestReport report = TestReport.builder()
                .title("Test Report")
                .coverage(new CoverageData(Map.of("Line", 0.85), 0.85))
                .build();
            
            String json = visualizer.exportToJson(report);
            
            assertThat(json).contains("\"title\"");
            assertThat(json).contains("Test Report");
        }
    }

    @Nested
    @DisplayName("Dashboard Generation")
    class DashboardGeneration {

        @Test
        @DisplayName("Should generate complete dashboard")
        void shouldGenerateCompleteDashboard() {
            DashboardConfig config = DashboardConfig.builder()
                .includeCoverage(true)
                .includeQuality(true)
                .includeTrends(true)
                .build();
            
            String dashboard = visualizer.generateDashboard(config);
            
            assertThat(dashboard).isNotEmpty();
            assertThat(dashboard).contains("<html");
        }

        @Test
        @DisplayName("Should include custom sections")
        void shouldIncludeCustomSections() {
            DashboardConfig config = DashboardConfig.builder()
                .customSections(List.of("Performance", "Mutation"))
                .build();
            
            String dashboard = visualizer.generateDashboard(config);
            
            assertThat(dashboard).contains("Performance");
            assertThat(dashboard).contains("Mutation");
        }
    }
}
