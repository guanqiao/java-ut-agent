package com.utagent.optimizer;

import com.utagent.model.CoverageReport;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

/**
 * Interface for test optimization strategies.
 * Implementations should provide ways to optimize test coverage
 * through iterative generation and analysis.
 */
public interface TestOptimizer {

    /**
     * Optimize tests for a single source file.
     *
     * @param sourceFile the source file to optimize tests for
     * @return the optimization result
     */
    OptimizationResult optimize(File sourceFile);

    /**
     * Optimize tests for a single source file with an existing test file.
     *
     * @param sourceFile the source file to optimize tests for
     * @param existingTestFile the existing test file to enhance
     * @return the optimization result
     */
    OptimizationResult optimize(File sourceFile, File existingTestFile);

    /**
     * Optimize tests for all Java files in a directory.
     *
     * @param sourceDirectory the directory containing source files
     * @return list of optimization results
     */
    List<OptimizationResult> optimizeDirectory(File sourceDirectory);

    /**
     * Set the target coverage rate.
     *
     * @param targetCoverage target coverage rate (0.0 - 1.0)
     * @return this optimizer for method chaining
     */
    TestOptimizer setTargetCoverage(double targetCoverage);

    /**
     * Set the maximum number of optimization iterations.
     *
     * @param maxIterations maximum iterations
     * @return this optimizer for method chaining
     */
    TestOptimizer setMaxIterations(int maxIterations);

    /**
     * Set verbose mode for detailed output.
     *
     * @param verbose true to enable verbose output
     * @return this optimizer for method chaining
     */
    TestOptimizer setVerbose(boolean verbose);

    /**
     * Set a listener for progress updates.
     *
     * @param progressListener listener that receives progress messages
     * @return this optimizer for method chaining
     */
    TestOptimizer setProgressListener(Consumer<String> progressListener);

    /**
     * Set a listener for coverage updates.
     *
     * @param coverageListener listener that receives coverage reports
     * @return this optimizer for method chaining
     */
    TestOptimizer setCoverageListener(Consumer<CoverageReport> coverageListener);

    /**
     * Get the current target coverage.
     *
     * @return target coverage rate
     */
    double getTargetCoverage();

    /**
     * Get the current iteration count.
     *
     * @return current iteration
     */
    int getCurrentIteration();

    /**
     * Get the maximum iterations.
     *
     * @return maximum iterations
     */
    int getMaxIterations();

    /**
     * Get the name of the build tool being used.
     *
     * @return build tool name
     */
    String getBuildToolName();

    /**
     * Set incremental mode for test generation.
     * When enabled, existing tests are preserved and new tests are added incrementally.
     *
     * @param incrementalMode true to enable incremental mode
     * @return this optimizer for method chaining
     */
    default TestOptimizer setIncrementalMode(boolean incrementalMode) {
        return this;
    }

    /**
     * Check if incremental mode is enabled.
     *
     * @return true if incremental mode is enabled
     */
    default boolean isIncrementalMode() {
        return true;
    }
}
