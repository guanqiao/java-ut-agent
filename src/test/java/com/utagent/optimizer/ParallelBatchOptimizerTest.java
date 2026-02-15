package com.utagent.optimizer;

import com.utagent.config.ParallelConfig;
import com.utagent.model.CoverageReport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ParallelBatchOptimizer Tests")
class ParallelBatchOptimizerTest {

    @TempDir
    Path tempDir;

    private TestOptimizer mockDelegate;
    private ParallelBatchOptimizer optimizer;

    @BeforeEach
    void setUp() {
        mockDelegate = mock(TestOptimizer.class);
        when(mockDelegate.getTargetCoverage()).thenReturn(0.8);
        when(mockDelegate.getMaxIterations()).thenReturn(10);
        when(mockDelegate.getBuildToolName()).thenReturn("maven");
    }

    @AfterEach
    void tearDown() {
        if (optimizer != null) {
            optimizer.shutdown();
        }
    }

    @Test
    @DisplayName("Should delegate single file optimization to underlying optimizer")
    void shouldDelegateSingleFileOptimization() {
        // Given
        File sourceFile = new File(tempDir.toFile(), "Test.java");
        OptimizationResult expectedResult = new OptimizationResult();
        when(mockDelegate.optimize(sourceFile)).thenReturn(expectedResult);

        optimizer = new ParallelBatchOptimizer(mockDelegate);

        // When
        OptimizationResult result = optimizer.optimize(sourceFile);

        // Then
        assertEquals(expectedResult, result);
        verify(mockDelegate).optimize(sourceFile);
    }

    @Test
    @DisplayName("Should delegate configuration to underlying optimizer")
    void shouldDelegateConfiguration() {
        // Given
        optimizer = new ParallelBatchOptimizer(mockDelegate);

        // When
        optimizer.setTargetCoverage(0.9)
            .setMaxIterations(5)
            .setVerbose(true);

        // Then
        verify(mockDelegate).setTargetCoverage(0.9);
        verify(mockDelegate).setMaxIterations(5);
        verify(mockDelegate).setVerbose(true);
    }

    @Test
    @DisplayName("Should return delegate values for getters")
    void shouldReturnDelegateValuesForGetters() {
        // Given
        optimizer = new ParallelBatchOptimizer(mockDelegate);

        // When & Then
        assertEquals(0.8, optimizer.getTargetCoverage());
        assertEquals(10, optimizer.getMaxIterations());
        assertEquals("maven", optimizer.getBuildToolName());
    }

    @Test
    @DisplayName("Should process files in parallel when enabled")
    void shouldProcessFilesInParallelWhenEnabled() throws IOException {
        // Given
        ParallelConfig config = ParallelConfig.builder()
            .enabled(true)
            .threadPoolSize(2)
            .build();

        optimizer = new ParallelBatchOptimizer(mockDelegate, config);

        File sourceDir = tempDir.toFile();
        createJavaFile(sourceDir, "Class1.java");
        createJavaFile(sourceDir, "Class2.java");
        createJavaFile(sourceDir, "Class3.java");

        when(mockDelegate.optimize(any(File.class))).thenAnswer(invocation -> {
            Thread.sleep(100); // Simulate work
            OptimizationResult result = new OptimizationResult();
            result.setSourceFile(invocation.getArgument(0));
            result.setSuccess(true);
            return result;
        });

        // When
        List<OptimizationResult> results = optimizer.optimizeDirectory(sourceDir);

        // Then
        assertEquals(3, results.size());
        verify(mockDelegate, times(3)).optimize(any(File.class));
    }

    @Test
    @DisplayName("Should use sequential processing when parallel is disabled")
    void shouldUseSequentialProcessingWhenParallelDisabled() throws IOException {
        // Given
        ParallelConfig config = ParallelConfig.builder()
            .enabled(false)
            .build();

        optimizer = new ParallelBatchOptimizer(mockDelegate, config);

        File sourceDir = tempDir.toFile();
        createJavaFile(sourceDir, "Class1.java");
        createJavaFile(sourceDir, "Class2.java");

        // When
        optimizer.optimizeDirectory(sourceDir);

        // Then
        verify(mockDelegate).optimizeDirectory(sourceDir);
        verify(mockDelegate, never()).optimize(any(File.class));
    }

    @Test
    @DisplayName("Should handle errors gracefully and continue processing")
    void shouldHandleErrorsGracefully() throws IOException {
        // Given
        optimizer = new ParallelBatchOptimizer(mockDelegate);

        File sourceDir = tempDir.toFile();
        File file1 = createJavaFile(sourceDir, "Class1.java");
        File file2 = createJavaFile(sourceDir, "Class2.java");

        when(mockDelegate.optimize(file1)).thenThrow(new RuntimeException("Error"));
        when(mockDelegate.optimize(file2)).thenAnswer(invocation -> {
            OptimizationResult result = new OptimizationResult();
            result.setSourceFile(file2);
            result.setSuccess(true);
            return result;
        });

        // When
        List<OptimizationResult> results = optimizer.optimizeDirectory(sourceDir);

        // Then
        assertEquals(2, results.size());
        assertFalse(results.get(0).isSuccess());
        assertEquals("Error", results.get(0).getErrorMessage());
        assertTrue(results.get(1).isSuccess());
    }

    @Test
    @DisplayName("Should return empty list when directory has no Java files")
    void shouldReturnEmptyListWhenNoJavaFiles() {
        // Given
        optimizer = new ParallelBatchOptimizer(mockDelegate);
        File emptyDir = tempDir.toFile();

        // When
        List<OptimizationResult> results = optimizer.optimizeDirectory(emptyDir);

        // Then
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Should notify progress listener during parallel processing")
    void shouldNotifyProgressListener() throws IOException {
        // Given
        optimizer = new ParallelBatchOptimizer(mockDelegate);

        File sourceDir = tempDir.toFile();
        createJavaFile(sourceDir, "Class1.java");
        createJavaFile(sourceDir, "Class2.java");

        when(mockDelegate.optimize(any(File.class))).thenReturn(new OptimizationResult());

        List<String> progressMessages = new CopyOnWriteArrayList<>();
        optimizer.setProgressListener(progressMessages::add);

        // When
        optimizer.optimizeDirectory(sourceDir);

        // Then
        assertTrue(progressMessages.size() >= 2);
        assertTrue(progressMessages.get(0).contains("Starting parallel optimization"));
    }

    @Test
    @DisplayName("Should shutdown executor service gracefully")
    void shouldShutdownGracefully() {
        // Given
        optimizer = new ParallelBatchOptimizer(mockDelegate);

        // When
        optimizer.shutdown();

        // Then - should not throw exception
        assertDoesNotThrow(() -> optimizer.shutdown());
    }

    @Test
    @DisplayName("Should use custom thread pool configuration")
    void shouldUseCustomThreadPoolConfiguration() {
        // Given
        ParallelConfig config = ParallelConfig.builder()
            .threadPoolSize(4)
            .queueCapacity(50)
            .timeoutSeconds(120L)
            .build();

        // When
        optimizer = new ParallelBatchOptimizer(mockDelegate, config);

        // Then - should create optimizer without error
        assertNotNull(optimizer);
    }

    @Test
    @DisplayName("Should exclude test files from processing")
    void shouldExcludeTestFiles() throws IOException {
        // Given
        optimizer = new ParallelBatchOptimizer(mockDelegate);

        File sourceDir = tempDir.toFile();
        createJavaFile(sourceDir, "Class1.java");
        createJavaFile(sourceDir, "Class1Test.java");

        when(mockDelegate.optimize(any(File.class))).thenReturn(new OptimizationResult());

        // When
        List<OptimizationResult> results = optimizer.optimizeDirectory(sourceDir);

        // Then
        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Should process files in subdirectories")
    void shouldProcessFilesInSubdirectories() throws IOException {
        // Given
        optimizer = new ParallelBatchOptimizer(mockDelegate);

        File sourceDir = tempDir.toFile();
        File subDir = new File(sourceDir, "subpackage");
        subDir.mkdirs();
        createJavaFile(sourceDir, "Class1.java");
        createJavaFile(subDir, "Class2.java");

        when(mockDelegate.optimize(any(File.class))).thenReturn(new OptimizationResult());

        // When
        List<OptimizationResult> results = optimizer.optimizeDirectory(sourceDir);

        // Then
        assertEquals(2, results.size());
    }

    private File createJavaFile(File directory, String fileName) throws IOException {
        File file = new File(directory, fileName);
        String content = """
            package com.example;
            public class %s {
                public void method() {}
            }
            """.formatted(fileName.replace(".java", ""));
        Files.writeString(file.toPath(), content);
        return file;
    }
}
