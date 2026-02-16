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

class MutationAnalyzerTest {

    @TempDir
    Path tempDir;

    private MutationAnalyzer analyzer;
    private File projectRoot;

    @BeforeEach
    void setUp() throws IOException {
        projectRoot = tempDir.toFile();
        analyzer = new MutationAnalyzer(projectRoot);
    }

    @Test
    void shouldCreateMutationAnalyzerWithProjectRoot() {
        assertThat(analyzer).isNotNull();
        assertThat(analyzer.getProjectRoot()).isEqualTo(projectRoot);
    }

    @Test
    void shouldReturnEmptyReportWhenNoTestClasses() {
        MutationReport report = analyzer.analyze();

        assertThat(report).isNotNull();
        assertThat(report.getMutationScore()).isEqualTo(0.0);
        assertThat(report.getTotalMutants()).isEqualTo(0);
    }

    @Test
    void shouldDetectMutationsInSimpleClass() throws IOException {
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

        MutationReport report = analyzer.analyze("com.example.Calculator");

        assertThat(report).isNotNull();
        assertThat(report.getTargetClass()).isEqualTo("com.example.Calculator");
    }

    @Test
    void shouldCalculateMutationScoreCorrectly() {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(10)
            .killedMutants(8)
            .survivedMutants(2)
            .build();

        assertThat(report.getMutationScore()).isEqualTo(0.8);
    }

    @Test
    void shouldHandleZeroMutants() {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.EmptyClass")
            .totalMutants(0)
            .killedMutants(0)
            .survivedMutants(0)
            .build();

        assertThat(report.getMutationScore()).isEqualTo(0.0);
    }

    @Test
    void shouldReturnMutantDetails() {
        Mutant mutant = Mutant.builder()
            .id("1")
            .mutator("org.pitest.mutationtest.engine.gregor.mutators.MathMutator")
            .method("add")
            .lineNumber(5)
            .status(MutantStatus.KILLED)
            .description("Replaced integer addition with subtraction")
            .build();

        assertThat(mutant.id()).isEqualTo("1");
        assertThat(mutant.status()).isEqualTo(MutantStatus.KILLED);
        assertThat(mutant.isKilled()).isTrue();
    }

    @Test
    void shouldIdentifySurvivedMutants() {
        Mutant survivedMutant = Mutant.builder()
            .id("2")
            .mutator("org.pitest.mutationtest.engine.gregor.mutators.MathMutator")
            .method("subtract")
            .lineNumber(10)
            .status(MutantStatus.SURVIVED)
            .description("Replaced integer subtraction with addition")
            .build();

        assertThat(survivedMutant.status()).isEqualTo(MutantStatus.SURVIVED);
        assertThat(survivedMutant.isKilled()).isFalse();
    }

    @Test
    void shouldSupportMultipleMutators() {
        List<String> mutators = List.of(
            "DEFAULTS",
            "NON_VOID_METHOD_CALL_MUTATOR",
            "NULL_RETURN_VALUES"
        );

        MutationAnalyzer customAnalyzer = new MutationAnalyzer(projectRoot, mutators);
        assertThat(customAnalyzer.getMutators()).containsExactlyElementsOf(mutators);
    }

    @Test
    void shouldGenerateMutationReportInXml() throws IOException {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(10)
            .killedMutants(8)
            .survivedMutants(2)
            .build();

        File outputDir = new File(projectRoot, "target/pit-reports");
        outputDir.mkdirs();

        File xmlReport = analyzer.generateXmlReport(report, outputDir);

        assertThat(xmlReport).exists();
        assertThat(xmlReport.getName()).endsWith(".xml");
    }

    @Test
    void shouldGenerateMutationReportInHtml() throws IOException {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(10)
            .killedMutants(8)
            .survivedMutants(2)
            .build();

        File outputDir = new File(projectRoot, "target/pit-reports");
        outputDir.mkdirs();

        File htmlReport = analyzer.generateHtmlReport(report, outputDir);

        assertThat(htmlReport).exists();
        assertThat(htmlReport.getName()).endsWith(".html");
    }

    @Test
    void shouldAnalyzeSpecificMethod() throws IOException {
        File srcDir = new File(projectRoot, "src/main/java/com/example");
        File testDir = new File(projectRoot, "src/test/java/com/example");
        srcDir.mkdirs();
        testDir.mkdirs();

        String sourceCode = """
            package com.example;
            public class Calculator {
                public int add(int a, int b) { return a + b; }
                public int multiply(int a, int b) { return a * b; }
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
                }
            }
            """;

        Files.writeString(new File(srcDir, "Calculator.java").toPath(), sourceCode);
        Files.writeString(new File(testDir, "CalculatorTest.java").toPath(), testCode);

        MutationReport report = analyzer.analyzeMethod("com.example.Calculator", "add");

        assertThat(report).isNotNull();
    }

    @Test
    void shouldProvideMutationSummary() {
        MutationReport report = MutationReport.builder()
            .targetClass("com.example.Calculator")
            .totalMutants(100)
            .killedMutants(85)
            .survivedMutants(10)
            .timeoutMutants(3)
            .memoryErrorMutants(2)
            .build();

        String summary = report.getSummary();

        assertThat(summary).contains("Mutation Score: 85.00%");
        assertThat(summary).contains("Total Mutants: 100");
        assertThat(summary).contains("Killed: 85");
        assertThat(summary).contains("Survived: 10");
    }

    @Test
    void shouldCompareMutationScores() {
        MutationReport report1 = MutationReport.builder()
            .targetClass("Class1")
            .totalMutants(10)
            .killedMutants(8)
            .build();

        MutationReport report2 = MutationReport.builder()
            .targetClass("Class2")
            .totalMutants(10)
            .killedMutants(9)
            .build();

        assertThat(report2.getMutationScore()).isGreaterThan(report1.getMutationScore());
    }

    @Test
    void shouldHandleAnalysisException() {
        File nonExistentRoot = new File("/non/existent/path");
        MutationAnalyzer failingAnalyzer = new MutationAnalyzer(nonExistentRoot);

        assertThatCode(() -> failingAnalyzer.analyze())
            .doesNotThrowAnyException();
    }
}
