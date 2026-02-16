package com.utagent.optimizer;

import com.utagent.mutation.Mutant;
import com.utagent.mutation.MutantStatus;
import com.utagent.mutation.MutationAnalyzer;
import com.utagent.mutation.MutationReport;
import com.utagent.mutation.MutationScoreCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class IterativeOptimizerMutationTest {

    @TempDir
    Path tempDir;

    private File projectRoot;
    private MutationAnalyzer mutationAnalyzer;
    private MutationScoreCalculator scoreCalculator;

    @BeforeEach
    void setUp() throws IOException {
        projectRoot = tempDir.toFile();
        mutationAnalyzer = new MutationAnalyzer(projectRoot);
        scoreCalculator = new MutationScoreCalculator();
        createSampleProject();
    }

    private void createSampleProject() throws IOException {
        File srcDir = new File(projectRoot, "src/main/java/com/example");
        File testDir = new File(projectRoot, "src/test/java/com/example");
        srcDir.mkdirs();
        testDir.mkdirs();

        String sourceCode = """
            package com.example;
            public class Calculator {
                public int add(int a, int b) {
                    return a + b;
                }
                public int subtract(int a, int b) {
                    return a - b;
                }
                public int multiply(int a, int b) {
                    return a * b;
                }
            }
            """;

        String testCode = """
            package com.example;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.*;
            
            class CalculatorTest {
                @Test
                void testAdd() {
                    Calculator calc = new Calculator();
                    assertEquals(5, calc.add(2, 3));
                    assertEquals(0, calc.add(0, 0));
                    assertEquals(-1, calc.add(2, -3));
                }
                @Test
                void testSubtract() {
                    Calculator calc = new Calculator();
                    assertEquals(2, calc.subtract(5, 3));
                }
            }
            """;

        Files.writeString(new File(srcDir, "Calculator.java").toPath(), sourceCode);
        Files.writeString(new File(testDir, "CalculatorTest.java").toPath(), testCode);
    }

    @Test
    void shouldIntegrateMutationAnalysisWithOptimization() {
        MutationReport report = mutationAnalyzer.analyze("com.example.Calculator");

        assertThat(report).isNotNull();
        assertThat(report.getTargetClass()).isEqualTo("com.example.Calculator");
    }

    @Test
    void shouldCalculateMutationScoreForGeneratedTests() {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(20)
            .killedMutants(16)
            .survivedMutants(4)
            .build();

        double score = scoreCalculator.calculate(report);

        assertThat(score).isCloseTo(0.8, within(0.001));
    }

    @Test
    void shouldIdentifyWeakTestAreas() {
        List<Mutant> mutants = List.of(
            createMutant("add", MutantStatus.KILLED),
            createMutant("add", MutantStatus.KILLED),
            createMutant("subtract", MutantStatus.SURVIVED),
            createMutant("subtract", MutantStatus.SURVIVED),
            createMutant("multiply", MutantStatus.SURVIVED)
        );

        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(5)
            .killedMutants(2)
            .survivedMutants(3)
            .mutants(mutants)
            .build();

        List<String> weakAreas = scoreCalculator.identifyWeakAreas(report, 0.5);

        assertThat(weakAreas).contains("subtract", "multiply");
        assertThat(weakAreas).doesNotContain("add");
    }

    @Test
    void shouldTrackMutationScoreImprovement() {
        MutationReport report1 = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(20)
            .killedMutants(12)
            .build();

        MutationReport report2 = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(20)
            .killedMutants(16)
            .build();

        MutationReport report3 = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(20)
            .killedMutants(18)
            .build();

        List<MutationReport> reports = List.of(report1, report2, report3);
        double trend = scoreCalculator.calculateTrend(reports);

        assertThat(trend).isPositive();
        assertThat(trend).isCloseTo(0.3, within(0.001));
    }

    @Test
    void shouldGenerateMutationBasedRecommendations() {
        List<Mutant> mutants = List.of(
            createMutant("multiply", MutantStatus.SURVIVED),
            createMutant("multiply", MutantStatus.SURVIVED)
        );

        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(2)
            .killedMutants(0)
            .survivedMutants(2)
            .mutants(mutants)
            .build();

        var recommendations = new com.utagent.mutation.MutationReportGenerator()
            .generateRecommendations(report);

        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations).anyMatch(r -> r.contains("multiply"));
    }

    @Test
    void shouldDetermineIfMoreTestsNeeded() {
        MutationReport lowScoreReport = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(20)
            .killedMutants(10)
            .build();

        MutationReport highScoreReport = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(20)
            .killedMutants(18)
            .build();

        var lowQuality = scoreCalculator.determineQualityLevel(lowScoreReport.getMutationScore());
        var highQuality = scoreCalculator.determineQualityLevel(highScoreReport.getMutationScore());

        assertThat(lowQuality).isIn(com.utagent.mutation.MutationQualityLevel.POOR, 
            com.utagent.mutation.MutationQualityLevel.MODERATE);
        assertThat(highQuality).isEqualTo(com.utagent.mutation.MutationQualityLevel.EXCELLENT);
    }

    @Test
    void shouldCalculateOverallTestEffectiveness() {
        double lineCoverage = 0.85;
        double branchCoverage = 0.75;
        double mutationScore = 0.80;

        double effectiveness = calculateTestEffectiveness(lineCoverage, branchCoverage, mutationScore);

        assertThat(effectiveness).isCloseTo(0.80, within(0.01));
    }

    @Test
    void shouldPrioritizeMethodsForAdditionalTesting() {
        List<Mutant> mutants = List.of(
            createMutant("add", MutantStatus.KILLED),
            createMutant("add", MutantStatus.KILLED),
            createMutant("subtract", MutantStatus.SURVIVED),
            createMutant("multiply", MutantStatus.SURVIVED),
            createMutant("multiply", MutantStatus.SURVIVED),
            createMutant("multiply", MutantStatus.SURVIVED)
        );

        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(6)
            .killedMutants(2)
            .survivedMutants(4)
            .mutants(mutants)
            .build();

        var methodScores = scoreCalculator.calculateByMethod(report);

        assertThat(methodScores.get("multiply")).isLessThan(methodScores.get("add"));
    }

    @Test
    void shouldSupportMutationBasedIterationDecision() {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(20)
            .killedMutants(15)
            .build();

        boolean needsMoreIterations = shouldContinueIterating(report, 0.85);

        assertThat(needsMoreIterations).isTrue();
    }

    @Test
    void shouldGenerateMutationReportAfterOptimization() throws IOException {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(20)
            .killedMutants(16)
            .survivedMutants(4)
            .build();

        File outputDir = new File(projectRoot, "target/pit-reports");
        outputDir.mkdirs();

        File reportFile = mutationAnalyzer.generateHtmlReport(report, outputDir);

        assertThat(reportFile).exists();
        String content = Files.readString(reportFile.toPath());
        assertThat(content).contains("com.example.Calculator");
        assertThat(content).contains("80.00%");
    }

    private Mutant createMutant(String method, MutantStatus status) {
        return Mutant.builder()
            .id("test-id")
            .mutator("TestMutator")
            .method(method)
            .lineNumber(1)
            .status(status)
            .description("Test mutant")
            .build();
    }

    private double calculateTestEffectiveness(double lineCoverage, double branchCoverage, double mutationScore) {
        return (lineCoverage * 0.3 + branchCoverage * 0.3 + mutationScore * 0.4);
    }

    private boolean shouldContinueIterating(MutationReport report, double targetScore) {
        return report.getMutationScore() < targetScore;
    }
}
