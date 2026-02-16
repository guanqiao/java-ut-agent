package com.utagent.mutation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class MutationReportGeneratorTest {

    @TempDir
    Path tempDir;

    private MutationReportGenerator generator;
    private File outputDir;

    @BeforeEach
    void setUp() {
        generator = new MutationReportGenerator();
        outputDir = tempDir.toFile();
    }

    @Test
    void shouldGenerateSummaryReport() {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(100)
            .killedMutants(85)
            .survivedMutants(10)
            .timeoutMutants(3)
            .memoryErrorMutants(2)
            .build();

        String summary = generator.generateSummary(report);

        assertThat(summary).contains("Mutation Test Summary");
        assertThat(summary).contains("Target Class: com.example.Calculator");
        assertThat(summary).contains("Mutation Score: 85.00%");
        assertThat(summary).contains("Total Mutants: 100");
    }

    @Test
    void shouldGenerateDetailedReport() {
        List<Mutant> mutants = List.of(
            Mutant.builder()
                .id("1")
                .mutator("MathMutator")
                .method("add")
                .lineNumber(5)
                .status(MutantStatus.KILLED)
                .description("Replaced + with -")
                .build(),
            Mutant.builder()
                .id("2")
                .mutator("MathMutator")
                .method("subtract")
                .lineNumber(10)
                .status(MutantStatus.SURVIVED)
                .description("Replaced - with +")
                .build()
        );

        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(2)
            .killedMutants(1)
            .survivedMutants(1)
            .mutants(mutants)
            .build();

        String detailed = generator.generateDetailed(report);

        assertThat(detailed).contains("Mutant #1");
        assertThat(detailed).contains("Mutant #2");
        assertThat(detailed).contains("KILLED");
        assertThat(detailed).contains("SURVIVED");
    }

    @Test
    void shouldGenerateHtmlReport() throws IOException {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(50)
            .killedMutants(40)
            .survivedMutants(10)
            .build();

        File htmlFile = generator.generateHtml(report, outputDir);

        assertThat(htmlFile).exists();
        String content = Files.readString(htmlFile.toPath());
        assertThat(content).contains("<!DOCTYPE html>");
        assertThat(content).contains("Mutation Test Report");
        assertThat(content).contains("com.example.Calculator");
    }

    @Test
    void shouldGenerateJsonReport() throws IOException {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(50)
            .killedMutants(40)
            .survivedMutants(10)
            .build();

        File jsonFile = generator.generateJson(report, outputDir);

        assertThat(jsonFile).exists();
        assertThat(jsonFile.getName()).isEqualTo("mutation-report.json");
        String content = Files.readString(jsonFile.toPath());
        assertThat(content).contains("\"targetClass\"");
        assertThat(content).contains("\"mutationScore\"");
    }

    @Test
    void shouldGenerateCsvReport() throws IOException {
        List<Mutant> mutants = List.of(
            Mutant.builder()
                .id("1")
                .mutator("MathMutator")
                .method("add")
                .lineNumber(5)
                .status(MutantStatus.KILLED)
                .description("Replaced + with -")
                .build(),
            Mutant.builder()
                .id("2")
                .mutator("NullMutator")
                .method("process")
                .lineNumber(15)
                .status(MutantStatus.SURVIVED)
                .description("Returned null")
                .build()
        );

        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(2)
            .killedMutants(1)
            .survivedMutants(1)
            .mutants(mutants)
            .build();

        File csvFile = generator.generateCsv(report, outputDir);

        assertThat(csvFile).exists();
        assertThat(csvFile.getName()).isEqualTo("mutation-report.csv");
        String content = Files.readString(csvFile.toPath());
        assertThat(content).contains("ID,Mutator,Method,Line,Status,Description");
    }

    @Test
    void shouldGenerateMarkdownReport() throws IOException {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(100)
            .killedMutants(85)
            .survivedMutants(15)
            .build();

        File mdFile = generator.generateMarkdown(report, outputDir);

        assertThat(mdFile).exists();
        assertThat(mdFile.getName()).isEqualTo("mutation-report.md");
        String content = Files.readString(mdFile.toPath());
        assertThat(content).contains("# Mutation Test Report");
        assertThat(content).contains("| Metric | Value |");
    }

    @Test
    void shouldGenerateComparisonReport() {
        MutationReport report1 = MutationReport.builder()
            .targetClass("Class1")
            .totalMutants(100)
            .killedMutants(80)
            .build();

        MutationReport report2 = MutationReport.builder()
            .targetClass("Class2")
            .totalMutants(100)
            .killedMutants(90)
            .build();

        String comparison = generator.generateComparison(report1, report2);

        assertThat(comparison).contains("Mutation Score Comparison");
        assertThat(comparison).contains("Class1: 80.00%");
        assertThat(comparison).contains("Class2: 90.00%");
    }

    @Test
    void shouldGenerateTrendReport() {
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

        String trend = generator.generateTrend(List.of(report1, report2, report3));

        assertThat(trend).contains("Mutation Score Trend");
        assertThat(trend).contains("60.00%");
        assertThat(trend).contains("70.00%");
        assertThat(trend).contains("80.00%");
    }

    @Test
    void shouldGenerateQualityAssessment() {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(100)
            .killedMutants(92)
            .survivedMutants(8)
            .build();

        String assessment = generator.generateQualityAssessment(report);

        assertThat(assessment).contains("Quality Assessment");
        assertThat(assessment).containsIgnoringCase("EXCELLENT");
    }

    @Test
    void shouldGenerateRecommendations() {
        List<Mutant> mutants = List.of(
            Mutant.builder()
                .id("1")
                .mutator("MathMutator")
                .method("add")
                .lineNumber(5)
                .status(MutantStatus.SURVIVED)
                .description("Replaced + with -")
                .build(),
            Mutant.builder()
                .id("2")
                .mutator("NullMutator")
                .method("process")
                .lineNumber(15)
                .status(MutantStatus.SURVIVED)
                .description("Returned null")
                .build()
        );

        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(2)
            .killedMutants(0)
            .survivedMutants(2)
            .mutants(mutants)
            .build();

        List<String> recommendations = generator.generateRecommendations(report);

        assertThat(recommendations).isNotEmpty();
        assertThat(recommendations).anyMatch(r -> r.contains("add"));
        assertThat(recommendations).anyMatch(r -> r.contains("process"));
    }

    @Test
    void shouldHandleEmptyReport() {
        MutationReport emptyReport = MutationReport.builder()
            .targetClass("com.example.EmptyClass")
            .totalMutants(0)
            .killedMutants(0)
            .build();

        assertThatCode(() -> generator.generateSummary(emptyReport))
            .doesNotThrowAnyException();

        assertThatCode(() -> generator.generateDetailed(emptyReport))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldGenerateReportWithCustomTemplate() throws IOException {
        String template = """
            === Custom Mutation Report ===
            Class: {{targetClass}}
            Score: {{mutationScore}}%
            """;

        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(100)
            .killedMutants(85)
            .build();

        String customReport = generator.generateWithTemplate(report, template);

        assertThat(customReport).contains("=== Custom Mutation Report ===");
        assertThat(customReport).contains("Class: com.example.Calculator");
        assertThat(customReport).contains("Score: 85.00%");
    }
}
