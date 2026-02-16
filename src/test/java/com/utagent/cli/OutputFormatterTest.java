package com.utagent.cli;

import com.utagent.config.OutputConfig;
import com.utagent.model.CoverageReport;
import com.utagent.parser.FrameworkType;
import com.utagent.optimizer.OptimizationResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("OutputFormatter Tests")
class OutputFormatterTest {

    private OutputFormatter outputFormatter;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalErr;

    @Mock
    private OutputConfig outputConfig;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(outputConfig.getVerboseOrDefault()).thenReturn(false);
        
        outputFormatter = new OutputFormatter(outputConfig);
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        
        // Also capture System.err for error messages
        errorStream = new ByteArrayOutputStream();
        originalErr = System.err;
        System.setErr(new PrintStream(errorStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    private String getCombinedOutput() {
        return outputStream.toString() + errorStream.toString();
    }

    @Test
    @DisplayName("Should print banner correctly")
    void shouldPrintBannerCorrectly() {
        outputFormatter.printBanner();
        String output = outputStream.toString();
        
        assertTrue(output.contains("Java UT Agent - AI Test Generator"));
        assertTrue(output.contains("Usage: java-ut-agent <source> [options]"));
        assertTrue(output.contains("LLM Providers:"));
    }

    @Test
    @DisplayName("Should print generation start message")
    void shouldPrintGenerationStartMessage() {
        outputFormatter.printGenerationStart();
        String output = outputStream.toString();
        
        assertTrue(output.contains("Starting test generation"));
    }

    @Test
    @DisplayName("Should print provider info with model")
    void shouldPrintProviderInfoWithModel() {
        outputFormatter.printProviderInfo("OpenAI", "gpt-4");
        String output = outputStream.toString();
        
        assertTrue(output.contains("Using OpenAI provider"));
        assertTrue(output.contains("Model: gpt-4"));
    }

    @Test
    @DisplayName("Should print provider info without model")
    void shouldPrintProviderInfoWithoutModel() {
        outputFormatter.printProviderInfo("OpenAI", null);
        String output = outputStream.toString();
        
        assertTrue(output.contains("Using template-based generation"));
    }

    @Test
    @DisplayName("Should print target info")
    void shouldPrintTargetInfo() {
        outputFormatter.printTargetInfo(0.8, 10);
        String output = outputStream.toString();
        
        assertTrue(output.contains("Target coverage: 80%"));
        assertTrue(output.contains("Max iterations: 10"));
    }

    @Test
    @DisplayName("Should print dry run message")
    void shouldPrintDryRunMessage() {
        outputFormatter.printDryRunMessage();
        String output = outputStream.toString();
        
        assertTrue(output.contains("Dry run mode - generating tests without execution"));
    }

    @Test
    @DisplayName("Should print coverage analysis start message")
    void shouldPrintCoverageAnalysisStartMessage() {
        outputFormatter.printCoverageAnalysisStart();
        String output = outputStream.toString();
        
        assertTrue(output.contains("Analyzing coverage"));
    }

    @Test
    @DisplayName("Should print framework detection start message")
    void shouldPrintFrameworkDetectionStartMessage() {
        outputFormatter.printFrameworkDetectionStart();
        String output = outputStream.toString();
        
        assertTrue(output.contains("Detecting frameworks"));
    }

    @Test
    @DisplayName("Should print frameworks correctly")
    void shouldPrintFrameworksCorrectly() {
        Set<FrameworkType> frameworks = Set.of(FrameworkType.SPRING_BOOT, FrameworkType.MYBATIS);
        outputFormatter.printFrameworks("TestClass", frameworks);
        String output = outputStream.toString();
        
        assertTrue(output.contains("TestClass"));
        assertTrue(output.contains("Spring Boot"));
        assertTrue(output.contains("MyBatis"));
    }

    @Test
    @DisplayName("Should print no frameworks message")
    void shouldPrintNoFrameworksMessage() {
        outputFormatter.printFrameworks("TestClass", Set.of());
        String output = outputStream.toString();
        
        assertTrue(output.contains("TestClass"));
        assertTrue(output.contains("No frameworks detected"));
    }

    @Test
    @DisplayName("Should print config init success message")
    void shouldPrintConfigInitSuccessMessage() {
        outputFormatter.printConfigInitSuccess("/path/to/config.yaml");
        String output = outputStream.toString();
        
        assertTrue(output.contains("Created default configuration file"));
        assertTrue(output.contains("/path/to/config.yaml"));
    }

    @Test
    @DisplayName("Should print config init failure message")
    void shouldPrintConfigInitFailureMessage() {
        outputFormatter.printConfigInitFailure("Error message");
        String output = getCombinedOutput();
        
        assertTrue(output.contains("Failed to create configuration file"));
        assertTrue(output.contains("Error message"));
    }

    @Test
    @DisplayName("Should print source not found error")
    void shouldPrintSourceNotFoundError() {
        outputFormatter.printSourceNotFoundError("/path/to/file.java");
        String output = getCombinedOutput();
        
        assertTrue(output.contains("Source path does not exist"));
        assertTrue(output.contains("/path/to/file.java"));
    }

    @Test
    @DisplayName("Should print coverage data not found error")
    void shouldPrintCoverageDataNotFoundError() {
        outputFormatter.printCoverageDataNotFoundError();
        String output = getCombinedOutput();
        
        assertTrue(output.contains("No coverage data found"));
        assertTrue(output.contains("Run tests with JaCoCo first"));
    }

    @Test
    @DisplayName("Should print coverage target met message")
    void shouldPrintCoverageTargetMetMessage() {
        outputFormatter.printCoverageTargetStatus(0.8, true);
        String output = outputStream.toString();
        
        assertTrue(output.contains("Coverage meets target: 80%"));
    }

    @Test
    @DisplayName("Should print coverage target not met message")
    void shouldPrintCoverageTargetNotMetMessage() {
        outputFormatter.printCoverageTargetStatus(0.8, false);
        String output = outputStream.toString();
        
        assertTrue(output.contains("Coverage below target: 80%"));
    }

    @Test
    @DisplayName("Should print result success message")
    void shouldPrintResultSuccessMessage() {
        OptimizationResult result = mock(OptimizationResult.class);
        when(result.getSummary()).thenReturn("Test summary");
        when(result.isSuccess()).thenReturn(true);
        
        outputFormatter.printResult(result);
        String output = outputStream.toString();
        
        assertTrue(output.contains("Test summary"));
        assertTrue(output.contains("Target coverage achieved"));
    }

    @Test
    @DisplayName("Should print result failure message")
    void shouldPrintResultFailureMessage() {
        OptimizationResult result = mock(OptimizationResult.class);
        when(result.getSummary()).thenReturn("Test summary");
        when(result.isSuccess()).thenReturn(false);
        
        outputFormatter.printResult(result);
        String output = outputStream.toString();
        
        assertTrue(output.contains("Test summary"));
        assertTrue(output.contains("Target coverage not reached"));
        assertTrue(output.contains("Increasing max iterations"));
    }

    @Test
    @DisplayName("Should print summary correctly")
    void shouldPrintSummaryCorrectly() {
        OptimizationResult result1 = mock(OptimizationResult.class);
        when(result1.isSuccess()).thenReturn(true);
        when(result1.getCoverageImprovement()).thenReturn(0.1);
        
        OptimizationResult result2 = mock(OptimizationResult.class);
        when(result2.isSuccess()).thenReturn(false);
        when(result2.getCoverageImprovement()).thenReturn(0.05);
        
        outputFormatter.printSummary(List.of(result1, result2));
        String output = outputStream.toString();
        
        assertTrue(output.contains("Summary"));
        assertTrue(output.contains("Processed 2 files"));
        assertTrue(output.contains("Target achieved: 1/2"));
        assertTrue(output.contains("Average coverage improvement"));
    }

    @Test
    @DisplayName("Should print progress message when verbose")
    void shouldPrintProgressMessageWhenVerbose() {
        when(outputConfig.getVerboseOrDefault()).thenReturn(true);
        OutputFormatter verboseFormatter = new OutputFormatter(outputConfig);
        
        verboseFormatter.printProgress("Test message");
        String output = outputStream.toString();
        
        assertTrue(output.contains("[INFO] Test message"));
    }

    @Test
    @DisplayName("Should not print progress message when not verbose")
    void shouldNotPrintProgressMessageWhenNotVerbose() {
        outputFormatter.printProgress("Test message");
        String output = outputStream.toString();
        
        assertFalse(output.contains("[INFO] Test message"));
    }

    @Test
    @DisplayName("Should print coverage update message")
    void shouldPrintCoverageUpdateMessage() {
        CoverageReport report = mock(CoverageReport.class);
        when(report.overallLineCoverage()).thenReturn(0.85);
        when(report.overallBranchCoverage()).thenReturn(0.75);
        
        outputFormatter.printCoverage(report);
        String output = outputStream.toString();
        
        assertTrue(output.contains("Coverage update"));
        assertTrue(output.contains("Line: 85.0%"));
        assertTrue(output.contains("Branch: 75.0%"));
    }

    @Test
    @DisplayName("Should handle null inputs gracefully")
    void shouldHandleNullInputsGracefully() {
        assertDoesNotThrow(() -> outputFormatter.printProviderInfo(null, null));
        assertDoesNotThrow(() -> outputFormatter.printFrameworks(null, null));
        assertDoesNotThrow(() -> outputFormatter.printConfigInitSuccess(null));
        assertDoesNotThrow(() -> outputFormatter.printConfigInitFailure(null));
        assertDoesNotThrow(() -> outputFormatter.printSourceNotFoundError(null));
    }
}
