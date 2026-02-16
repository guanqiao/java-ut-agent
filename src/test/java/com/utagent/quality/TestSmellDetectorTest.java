package com.utagent.quality;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TestSmellDetectorTest {

    private TestSmellDetector detector;

    @BeforeEach
    void setUp() {
        detector = new TestSmellDetector();
    }

    @Test
    void shouldDetectDuplicateAssertions() {
        String testCode = """
            @Test
            void testAdd() {
                Calculator calc = new Calculator();
                assertEquals(5, calc.add(2, 3));
                assertEquals(5, calc.add(2, 3));
                assertEquals(5, calc.add(2, 3));
            }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).anyMatch(s -> s.type() == TestSmellType.DUPLICATE_ASSERTION);
    }

    @Test
    void shouldDetectMagicNumbers() {
        String testCode = """
            @Test
            void testCalculation() {
                Calculator calc = new Calculator();
                assertEquals(42, calc.multiply(6, 7));
                assertEquals(100, calc.add(50, 50));
            }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).anyMatch(s -> s.type() == TestSmellType.MAGIC_NUMBER);
    }

    @Test
    void shouldDetectLongTestMethod() {
        StringBuilder longTest = new StringBuilder();
        longTest.append("@Test\nvoid testLongMethod() {\n");
        for (int i = 0; i < 60; i++) {
            longTest.append("    int x").append(i).append(" = ").append(i).append(";\n");
        }
        longTest.append("}");

        List<TestSmell> smells = detector.detect(longTest.toString());

        assertThat(smells).anyMatch(s -> s.type() == TestSmellType.LONG_TEST_METHOD);
    }

    @Test
    void shouldDetectEmptyTest() {
        String testCode = """
            @Test
            void testNothing() {
            }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).anyMatch(s -> s.type() == TestSmellType.EMPTY_TEST);
    }

    @Test
    void shouldDetectMissingAssertions() {
        String testCode = """
            @Test
            void testWithoutAssertion() {
                Calculator calc = new Calculator();
                calc.add(2, 3);
            }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).anyMatch(s -> s.type() == TestSmellType.MISSING_ASSERTION);
    }

    @Test
    void shouldDetectMultipleAssertions() {
        String testCode = """
            @Test
            void testMultipleAssertions() {
                Calculator calc = new Calculator();
                assertEquals(1, calc.add(0, 1));
                assertEquals(2, calc.add(1, 1));
                assertEquals(3, calc.add(1, 2));
                assertEquals(4, calc.add(2, 2));
                assertEquals(5, calc.add(2, 3));
                assertEquals(6, calc.add(3, 3));
                assertEquals(7, calc.add(3, 4));
            }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).anyMatch(s -> s.type() == TestSmellType.MULTIPLE_ASSERTIONS);
    }

    @Test
    void shouldDetectPrintStatements() {
        String testCode = """
            @Test
            void testWithPrint() {
                Calculator calc = new Calculator();
                System.out.println("Testing add");
                assertEquals(5, calc.add(2, 3));
            }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).anyMatch(s -> s.type() == TestSmellType.PRINT_STATEMENT);
    }

    @Test
    void shouldDetectIgnoredTest() {
        String testCode = """
            @Disabled
            @Test
            void testDisabled() {
                Calculator calc = new Calculator();
                assertEquals(5, calc.add(2, 3));
            }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).anyMatch(s -> s.type() == TestSmellType.IGNORED_TEST);
    }

    @Test
    void shouldDetectComplexSetup() {
        String testCode = """
            private Calculator calc;
            
            @BeforeEach
            void setUp() {
                calc = new Calculator();
                calc.configure("mode", "advanced");
                calc.setPrecision(10);
                calc.enableCache(true);
                calc.setMemoryLimit(1024);
                calc.initialize();
                calibrate(calc);
                validateState(calc);
            }
            
            private void calibrate(Calculator c) { }
            private void validateState(Calculator c) { }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).anyMatch(s -> s.type() == TestSmellType.COMPLEX_SETUP);
    }

    @Test
    void shouldDetectAssertionRoulette() {
        String testCode = """
            @Test
            void testWithoutMessage() {
                Calculator calc = new Calculator();
                assertEquals(5, calc.add(2, 3));
                assertEquals(2, calc.subtract(5, 3));
                assertEquals(6, calc.multiply(2, 3));
            }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).anyMatch(s -> s.type() == TestSmellType.ASSERTION_ROULETTE);
    }

    @Test
    void shouldDetectEagerTest() {
        String testCode = """
            @Test
            void testMultipleBehaviors() {
                Calculator calc = new Calculator();
                assertEquals(5, calc.add(2, 3));
                assertEquals(2, calc.subtract(5, 3));
                assertEquals(6, calc.multiply(2, 3));
                assertEquals(2, calc.divide(6, 3));
            }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).anyMatch(s -> s.type() == TestSmellType.EAGER_TEST);
    }

    @Test
    void shouldDetectGeneralFixture() {
        String testCode = """
            private static Calculator sharedCalc;
            
            @BeforeAll
            static void setUpAll() {
                sharedCalc = new Calculator();
            }
            
            @Test
            void test1() {
                assertEquals(5, sharedCalc.add(2, 3));
            }
            
            @Test
            void test2() {
                assertEquals(2, sharedCalc.subtract(5, 3));
            }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).anyMatch(s -> s.type() == TestSmellType.GENERAL_FIXTURE);
    }

    @Test
    void shouldReturnEmptyListForCleanCode() {
        String testCode = """
            @Test
            @DisplayName("Should add two positive numbers")
            void shouldAddTwoPositiveNumbers() {
                Calculator calc = new Calculator();
                int expected = 5;
                int actual = calc.add(2, 3);
                assertEquals(expected, actual, "Addition should work correctly");
            }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).noneMatch(s -> s.type() == TestSmellType.EMPTY_TEST);
        assertThat(smells).noneMatch(s -> s.type() == TestSmellType.DUPLICATE_ASSERTION);
        assertThat(smells).noneMatch(s -> s.type() == TestSmellType.LONG_TEST_METHOD);
    }

    @Test
    void shouldProvideSmellSeverity() {
        String testCode = """
            @Test
            void testWithPrint() {
                System.out.println("debug");
            }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).isNotEmpty();
        TestSmell smell = smells.stream()
            .filter(s -> s.type() == TestSmellType.PRINT_STATEMENT)
            .findFirst()
            .orElse(null);
        assertThat(smell).isNotNull();
        assertThat(smell.severity()).isNotNull();
        assertThat(smell.severity()).isIn(TestSmellSeverity.values());
    }

    @Test
    void shouldProvideSmellLocation() {
        String testCode = """
            @Test
            void testWithPrint() {
                System.out.println("debug");
                assertEquals(5, 5);
            }
            """;

        List<TestSmell> smells = detector.detect(testCode);

        assertThat(smells).isNotEmpty();
        TestSmell smell = smells.get(0);
        assertThat(smell.lineNumber()).isGreaterThan(0);
    }

    @Test
    void shouldCalculateSmellScore() {
        String cleanCode = """
            @Test
            void testClean() {
                Calculator calc = new Calculator();
                assertEquals(5, calc.add(2, 3));
            }
            """;

        String smellyCode = """
            @Test
            void testSmelly() {
                System.out.println("test");
                Calculator calc = new Calculator();
                assertEquals(5, calc.add(2, 3));
                assertEquals(5, calc.add(2, 3));
                assertEquals(5, calc.add(2, 3));
            }
            """;

        double cleanScore = detector.calculateSmellScore(cleanCode);
        double smellyScore = detector.calculateSmellScore(smellyCode);

        assertThat(cleanScore).isGreaterThan(smellyScore);
    }

    @Test
    void shouldGenerateSmellReport() {
        String testCode = """
            @Test
            void testWithIssues() {
                System.out.println("debug");
                Calculator calc = new Calculator();
                assertEquals(42, calc.add(2, 40));
                assertEquals(42, calc.add(2, 40));
            }
            """;

        String report = detector.generateSmellReport(testCode);

        assertThat(report).containsIgnoringCase("smell");
        assertThat(report).containsIgnoringCase("summary");
    }
}
