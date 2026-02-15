package com.utagent.optimizer;

import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("OptimizationResult Tests")
class OptimizationResultTest {

    private OptimizationResult result;

    @BeforeEach
    void setUp() {
        result = new OptimizationResult();
    }

    @Test
    @DisplayName("Should set and get source file")
    void shouldSetAndGetSourceFile() {
        // Given
        File sourceFile = new File("/path/to/Source.java");

        // When
        result.setSourceFile(sourceFile);

        // Then
        assertEquals(sourceFile, result.getSourceFile());
    }

    @Test
    @DisplayName("Should set and get class info")
    void shouldSetAndGetClassInfo() {
        // Given
        ClassInfo classInfo = mock(ClassInfo.class);
        when(classInfo.className()).thenReturn("TestClass");

        // When
        result.setClassInfo(classInfo);

        // Then
        assertEquals(classInfo, result.getClassInfo());
    }

    @Test
    @DisplayName("Should set and get generated test file")
    void shouldSetAndGetGeneratedTestFile() {
        // Given
        File testFile = new File("/path/to/Test.java");

        // When
        result.setGeneratedTestFile(testFile);

        // Then
        assertEquals(testFile, result.getGeneratedTestFile());
    }

    @Test
    @DisplayName("Should add coverage reports to history")
    void shouldAddCoverageReportsToHistory() {
        // Given
        CoverageReport report1 = mock(CoverageReport.class);
        CoverageReport report2 = mock(CoverageReport.class);

        // When
        result.addCoverageReport(0, report1);
        result.addCoverageReport(1, report2);

        // Then
        assertEquals(2, result.getCoverageHistory().size());
        assertEquals(report1, result.getCoverageHistory().get(0));
        assertEquals(report2, result.getCoverageHistory().get(1));
    }

    @Test
    @DisplayName("Should set and get final coverage")
    void shouldSetAndGetFinalCoverage() {
        // Given
        CoverageReport finalCoverage = mock(CoverageReport.class);

        // When
        result.setFinalCoverage(finalCoverage);

        // Then
        assertEquals(finalCoverage, result.getFinalCoverage());
    }

    @Test
    @DisplayName("Should set and check success status")
    void shouldSetAndCheckSuccessStatus() {
        // When
        result.setSuccess(true);

        // Then
        assertTrue(result.isSuccess());

        // When
        result.setSuccess(false);

        // Then
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("Should set and get error message")
    void shouldSetAndGetErrorMessage() {
        // Given
        String errorMessage = "Something went wrong";

        // When
        result.setErrorMessage(errorMessage);

        // Then
        assertEquals(errorMessage, result.getErrorMessage());
    }

    @Test
    @DisplayName("Should set and get iterations")
    void shouldSetAndGetIterations() {
        // Given
        int iterations = 5;

        // When
        result.setIterations(iterations);

        // Then
        assertEquals(iterations, result.getIterations());
    }

    @Test
    @DisplayName("Should add generated test methods")
    void shouldAddGeneratedTestMethods() {
        // When
        result.addGeneratedTestMethod("testMethod1");
        result.addGeneratedTestMethod("testMethod2");

        // Then
        List<String> methods = result.getGeneratedTestMethods();
        assertEquals(2, methods.size());
        assertTrue(methods.contains("testMethod1"));
        assertTrue(methods.contains("testMethod2"));
    }

    @Test
    @DisplayName("Should calculate coverage improvement correctly")
    void shouldCalculateCoverageImprovementCorrectly() {
        // Given
        CoverageReport initial = mock(CoverageReport.class);
        when(initial.overallLineCoverage()).thenReturn(0.3);

        CoverageReport finalReport = mock(CoverageReport.class);
        when(finalReport.overallLineCoverage()).thenReturn(0.8);

        // When
        result.addCoverageReport(0, initial);
        result.setFinalCoverage(finalReport);

        // Then
        assertEquals(0.5, result.getCoverageImprovement(), 0.001);
    }

    @Test
    @DisplayName("Should return zero coverage improvement when history is empty")
    void shouldReturnZeroCoverageImprovementWhenHistoryIsEmpty() {
        // When & Then
        assertEquals(0.0, result.getCoverageImprovement(), 0.001);
    }

    @Test
    @DisplayName("Should return zero coverage improvement when reports are null")
    void shouldReturnZeroCoverageImprovementWhenReportsAreNull() {
        // When
        result.addCoverageReport(0, null);

        // Then
        assertEquals(0.0, result.getCoverageImprovement(), 0.001);
    }

    @Test
    @DisplayName("Should generate summary with all fields")
    void shouldGenerateSummaryWithAllFields() {
        // Given
        File sourceFile = new File("/path/to/Source.java");
        ClassInfo classInfo = mock(ClassInfo.class);
        when(classInfo.className()).thenReturn("TestClass");

        File testFile = new File("/path/to/SourceTest.java");

        CoverageReport finalCoverage = mock(CoverageReport.class);
        when(finalCoverage.overallLineCoverage()).thenReturn(0.85);
        when(finalCoverage.overallBranchCoverage()).thenReturn(0.75);

        result.setSourceFile(sourceFile);
        result.setClassInfo(classInfo);
        result.setGeneratedTestFile(testFile);
        result.setSuccess(true);
        result.setIterations(3);
        result.setFinalCoverage(finalCoverage);

        // When
        String summary = result.getSummary();

        // Then
        assertTrue(summary.contains("Source.java"));
        assertTrue(summary.contains("TestClass"));
        assertTrue(summary.contains("SourceTest.java"));
        assertTrue(summary.contains("Success: Yes"));
        assertTrue(summary.contains("Iterations: 3"));
        assertTrue(summary.contains("85.0%"));
        assertTrue(summary.contains("75.0%"));
    }

    @Test
    @DisplayName("Should generate summary with error message")
    void shouldGenerateSummaryWithErrorMessage() {
        // Given
        result.setSourceFile(new File("/path/to/Source.java"));
        result.setSuccess(false);
        result.setErrorMessage("Parsing failed");

        // When
        String summary = result.getSummary();

        // Then
        assertTrue(summary.contains("Success: No"));
        assertTrue(summary.contains("Error: Parsing failed"));
    }

    @Test
    @DisplayName("Should generate summary with N/A for null fields")
    void shouldGenerateSummaryWithNaForNullFields() {
        // When
        String summary = result.getSummary();

        // Then
        assertTrue(summary.contains("N/A"));
    }
}
