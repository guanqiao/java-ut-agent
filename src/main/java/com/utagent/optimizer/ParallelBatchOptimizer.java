package com.utagent.optimizer;

import com.utagent.config.ParallelConfig;
import com.utagent.model.CoverageReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Parallel batch optimizer that processes multiple files concurrently.
 */
public class ParallelBatchOptimizer implements TestOptimizer {

    private static final Logger logger = LoggerFactory.getLogger(ParallelBatchOptimizer.class);

    private final TestOptimizer delegate;
    private final ParallelConfig config;
    private final ExecutorService executorService;

    private Consumer<String> progressListener;
    private Consumer<CoverageReport> coverageListener;

    public ParallelBatchOptimizer(TestOptimizer delegate) {
        this(delegate, ParallelConfig.defaults());
    }

    public ParallelBatchOptimizer(TestOptimizer delegate, ParallelConfig config) {
        this.delegate = delegate;
        this.config = config;
        this.executorService = createExecutorService();
    }

    private ExecutorService createExecutorService() {
        int poolSize = config.getThreadPoolSizeOrDefault();
        int queueCapacity = config.getQueueCapacityOrDefault();

        logger.info("Creating thread pool with {} threads and queue capacity {}", poolSize, queueCapacity);

        return new ThreadPoolExecutor(
            poolSize,
            poolSize,
            60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueCapacity),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r, "optimizer-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Override
    public OptimizationResult optimize(File sourceFile) {
        return delegate.optimize(sourceFile);
    }

    @Override
    public OptimizationResult optimize(File sourceFile, File existingTestFile) {
        return delegate.optimize(sourceFile, existingTestFile);
    }

    @Override
    public List<OptimizationResult> optimizeDirectory(File sourceDirectory) {
        if (!config.getEnabledOrDefault()) {
            logger.info("Parallel processing disabled, using sequential optimization");
            return delegate.optimizeDirectory(sourceDirectory);
        }

        List<File> javaFiles = findJavaFiles(sourceDirectory);
        int totalFiles = javaFiles.size();

        if (totalFiles == 0) {
            return new ArrayList<>();
        }

        logger.info("Starting parallel optimization for {} files using {} threads", totalFiles, config.getThreadPoolSizeOrDefault());
        notifyProgress("Starting parallel optimization for " + totalFiles + " files");

        List<Future<OptimizationResult>> futures = new ArrayList<>();
        AtomicInteger completedCount = new AtomicInteger(0);

        for (File javaFile : javaFiles) {
            Future<OptimizationResult> future = executorService.submit(() -> {
                try {
                    OptimizationResult result = optimize(javaFile);
                    int completed = completedCount.incrementAndGet();
                    notifyProgress(String.format("Progress: %d/%d files processed (%s)",
                        completed, totalFiles, javaFile.getName()));
                    return result;
                } catch (Exception e) {
                    logger.error("Error optimizing file: {}", javaFile.getAbsolutePath(), e);
                    OptimizationResult errorResult = new OptimizationResult();
                    errorResult.setSourceFile(javaFile);
                    errorResult.setSuccess(false);
                    errorResult.setErrorMessage(e.getMessage());
                    return errorResult;
                }
            });
            futures.add(future);
        }

        List<OptimizationResult> results = new ArrayList<>();
        long timeoutSeconds = config.getTimeoutSecondsOrDefault();

        for (Future<OptimizationResult> future : futures) {
            try {
                OptimizationResult result = future.get(timeoutSeconds, TimeUnit.SECONDS);
                results.add(result);
                if (result.getFinalCoverage() != null) {
                    notifyCoverage(result.getFinalCoverage());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Optimization interrupted", e);
                break;
            } catch (ExecutionException e) {
                logger.error("Optimization task failed", e.getCause());
            } catch (TimeoutException e) {
                logger.error("Optimization task timed out after {} seconds", timeoutSeconds);
                future.cancel(true);
            }
        }

        logger.info("Parallel optimization completed. Success: {}/{}",
            results.stream().filter(OptimizationResult::isSuccess).count(), totalFiles);

        return results;
    }

    @Override
    public TestOptimizer setTargetCoverage(double targetCoverage) {
        delegate.setTargetCoverage(targetCoverage);
        return this;
    }

    @Override
    public TestOptimizer setMaxIterations(int maxIterations) {
        delegate.setMaxIterations(maxIterations);
        return this;
    }

    @Override
    public TestOptimizer setVerbose(boolean verbose) {
        delegate.setVerbose(verbose);
        return this;
    }

    @Override
    public TestOptimizer setProgressListener(Consumer<String> progressListener) {
        this.progressListener = progressListener;
        delegate.setProgressListener(progressListener);
        return this;
    }

    @Override
    public TestOptimizer setCoverageListener(Consumer<CoverageReport> coverageListener) {
        this.coverageListener = coverageListener;
        delegate.setCoverageListener(coverageListener);
        return this;
    }

    @Override
    public double getTargetCoverage() {
        return delegate.getTargetCoverage();
    }

    @Override
    public int getCurrentIteration() {
        return delegate.getCurrentIteration();
    }

    @Override
    public int getMaxIterations() {
        return delegate.getMaxIterations();
    }

    @Override
    public String getBuildToolName() {
        return delegate.getBuildToolName();
    }

    /**
     * Shutdown the executor service gracefully.
     */
    public void shutdown() {
        logger.info("Shutting down parallel optimizer");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private List<File> findJavaFiles(File directory) {
        List<File> javaFiles = new ArrayList<>();

        if (directory.isDirectory()) {
            File[] files = directory.listFiles((dir, name) ->
                name.endsWith(".java") && !name.endsWith("Test.java"));

            if (files != null) {
                for (File file : files) {
                    javaFiles.add(file);
                }
            }

            File[] subDirs = directory.listFiles(File::isDirectory);
            if (subDirs != null) {
                for (File subDir : subDirs) {
                    javaFiles.addAll(findJavaFiles(subDir));
                }
            }
        }

        return javaFiles;
    }

    private void notifyProgress(String message) {
        if (progressListener != null) {
            progressListener.accept(message);
        }
    }

    private void notifyCoverage(CoverageReport report) {
        if (coverageListener != null) {
            coverageListener.accept(report);
        }
    }
}
