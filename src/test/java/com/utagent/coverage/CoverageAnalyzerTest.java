package com.utagent.coverage;

import com.utagent.model.CoverageInfo;
import com.utagent.model.CoverageReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CoverageAnalyzer Tests")
class CoverageAnalyzerTest {

    private CoverageAnalyzer analyzer;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        analyzer = new CoverageAnalyzer(tempDir.toFile());
    }

    @Test
    @DisplayName("Should create analyzer with default paths")
    void shouldCreateAnalyzerWithDefaultPaths() {
        CoverageAnalyzer defaultAnalyzer = new CoverageAnalyzer(tempDir.toFile());

        assertNotNull(defaultAnalyzer);
    }

    @Test
    @DisplayName("Should create analyzer with custom paths")
    void shouldCreateAnalyzerWithCustomPaths() {
        CoverageAnalyzer customAnalyzer = new CoverageAnalyzer(
            tempDir.toFile(),
            "build/classes",
            "src/main/java"
        );

        assertNotNull(customAnalyzer);
    }

    @Test
    @DisplayName("Should return empty report for non-existent exec file")
    void shouldReturnEmptyReportForNonExistentExecFile() {
        File nonExistentFile = new File(tempDir.toFile(), "non-existent.exec");

        CoverageReport report = analyzer.analyzeCoverage(nonExistentFile);

        assertNotNull(report);
        assertEquals(0.0, report.overallLineCoverage());
        assertTrue(report.classCoverages().isEmpty());
    }

    @Test
    @DisplayName("Should return empty report for non-existent XML file")
    void shouldReturnEmptyReportForNonExistentXmlFile() {
        File nonExistentFile = new File(tempDir.toFile(), "non-existent.xml");

        CoverageReport report = analyzer.analyzeFromJacocoXml(nonExistentFile);

        assertNotNull(report);
        assertEquals(0.0, report.overallLineCoverage());
    }

    @Test
    @DisplayName("Should format coverage report")
    void shouldFormatCoverageReport() {
        CoverageReport report = new CoverageReport(
            0.75,
            0.60,
            0.80,
            List.of(),
            List.of()
        );

        String formatted = analyzer.formatCoverageReport(report);

        assertNotNull(formatted);
        assertTrue(formatted.contains("Coverage Report"));
        assertTrue(formatted.contains("Line Coverage:"));
        assertTrue(formatted.contains("Branch Coverage:"));
        assertTrue(formatted.contains("75.00%") || formatted.contains("75%"));
    }

    @Test
    @DisplayName("Should check if coverage meets target")
    void shouldCheckIfCoverageMeetsTarget() {
        CoverageReport report = new CoverageReport(
            0.85,
            0.85,
            0.90,
            List.of(),
            List.of()
        );

        assertTrue(analyzer.meetsTarget(report, 0.80));
        assertTrue(analyzer.meetsTarget(report, 0.85));
        assertFalse(analyzer.meetsTarget(report, 0.90));
    }

    @Test
    @DisplayName("Should get uncovered lines from report")
    void shouldGetUncoveredLinesFromReport() {
        CoverageInfo info = new CoverageInfo(
            "com.example.Service",
            "process",
            10,
            2, 1,
            10, 5,
            5, 2
        );

        CoverageReport report = new CoverageReport(
            0.75,
            0.60,
            0.80,
            List.of(info),
            List.of()
        );

        List<Integer> uncoveredLines = analyzer.getUncoveredLines(report, "com.example.Service");

        assertNotNull(uncoveredLines);
    }

    @Test
    @DisplayName("Should get uncovered methods from report")
    void shouldGetUncoveredMethodsFromReport() {
        CoverageInfo coveredMethod = new CoverageInfo(
            "com.example.Service",
            "coveredMethod",
            10,
            2, 0,
            10, 0,
            5, 0
        );

        CoverageInfo uncoveredMethod = new CoverageInfo(
            "com.example.Service",
            "uncoveredMethod",
            20,
            2, 1,
            10, 5,
            5, 2
        );

        CoverageReport report = new CoverageReport(
            0.75,
            0.60,
            0.80,
            List.of(coveredMethod, uncoveredMethod),
            List.of()
        );

        List<CoverageInfo> uncoveredMethods = analyzer.getUncoveredMethods(report, "com.example.Service");

        assertNotNull(uncoveredMethods);
        assertEquals(1, uncoveredMethods.size());
        assertEquals("uncoveredMethod", uncoveredMethods.get(0).methodName());
    }

    @Test
    @DisplayName("Should handle empty coverage report")
    void shouldHandleEmptyCoverageReport() {
        CoverageReport emptyReport = new CoverageReport();

        assertEquals(0.0, emptyReport.overallLineCoverage());
        assertEquals(0.0, emptyReport.overallBranchCoverage());
        assertEquals(0.0, emptyReport.overallInstructionCoverage());
        assertTrue(emptyReport.classCoverages().isEmpty());
    }

    @Test
    @DisplayName("Should calculate coverage rates correctly")
    void shouldCalculateCoverageRatesCorrectly() {
        CoverageInfo info = new CoverageInfo(
            "com.example.Service",
            "",
            0,
            10, 2,
            100, 20,
            50, 10
        );

        double branchRate = info.getBranchCoverageRate();
        double instructionRate = info.getInstructionCoverageRate();
        double lineRate = info.getLineCoverageRate();

        assertEquals(0.8, branchRate, 0.01);
        assertEquals(0.8, instructionRate, 0.01);
        assertEquals(0.8, lineRate, 0.01);
    }

    @Test
    @DisplayName("Should handle zero total counts")
    void shouldHandleZeroTotalCounts() {
        CoverageInfo info = new CoverageInfo(
            "com.example.Service",
            "",
            0,
            0, 0,
            0, 0,
            0, 0
        );

        double branchRate = info.getBranchCoverageRate();
        double instructionRate = info.getInstructionCoverageRate();
        double lineRate = info.getLineCoverageRate();

        assertEquals(1.0, branchRate);
        assertEquals(1.0, instructionRate);
        assertEquals(1.0, lineRate);
    }

    @Test
    @DisplayName("Should create CoverageInfo with all fields")
    void shouldCreateCoverageInfoWithAllFields() {
        CoverageInfo info = new CoverageInfo(
            "com.example.Class",
            "methodName",
            10,
            5, 2,
            20, 5,
            10, 3
        );

        assertEquals("com.example.Class", info.className());
        assertEquals("methodName", info.methodName());
        assertEquals(10, info.lineNumber());
        assertEquals(5, info.branchCount());
        assertEquals(2, info.branchMissed());
        assertEquals(20, info.instructionCount());
        assertEquals(5, info.instructionMissed());
        assertEquals(10, info.lineCount());
        assertEquals(3, info.lineMissed());
    }

    @Test
    @DisplayName("Should create CoverageReport with all fields")
    void shouldCreateCoverageReportWithAllFields() {
        CoverageInfo info = new CoverageInfo(
            "com.example.Class",
            "",
            0,
            10, 2,
            100, 20,
            50, 10
        );

        CoverageReport report = new CoverageReport(
            0.80,
            0.75,
            0.85,
            List.of(info),
            List.of(10, 11, 12)
        );

        assertEquals(0.80, report.overallLineCoverage(), 0.001);
        assertEquals(0.75, report.overallBranchCoverage(), 0.001);
        assertEquals(0.85, report.overallInstructionCoverage(), 0.001);
        assertEquals(1, report.classCoverages().size());
        assertEquals(3, report.uncoveredLines().size());
    }

    @Test
    @DisplayName("Should meet target when coverage equals target")
    void shouldMeetTargetWhenCoverageEqualsTarget() {
        CoverageReport report = new CoverageReport(
            0.80,
            0.80,
            0.80,
            List.of(),
            List.of()
        );

        assertTrue(analyzer.meetsTarget(report, 0.80));
    }

    @Test
    @DisplayName("Should not meet target when branch coverage is low")
    void shouldNotMeetTargetWhenBranchCoverageIsLow() {
        CoverageReport report = new CoverageReport(
            0.90,
            0.70,
            0.90,
            List.of(),
            List.of()
        );

        assertFalse(analyzer.meetsTarget(report, 0.80));
    }
}
