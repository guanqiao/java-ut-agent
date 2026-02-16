package com.utagent.mutation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class MutationScoreCalculatorTest {

    private MutationScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MutationScoreCalculator();
    }

    @Test
    void shouldCalculateScoreFromReport() {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(100)
            .killedMutants(85)
            .survivedMutants(15)
            .build();

        double score = calculator.calculate(report);

        assertThat(score).isCloseTo(0.85, within(0.001));
    }

    @Test
    void shouldReturnZeroForEmptyReport() {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.EmptyClass")
            .totalMutants(0)
            .killedMutants(0)
            .build();

        double score = calculator.calculate(report);

        assertThat(score).isEqualTo(0.0);
    }

    @Test
    void shouldReturnOneForFullKillRate() {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.PerfectClass")
            .totalMutants(50)
            .killedMutants(50)
            .build();

        double score = calculator.calculate(report);

        assertThat(score).isEqualTo(1.0);
    }

    @Test
    void shouldCalculateAverageScoreFromMultipleReports() {
        MutationReport report1 = MutationReport.builder()
            .targetClass("Class1")
            .totalMutants(10)
            .killedMutants(8)
            .build();

        MutationReport report2 = MutationReport.builder()
            .targetClass("Class2")
            .totalMutants(20)
            .killedMutants(15)
            .build();

        MutationReport report3 = MutationReport.builder()
            .targetClass("Class3")
            .totalMutants(30)
            .killedMutants(27)
            .build();

        double averageScore = calculator.calculateAverage(List.of(report1, report2, report3));

        double expectedTotal = (8 + 15 + 27) / (double) (10 + 20 + 30);
        assertThat(averageScore).isCloseTo(expectedTotal, within(0.001));
    }

    @Test
    void shouldReturnZeroForEmptyReportList() {
        double averageScore = calculator.calculateAverage(List.of());

        assertThat(averageScore).isEqualTo(0.0);
    }

    @Test
    void shouldCalculateWeightedScore() {
        MutationReport report1 = MutationReport.builder()
            .targetClass("Class1")
            .totalMutants(100)
            .killedMutants(90)
            .build();

        MutationReport report2 = MutationReport.builder()
            .targetClass("Class2")
            .totalMutants(10)
            .killedMutants(9)
            .build();

        double weightedScore = calculator.calculateWeighted(List.of(report1, report2));

        assertThat(weightedScore).isCloseTo(0.9, within(0.001));
    }

    @Test
    void shouldCalculateScoreByMutator() {
        List<Mutant> mutants = List.of(
            createMutant("MathMutator", MutantStatus.KILLED),
            createMutant("MathMutator", MutantStatus.KILLED),
            createMutant("MathMutator", MutantStatus.SURVIVED),
            createMutant("NullMutator", MutantStatus.KILLED),
            createMutant("NullMutator", MutantStatus.SURVIVED)
        );

        MutationReport report = MutationReport.builder()
            .targetClass("TestClass")
            .totalMutants(5)
            .killedMutants(3)
            .survivedMutants(2)
            .mutants(mutants)
            .build();

        var scoresByMutator = calculator.calculateByMutator(report);

        assertThat(scoresByMutator).containsKeys("MathMutator", "NullMutator");
        assertThat(scoresByMutator.get("MathMutator")).isCloseTo(2.0/3.0, within(0.001));
        assertThat(scoresByMutator.get("NullMutator")).isCloseTo(0.5, within(0.001));
    }

    @Test
    void shouldCalculateScoreByMethod() {
        List<Mutant> mutants = List.of(
            createMutantWithMethod("add", MutantStatus.KILLED),
            createMutantWithMethod("add", MutantStatus.KILLED),
            createMutantWithMethod("add", MutantStatus.SURVIVED),
            createMutantWithMethod("subtract", MutantStatus.KILLED),
            createMutantWithMethod("subtract", MutantStatus.SURVIVED)
        );

        MutationReport report = MutationReport.builder()
            .targetClass("TestClass")
            .totalMutants(5)
            .killedMutants(3)
            .survivedMutants(2)
            .mutants(mutants)
            .build();

        var scoresByMethod = calculator.calculateByMethod(report);

        assertThat(scoresByMethod).containsKeys("add", "subtract");
        assertThat(scoresByMethod.get("add")).isCloseTo(2.0/3.0, within(0.001));
        assertThat(scoresByMethod.get("subtract")).isCloseTo(0.5, within(0.001));
    }

    @Test
    void shouldIdentifyWeakAreas() {
        List<Mutant> mutants = List.of(
            createMutantWithMethod("wellTestedMethod", MutantStatus.KILLED),
            createMutantWithMethod("wellTestedMethod", MutantStatus.KILLED),
            createMutantWithMethod("poorlyTestedMethod", MutantStatus.SURVIVED),
            createMutantWithMethod("poorlyTestedMethod", MutantStatus.SURVIVED),
            createMutantWithMethod("poorlyTestedMethod", MutantStatus.SURVIVED)
        );

        MutationReport report = MutationReport.builder()
            .targetClass("TestClass")
            .totalMutants(5)
            .killedMutants(2)
            .survivedMutants(3)
            .mutants(mutants)
            .build();

        List<String> weakAreas = calculator.identifyWeakAreas(report, 0.5);

        assertThat(weakAreas).contains("poorlyTestedMethod");
        assertThat(weakAreas).doesNotContain("wellTestedMethod");
    }

    @Test
    void shouldCalculateImprovementPotential() {
        MutationReport report = MutationReport.builder()
            .targetClass("TestClass")
            .totalMutants(100)
            .killedMutants(70)
            .survivedMutants(30)
            .build();

        double potential = calculator.calculateImprovementPotential(report);

        assertThat(potential).isEqualTo(0.3);
    }

    @Test
    void shouldCalculateScoreTrend() {
        MutationReport report1 = MutationReport.builder()
            .targetClass("Class")
            .totalMutants(10)
            .killedMutants(6)
            .build();

        MutationReport report2 = MutationReport.builder()
            .targetClass("Class")
            .totalMutants(10)
            .killedMutants(7)
            .build();

        MutationReport report3 = MutationReport.builder()
            .targetClass("Class")
            .totalMutants(10)
            .killedMutants(8)
            .build();

        double trend = calculator.calculateTrend(List.of(report1, report2, report3));

        assertThat(trend).isPositive();
    }

    @Test
    void shouldFormatScoreAsPercentage() {
        double score = 0.856;

        String formatted = calculator.formatAsPercentage(score);

        assertThat(formatted).isEqualTo("85.60%");
    }

    @Test
    void shouldDetermineQualityLevel() {
        assertThat(calculator.determineQualityLevel(0.95)).isEqualTo(MutationQualityLevel.EXCELLENT);
        assertThat(calculator.determineQualityLevel(0.85)).isEqualTo(MutationQualityLevel.GOOD);
        assertThat(calculator.determineQualityLevel(0.70)).isEqualTo(MutationQualityLevel.MODERATE);
        assertThat(calculator.determineQualityLevel(0.50)).isEqualTo(MutationQualityLevel.POOR);
        assertThat(calculator.determineQualityLevel(0.30)).isEqualTo(MutationQualityLevel.CRITICAL);
    }

    private Mutant createMutant(String mutator, MutantStatus status) {
        return Mutant.builder()
            .id("test-id")
            .mutator(mutator)
            .method("testMethod")
            .lineNumber(1)
            .status(status)
            .description("Test mutant")
            .build();
    }

    private Mutant createMutantWithMethod(String method, MutantStatus status) {
        return Mutant.builder()
            .id("test-id")
            .mutator("TestMutator")
            .method(method)
            .lineNumber(1)
            .status(status)
            .description("Test mutant")
            .build();
    }
}
