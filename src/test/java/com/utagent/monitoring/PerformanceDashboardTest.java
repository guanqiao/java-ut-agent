package com.utagent.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PerformanceDashboard Tests")
class PerformanceDashboardTest {

    private PerformanceDashboard dashboard;

    @BeforeEach
    void setUp() {
        dashboard = new PerformanceDashboard();
    }

    @Nested
    @DisplayName("Metrics Collection")
    class MetricsCollection {

        @Test
        @DisplayName("Should record test execution time")
        void shouldRecordTestExecutionTime() {
            dashboard.recordExecution("CalculatorTest", "testAdd", 150);
            
            assertThat(dashboard.getExecutionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should calculate average execution time")
        void shouldCalculateAverageExecutionTime() {
            dashboard.recordExecution("CalculatorTest", "testAdd", 100);
            dashboard.recordExecution("CalculatorTest", "testAdd", 200);
            
            double avgTime = dashboard.getAverageExecutionTime("CalculatorTest", "testAdd");
            
            assertThat(avgTime).isEqualTo(150.0);
        }

        @Test
        @DisplayName("Should track slow tests")
        void shouldTrackSlowTests() {
            dashboard.recordExecution("SlowTest", "slowMethod", 5000);
            
            List<SlowTestInfo> slowTests = dashboard.getSlowTests(1000);
            
            assertThat(slowTests).hasSize(1);
            assertThat(slowTests.get(0).getTestName()).isEqualTo("slowMethod");
        }
    }

    @Nested
    @DisplayName("Statistics")
    class Statistics {

        @Test
        @DisplayName("Should calculate total execution time")
        void shouldCalculateTotalExecutionTime() {
            dashboard.recordExecution("Test1", "test1", 100);
            dashboard.recordExecution("Test2", "test2", 200);
            
            long totalTime = dashboard.getTotalExecutionTime();
            
            assertThat(totalTime).isEqualTo(300);
        }

        @Test
        @DisplayName("Should calculate test throughput")
        void shouldCalculateTestThroughput() {
            dashboard.recordExecution("Test", "test", 100);
            dashboard.recordExecution("Test", "test", 100);
            dashboard.recordExecution("Test", "test", 100);
            
            double throughput = dashboard.getThroughput(1000);
            
            assertThat(throughput).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should get execution statistics")
        void shouldGetExecutionStatistics() {
            dashboard.recordExecution("Test", "test", 100);
            dashboard.recordExecution("Test", "test", 200);
            dashboard.recordExecution("Test", "test", 300);
            
            ExecutionStats stats = dashboard.getStats("Test", "test");
            
            assertThat(stats.getCount()).isEqualTo(3);
            assertThat(stats.getMin()).isEqualTo(100);
            assertThat(stats.getMax()).isEqualTo(300);
            assertThat(stats.getAverage()).isEqualTo(200.0);
        }
    }

    @Nested
    @DisplayName("Dashboard Display")
    class DashboardDisplay {

        @Test
        @DisplayName("Should generate summary report")
        void shouldGenerateSummaryReport() {
            dashboard.recordExecution("Test1", "test1", 100);
            dashboard.recordExecution("Test2", "test2", 200);
            
            String report = dashboard.generateSummaryReport();
            
            assertThat(report).contains("Test1");
            assertThat(report).contains("Test2");
        }

        @Test
        @DisplayName("Should generate HTML report")
        void shouldGenerateHtmlReport() {
            dashboard.recordExecution("Test", "test", 100);
            
            String html = dashboard.generateHtmlReport();
            
            assertThat(html).contains("<html");
            assertThat(html).contains("</html>");
        }

        @Test
        @DisplayName("Should generate JSON report")
        void shouldGenerateJsonReport() {
            dashboard.recordExecution("Test", "test", 100);
            
            String json = dashboard.generateJsonReport();
            
            assertThat(json).contains("\"test\"");
            assertThat(json).contains("100");
        }
    }

    @Nested
    @DisplayName("Alert System")
    class AlertSystem {

        @Test
        @DisplayName("Should detect performance regression")
        void shouldDetectPerformanceRegression() {
            dashboard.setBaseline("Test", "test", 100);
            dashboard.recordExecution("Test", "test", 500);
            
            List<PerformanceAlert> alerts = dashboard.checkForRegressions(2.0);
            
            assertThat(alerts).isNotEmpty();
        }

        @Test
        @DisplayName("Should not alert for normal execution")
        void shouldNotAlertForNormalExecution() {
            dashboard.setBaseline("Test", "test", 100);
            dashboard.recordExecution("Test", "test", 120);
            
            List<PerformanceAlert> alerts = dashboard.checkForRegressions(2.0);
            
            assertThat(alerts).isEmpty();
        }

        @Test
        @DisplayName("Should track flaky tests")
        void shouldTrackFlakyTests() {
            dashboard.recordExecution("Test", "flaky", 100);
            dashboard.recordExecution("Test", "flaky", 5000);
            dashboard.recordExecution("Test", "flaky", 150);
            
            List<FlakyTestInfo> flakyTests = dashboard.detectFlakyTests(0.5);
            
            assertThat(flakyTests).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Trend Analysis")
    class TrendAnalysis {

        @Test
        @DisplayName("Should track performance trend")
        void shouldTrackPerformanceTrend() {
            for (int i = 0; i < 10; i++) {
                dashboard.recordExecution("Test", "test", 100 + i * 10);
            }
            
            PerformanceTrend trend = dashboard.getTrend("Test", "test");
            
            assertThat(trend).isNotNull();
            assertThat(trend.getDirection()).isEqualTo(TrendDirection.INCREASING);
        }

        @Test
        @DisplayName("Should predict future execution time")
        void shouldPredictFutureExecutionTime() {
            for (int i = 0; i < 10; i++) {
                dashboard.recordExecution("Test", "test", 100);
            }
            
            double predicted = dashboard.predictExecutionTime("Test", "test", 5);
            
            assertThat(predicted).isGreaterThan(0);
        }
    }
}
