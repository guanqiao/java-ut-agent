package com.utagent.maintenance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChangeImpactAnalyzerTest {

    @TempDir
    Path tempDir;

    private ChangeImpactAnalyzer analyzer;
    private File projectRoot;

    @BeforeEach
    void setUp() throws IOException {
        projectRoot = tempDir.toFile();
        analyzer = new ChangeImpactAnalyzer(projectRoot);
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
    }

    @Test
    void shouldCreateAnalyzerWithProjectRoot() {
        assertThat(analyzer).isNotNull();
        assertThat(analyzer.getProjectRoot()).isEqualTo(projectRoot);
    }

    @Test
    void shouldAnalyzeMethodChange() throws IOException {
        File sourceFile = new File(projectRoot, "src/main/java/com/example/Calculator.java");
        String oldContent = Files.readString(sourceFile.toPath());
        String newContent = oldContent.replace("return a + b;", "return a + b + 1;");

        ImpactAnalysisResult result = analyzer.analyzeChange(sourceFile, oldContent, newContent);

        assertThat(result).isNotNull();
        assertThat(result.changedMethods()).contains("add");
    }

    @Test
    void shouldIdentifyAffectedTests() throws IOException {
        File sourceFile = new File(projectRoot, "src/main/java/com/example/Calculator.java");
        String oldContent = Files.readString(sourceFile.toPath());
        String newContent = oldContent.replace("return a + b;", "return a + b + 1;");

        ImpactAnalysisResult result = analyzer.analyzeChange(sourceFile, oldContent, newContent);

        assertThat(result.affectedTests()).contains("CalculatorTest");
    }

    @Test
    void shouldDetectNoChange() throws IOException {
        File sourceFile = new File(projectRoot, "src/main/java/com/example/Calculator.java");
        String content = Files.readString(sourceFile.toPath());

        ImpactAnalysisResult result = analyzer.analyzeChange(sourceFile, content, content);

        assertThat(result.changedMethods()).isEmpty();
        assertThat(result.impactLevel()).isEqualTo(ImpactLevel.NONE);
    }

    @Test
    void shouldCalculateImpactSeverity() {
        ImpactAnalysisResult highImpact = ImpactAnalysisResult.builder()
            .impactLevel(ImpactLevel.HIGH)
            .build();

        ImpactAnalysisResult lowImpact = ImpactAnalysisResult.builder()
            .impactLevel(ImpactLevel.LOW)
            .build();

        assertThat(highImpact.impactLevel()).isEqualTo(ImpactLevel.HIGH);
        assertThat(lowImpact.impactLevel()).isEqualTo(ImpactLevel.LOW);
    }

    @Test
    void shouldIdentifyNewMethod() throws IOException {
        File sourceFile = new File(projectRoot, "src/main/java/com/example/Calculator.java");
        String oldContent = Files.readString(sourceFile.toPath());
        String newContent = oldContent + """
            
                public int multiply(int a, int b) {
                    return a * b;
                }
            """;

        ImpactAnalysisResult result = analyzer.analyzeChange(sourceFile, oldContent, newContent);

        assertThat(result.addedMethods()).contains("multiply");
    }

    @Test
    void shouldIdentifyDeletedMethod() throws IOException {
        File sourceFile = new File(projectRoot, "src/main/java/com/example/Calculator.java");
        String oldContent = Files.readString(sourceFile.toPath());
        String newContent = oldContent.replaceFirst("public int subtract\\(int a, int b\\) \\{[^}]+\\}", "");

        ImpactAnalysisResult result = analyzer.analyzeChange(sourceFile, oldContent, newContent);

        assertThat(result.deletedMethods()).contains("subtract");
    }

    @Test
    void shouldGenerateImpactReport() {
        ImpactAnalysisResult result = ImpactAnalysisResult.builder()
            .sourceFile("Calculator.java")
            .changedMethod("add")
            .affectedTest("CalculatorTest")
            .impactLevel(ImpactLevel.HIGH)
            .build();

        String report = analyzer.generateImpactReport(result);

        assertThat(report).contains("Impact Analysis Report");
        assertThat(report).contains("Calculator.java");
        assertThat(report).containsIgnoringCase("HIGH");
    }

    @Test
    void shouldPrioritizeAffectedTests() {
        List<ImpactAnalysisResult> impacts = List.of(
            ImpactAnalysisResult.builder()
                .affectedTest("CriticalTest")
                .impactLevel(ImpactLevel.HIGH)
                .build(),
            ImpactAnalysisResult.builder()
                .affectedTest("MinorTest")
                .impactLevel(ImpactLevel.LOW)
                .build(),
            ImpactAnalysisResult.builder()
                .affectedTest("MediumTest")
                .impactLevel(ImpactLevel.MEDIUM)
                .build()
        );

        List<String> prioritized = analyzer.prioritizeTests(impacts);

        assertThat(prioritized.get(0)).isEqualTo("CriticalTest");
    }

    @Test
    void shouldDetectSignatureChange() throws IOException {
        File sourceFile = new File(projectRoot, "src/main/java/com/example/Calculator.java");
        String oldContent = Files.readString(sourceFile.toPath());
        String newContent = oldContent.replace("public int add(int a, int b)", "public int add(int a, int b, int c)");

        ImpactAnalysisResult result = analyzer.analyzeChange(sourceFile, oldContent, newContent);

        assertThat(result.hasSignatureChange()).isTrue();
    }

    @Test
    void shouldSuggestTestUpdates() {
        ImpactAnalysisResult result = ImpactAnalysisResult.builder()
            .sourceFile("Calculator.java")
            .changedMethod("add")
            .hasSignatureChange(true)
            .build();

        List<String> suggestions = analyzer.suggestTestUpdates(result);

        assertThat(suggestions).isNotEmpty();
        assertThat(suggestions).anyMatch(s -> s.contains("signature"));
    }

    @Test
    void shouldAnalyzeMultipleFiles() throws IOException {
        File file1 = new File(projectRoot, "src/main/java/com/example/Calculator.java");
        File file2 = new File(projectRoot, "src/test/java/com/example/CalculatorTest.java");

        String content1 = Files.readString(file1.toPath());
        String content2 = Files.readString(file2.toPath());

        String modified1 = content1.replace("return a + b;", "return a + b + 1;");
        String modified2 = content2.replace("assertEquals(5", "assertEquals(6");

        List<ImpactAnalysisResult> results = analyzer.analyzeChanges(List.of(
            new FileChange(file1, content1, modified1),
            new FileChange(file2, content2, modified2)
        ));

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldCalculateTestCoverageImpact() {
        Set<String> existingTests = Set.of("testAdd", "testSubtract");
        Set<String> changedMethods = Set.of("multiply");

        double coverage = analyzer.calculateCoverageImpact(existingTests, changedMethods);

        assertThat(coverage).isEqualTo(0.0);
    }

    @Test
    void shouldIdentifyOrphanedTests() throws IOException {
        File testFile = new File(projectRoot, "src/test/java/com/example/CalculatorTest.java");
        String testContent = Files.readString(testFile.toPath());

        String newTestContent = testContent + """
            
                @Test
                void testMultiply() {
                    Calculator calc = new Calculator();
                    assertEquals(6, calc.multiply(2, 3));
                }
            """;

        Files.writeString(testFile.toPath(), newTestContent);

        Set<String> orphanedTests = analyzer.findOrphanedTests(projectRoot);

        assertThat(orphanedTests).contains("testMultiply");
    }
}
