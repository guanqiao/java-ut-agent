package com.utagent.quality;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TestQualityScorerTest {

    private TestQualityScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new TestQualityScorer();
    }

    @Test
    void shouldCalculateOverallQualityScore() {
        TestQualityMetrics metrics = TestQualityMetrics.builder()
            .lineCoverage(0.85)
            .branchCoverage(0.75)
            .mutationScore(0.80)
            .readabilityScore(0.90)
            .build();

        double score = scorer.calculateOverallScore(metrics);

        assertThat(score).isGreaterThan(0.0);
        assertThat(score).isLessThanOrEqualTo(1.0);
    }

    @Test
    void shouldWeightMetricsCorrectly() {
        TestQualityMetrics metrics = TestQualityMetrics.builder()
            .lineCoverage(1.0)
            .branchCoverage(1.0)
            .mutationScore(1.0)
            .readabilityScore(1.0)
            .build();

        double score = scorer.calculateOverallScore(metrics);

        assertThat(score).isCloseTo(1.0, within(0.001));
    }

    @Test
    void shouldReturnZeroForEmptyMetrics() {
        TestQualityMetrics metrics = TestQualityMetrics.builder().build();

        double score = scorer.calculateOverallScore(metrics);

        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void shouldDetermineQualityGrade() {
        assertThat(scorer.determineGrade(0.95)).isEqualTo(QualityGrade.A);
        assertThat(scorer.determineGrade(0.85)).isEqualTo(QualityGrade.B);
        assertThat(scorer.determineGrade(0.70)).isEqualTo(QualityGrade.C);
        assertThat(scorer.determineGrade(0.55)).isEqualTo(QualityGrade.D);
        assertThat(scorer.determineGrade(0.30)).isEqualTo(QualityGrade.F);
    }

    @Test
    void shouldCalculateReadabilityScore() {
        String testCode = """
            @Test
            void shouldAddTwoNumbers() {
                Calculator calc = new Calculator();
                int result = calc.add(2, 3);
                assertEquals(5, result);
            }
            """;

        double readability = scorer.calculateReadability(testCode);

        assertThat(readability).isGreaterThan(0.5);
    }

    @Test
    void shouldPenalizeLongMethods() {
        StringBuilder longMethod = new StringBuilder("@Test\nvoid test() {\n");
        for (int i = 0; i < 100; i++) {
            longMethod.append("    int x").append(i).append(" = ").append(i).append(";\n");
        }
        longMethod.append("}");

        double readability = scorer.calculateReadability(longMethod.toString());

        assertThat(readability).isLessThan(0.7);
    }

    @Test
    void shouldRewardGoodNaming() {
        String goodNaming = """
            @Test
            void shouldReturnSumWhenAddingTwoPositiveNumbers() {
                Calculator calculator = new Calculator();
                int actualSum = calculator.add(2, 3);
                int expectedSum = 5;
                assertEquals(expectedSum, actualSum);
            }
            """;

        String poorNaming = """
            @Test
            void test1() {
                var c = new Calculator();
                var r = c.add(2, 3);
                assertEquals(5, r);
            }
            """;

        double goodScore = scorer.calculateReadability(goodNaming);
        double poorScore = scorer.calculateReadability(poorNaming);

        assertThat(goodScore).isGreaterThan(poorScore);
    }

    @Test
    void shouldCalculateAssertionDensity() {
        String testCode = """
            @Test
            void testCalculator() {
                Calculator calc = new Calculator();
                assertEquals(5, calc.add(2, 3));
                assertEquals(2, calc.subtract(5, 3));
                assertTrue(calc.isPositive(5));
            }
            """;

        double density = scorer.calculateAssertionDensity(testCode);

        assertThat(density).isGreaterThan(0.0);
    }

    @Test
    void shouldCalculateTestIndependence() {
        String independentTests = """
            @Test
            void testAdd() {
                Calculator calc = new Calculator();
                assertEquals(5, calc.add(2, 3));
            }
            @Test
            void testSubtract() {
                Calculator calc = new Calculator();
                assertEquals(2, calc.subtract(5, 3));
            }
            """;

        double independence = scorer.calculateIndependence(independentTests);

        assertThat(independence).isCloseTo(1.0, within(0.1));
    }

    @Test
    void shouldDetectTestDependencies() {
        String dependentTests = """
            private Calculator calc;
            
            @BeforeEach
            void setUp() {
                calc = new Calculator();
                calc.setState(10);
            }
            
            @Test
            void test1() {
                assertEquals(15, calc.add(5));
            }
            
            @Test
            void test2() {
                assertEquals(5, calc.subtract(5));
            }
            """;

        double independence = scorer.calculateIndependence(dependentTests);

        assertThat(independence).isLessThan(1.0);
    }

    @Test
    void shouldGenerateQualityReport() {
        TestQualityMetrics metrics = TestQualityMetrics.builder()
            .lineCoverage(0.85)
            .branchCoverage(0.75)
            .mutationScore(0.80)
            .readabilityScore(0.90)
            .build();

        QualityReport report = scorer.generateReport(metrics);

        assertThat(report).isNotNull();
        assertThat(report.overallScore()).isGreaterThan(0.0);
        assertThat(report.grade()).isNotNull();
        assertThat(report.recommendations()).isNotEmpty();
    }

    @Test
    void shouldProvideImprovementSuggestions() {
        TestQualityMetrics lowCoverage = TestQualityMetrics.builder()
            .lineCoverage(0.50)
            .branchCoverage(0.40)
            .mutationScore(0.30)
            .readabilityScore(0.60)
            .build();

        var suggestions = scorer.getImprovementSuggestions(lowCoverage);

        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions).anyMatch(s -> s.contains("coverage") || s.contains("mutation"));
    }

    @Test
    void shouldCompareTestQuality() {
        TestQualityMetrics metrics1 = TestQualityMetrics.builder()
            .lineCoverage(0.90)
            .mutationScore(0.85)
            .build();

        TestQualityMetrics metrics2 = TestQualityMetrics.builder()
            .lineCoverage(0.70)
            .mutationScore(0.60)
            .build();

        int comparison = scorer.compare(metrics1, metrics2);

        assertThat(comparison).isPositive();
    }

    @Test
    void shouldCalculateMaintainabilityIndex() {
        String testCode = """
            @Test
            void shouldCalculateCorrectly() {
                Calculator calc = new Calculator();
                assertEquals(5, calc.add(2, 3));
                assertEquals(-1, calc.add(2, -3));
                assertEquals(0, calc.add(0, 0));
            }
            """;

        double maintainability = scorer.calculateMaintainability(testCode);

        assertThat(maintainability).isGreaterThan(0.5);
    }

    @Test
    void shouldScoreTestOrganization() {
        String organizedTests = """
            class CalculatorTest {
                @Nested
                class AdditionTests {
                    @Test
                    void shouldAddPositiveNumbers() { }
                    @Test
                    void shouldAddNegativeNumbers() { }
                }
                @Nested
                class SubtractionTests {
                    @Test
                    void shouldSubtractNumbers() { }
                }
            }
            """;

        double organization = scorer.calculateOrganization(organizedTests);

        assertThat(organization).isGreaterThan(0.7);
    }

    @Test
    void shouldCalculateTestEffectivenessRatio() {
        int testsCount = 10;
        int defectsFound = 8;

        double ter = scorer.calculateTestEffectivenessRatio(testsCount, defectsFound);

        assertThat(ter).isCloseTo(0.8, within(0.001));
    }
}
