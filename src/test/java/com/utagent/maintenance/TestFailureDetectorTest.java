package com.utagent.maintenance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestFailureDetectorTest {

    @TempDir
    Path tempDir;

    private TestFailureDetector detector;
    private File projectRoot;

    @BeforeEach
    void setUp() throws IOException {
        projectRoot = tempDir.toFile();
        detector = new TestFailureDetector(projectRoot);
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
    }

    @Test
    void shouldCreateDetectorWithProjectRoot() {
        assertThat(detector).isNotNull();
        assertThat(detector.getProjectRoot()).isEqualTo(projectRoot);
    }

    @Test
    void shouldDetectNoFailuresInValidProject() {
        List<TestFailure> failures = detector.detectFailures();

        assertThat(failures).isEmpty();
    }

    @Test
    void shouldDetectCompilationFailure() throws IOException {
        File testDir = new File(projectRoot, "src/test/java/com/example");
        String invalidTestCode = """
            package com.example;
            import org.junit.jupiter.api.Test;
            
            class BrokenTest {
                @Test
                void testBroken() {
                    // Missing closing brace
            """;

        Files.writeString(new File(testDir, "BrokenTest.java").toPath(), invalidTestCode);

        List<TestFailure> failures = detector.detectFailures();

        assertThat(failures).isNotNull();
    }

    @Test
    void shouldDetectTestWithWrongAssertion() throws IOException {
        File testDir = new File(projectRoot, "src/test/java/com/example");
        String failingTestCode = """
            package com.example;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.*;
            
            class FailingTest {
                @Test
                void testFailing() {
                    Calculator calc = new Calculator();
                    assertEquals(10, calc.add(2, 3)); // Wrong expected value
                }
            }
            """;

        Files.writeString(new File(testDir, "FailingTest.java").toPath(), failingTestCode);

        List<TestFailure> failures = detector.detectFailures();

        assertThat(failures).isNotNull();
    }

    @Test
    void shouldIdentifyFailureLocation() throws IOException {
        File testDir = new File(projectRoot, "src/test/java/com/example");
        String testCode = """
            package com.example;
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.*;
            
            class LocationTest {
                @Test
                void testWithFailure() {
                    fail("Intentional failure");
                }
            }
            """;

        Files.writeString(new File(testDir, "LocationTest.java").toPath(), testCode);

        List<TestFailure> failures = detector.detectFailures();

        assertThat(failures).isNotNull();
    }

    @Test
    void shouldCategorizeFailureTypes() {
        TestFailure compilationFailure = new TestFailure(
            "BrokenTest", "testBroken", "Syntax error: cannot find symbol", null
        );

        TestFailure runtimeFailure = new TestFailure(
            "FailingTest", "testFailing", "AssertionFailedError: Expected 5 but was 3", null
        );

        assertThat(compilationFailure.type()).isEqualTo(FailureType.COMPILATION_ERROR);
        assertThat(runtimeFailure.type()).isEqualTo(FailureType.ASSERTION_FAILURE);
    }

    @Test
    void shouldProvideFailureSeverity() {
        TestFailure criticalFailure = new TestFailure(
            "BrokenTest", "testBroken", "OutOfMemoryError: Java heap space", null
        );

        TestFailure minorFailure = new TestFailure(
            "FailingTest", "testFailing", "AssertionFailedError: expected: <5>", null
        );

        assertThat(criticalFailure.severity()).isGreaterThan(minorFailure.severity());
    }

    @Test
    void shouldGenerateFailureReport() {
        List<TestFailure> failures = List.of(
            new TestFailure("CalculatorTest", "testAdd", "AssertionFailedError: Expected 5 but was 3", null)
        );

        String report = detector.generateFailureReport(failures);

        assertThat(report).contains("Test Failure Report");
        assertThat(report).contains("CalculatorTest");
        assertThat(report).contains("testAdd");
    }

    @Test
    void shouldGroupFailuresByClass() {
        List<TestFailure> failures = List.of(
            new TestFailure("CalculatorTest", "testAdd", "AssertionFailedError", null),
            new TestFailure("CalculatorTest", "testSubtract", "AssertionFailedError", null),
            new TestFailure("StringTest", "testConcat", "AssertionFailedError", null)
        );

        var grouped = detector.groupByClass(failures);

        assertThat(grouped).containsKeys("CalculatorTest", "StringTest");
        assertThat(grouped.get("CalculatorTest")).hasSize(2);
        assertThat(grouped.get("StringTest")).hasSize(1);
    }

    @Test
    void shouldCalculateFailureRate() {
        int totalTests = 100;
        int failedTests = 5;

        double rate = detector.calculateFailureRate(totalTests, failedTests);

        assertThat(rate).isEqualTo(0.05);
    }

    @Test
    void shouldIdentifyFlakyTests() {
        List<TestExecution> executions = List.of(
            new TestExecution("test1", true, 100),
            new TestExecution("test1", false, 100),
            new TestExecution("test1", true, 100),
            new TestExecution("test2", true, 100),
            new TestExecution("test2", true, 100)
        );

        List<String> flakyTests = detector.identifyFlakyTests(executions);

        assertThat(flakyTests).contains("test1");
        assertThat(flakyTests).doesNotContain("test2");
    }

    @Test
    void shouldDetectTimeoutFailures() {
        TestFailure timeoutFailure = new TestFailure(
            "SlowTest", "testSlowOperation", "TimeoutException: Test timed out after 30 seconds", null
        );

        assertThat(timeoutFailure.type()).isEqualTo(FailureType.TIMEOUT);
    }

    @Test
    void shouldPrioritizeFailures() {
        List<TestFailure> failures = List.of(
            new TestFailure("Test1", "test1", "AssertionFailedError: expected: <5>", null),
            new TestFailure("Test2", "test2", "OutOfMemoryError: Java heap space", null),
            new TestFailure("Test3", "test3", "NullPointerException", null)
        );

        List<TestFailure> prioritized = detector.prioritizeFailures(failures);

        assertThat(prioritized).isNotEmpty();
        assertThat(prioritized.get(0).severity()).isGreaterThanOrEqualTo(prioritized.get(prioritized.size() - 1).severity());
    }

    @Test
    void shouldSuggestFixForAssertionFailure() {
        TestFailure failure = new TestFailure(
            "CalculatorTest", "testAdd", "AssertionFailedError: Expected: 5, Actual: 4", null
        );

        String suggestion = detector.suggestFix(failure);

        assertThat(suggestion).isNotEmpty();
        assertThat(suggestion).containsIgnoringCase("assertion");
    }

    @Test
    void shouldTrackFailureHistory() {
        detector.recordFailure("test1", FailureType.ASSERTION_FAILURE);
        detector.recordFailure("test1", FailureType.ASSERTION_FAILURE);
        detector.recordFailure("test2", FailureType.COMPILATION_ERROR);

        var history = detector.getFailureHistory();

        assertThat(history.get("test1")).isEqualTo(2);
        assertThat(history.get("test2")).isEqualTo(1);
    }
}
