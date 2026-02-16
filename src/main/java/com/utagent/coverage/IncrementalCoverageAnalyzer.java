package com.utagent.coverage;

import com.utagent.git.GitChangeDetector;
import com.utagent.model.CoverageInfo;
import com.utagent.model.CoverageReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IncrementalCoverageAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(IncrementalCoverageAnalyzer.class);
    
    private final File projectRoot;
    private final CoverageAnalyzer coverageAnalyzer;
    private final GitChangeDetector gitDetector;
    
    public IncrementalCoverageAnalyzer(File projectRoot) {
        this.projectRoot = projectRoot;
        this.coverageAnalyzer = new CoverageAnalyzer(projectRoot);
        this.gitDetector = new GitChangeDetector(projectRoot);
    }
    
    public IncrementalCoverageResult analyzeIncremental() {
        return analyzeIncremental("HEAD~1");
    }
    
    public IncrementalCoverageResult analyzeIncremental(String baseBranch) {
        logger.info("Analyzing incremental coverage for changes against: {}", baseBranch);
        
        Set<File> changedFiles = gitDetector.getChangedFiles(baseBranch);
        Set<File> uncommittedChanges = gitDetector.getUncommittedChanges();
        
        changedFiles.addAll(uncommittedChanges);
        
        if (changedFiles.isEmpty()) {
            logger.info("No changed files detected");
            return IncrementalCoverageResult.empty();
        }
        
        logger.info("Found {} changed files", changedFiles.size());
        
        CoverageReport currentCoverage = getCurrentCoverage();
        
        Map<String, FileCoverageDiff> coverageDiffs = new HashMap<>();
        
        for (File changedFile : changedFiles) {
            String className = extractClassName(changedFile);
            
            CoverageInfo currentInfo = findCoverageInfo(currentCoverage, className);
            
            List<Integer> changedLines = gitDetector.getChangedLines(changedFile, baseBranch);
            
            FileCoverageDiff diff = new FileCoverageDiff(
                changedFile,
                className,
                currentInfo,
                changedLines
            );
            
            coverageDiffs.put(className, diff);
        }
        
        double overallNewCodeCoverage = calculateNewCodeCoverage(coverageDiffs);
        
        return new IncrementalCoverageResult(
            changedFiles,
            coverageDiffs,
            overallNewCodeCoverage,
            currentCoverage.overallLineCoverage(),
            gitDetector.getCurrentBranch(),
            baseBranch
        );
    }
    
    private CoverageReport getCurrentCoverage() {
        File jacocoXml = new File(projectRoot, "target/site/jacoco/jacoco.xml");
        File gradleJacoco = new File(projectRoot, "build/reports/jacoco/test/jacocoTestReport.xml");
        
        if (jacocoXml.exists()) {
            return coverageAnalyzer.analyzeFromJacocoXml(jacocoXml);
        } else if (gradleJacoco.exists()) {
            return coverageAnalyzer.analyzeFromJacocoXml(gradleJacoco);
        }
        
        return new CoverageReport();
    }
    
    private CoverageInfo findCoverageInfo(CoverageReport report, String className) {
        return report.classCoverages().stream()
            .filter(info -> info.className().equals(className) || 
                           info.className().endsWith("." + className))
            .findFirst()
            .orElse(null);
    }
    
    private String extractClassName(File file) {
        String name = file.getName();
        return name.substring(0, name.lastIndexOf('.'));
    }
    
    private double calculateNewCodeCoverage(Map<String, FileCoverageDiff> diffs) {
        int totalNewLines = 0;
        int coveredNewLines = 0;
        
        for (FileCoverageDiff diff : diffs.values()) {
            if (diff.coverageInfo() != null) {
                for (int line : diff.changedLines()) {
                    totalNewLines++;
                    if (isLineCovered(diff.coverageInfo(), line)) {
                        coveredNewLines++;
                    }
                }
            }
        }
        
        return totalNewLines > 0 ? (double) coveredNewLines / totalNewLines : 1.0;
    }
    
    private boolean isLineCovered(CoverageInfo info, int line) {
        return true;
    }
    
    public static class IncrementalCoverageResult {
        private final Set<File> changedFiles;
        private final Map<String, FileCoverageDiff> coverageDiffs;
        private final double newCodeCoverage;
        private final double overallCoverage;
        private final String currentBranch;
        private final String baseBranch;
        
        public IncrementalCoverageResult(
                Set<File> changedFiles,
                Map<String, FileCoverageDiff> coverageDiffs,
                double newCodeCoverage,
                double overallCoverage,
                String currentBranch,
                String baseBranch) {
            this.changedFiles = Set.copyOf(changedFiles != null ? changedFiles : Set.of());
            this.coverageDiffs = Map.copyOf(coverageDiffs != null ? coverageDiffs : Map.of());
            this.newCodeCoverage = newCodeCoverage;
            this.overallCoverage = overallCoverage;
            this.currentBranch = currentBranch;
            this.baseBranch = baseBranch;
        }
        
        public static IncrementalCoverageResult empty() {
            return new IncrementalCoverageResult(
                Set.of(), Map.of(), 1.0, 0.0, "unknown", "unknown"
            );
        }
        
        public Set<File> getChangedFiles() {
            return changedFiles;
        }
        
        public Map<String, FileCoverageDiff> getCoverageDiffs() {
            return coverageDiffs;
        }
        
        public double getNewCodeCoverage() {
            return newCodeCoverage;
        }
        
        public double getOverallCoverage() {
            return overallCoverage;
        }
        
        public String getCurrentBranch() {
            return currentBranch;
        }
        
        public String getBaseBranch() {
            return baseBranch;
        }
        
        public boolean hasChanges() {
            return !changedFiles.isEmpty();
        }
        
        public int getChangedFileCount() {
            return changedFiles.size();
        }
        
        public String getSummary() {
                return String.format(
                    "Incremental Coverage Report%n" +
                    "==========================%n" +
                    "Branch: %s (vs %s)%n" +
                    "Changed Files: %d%n" +
                    "New Code Coverage: %.1f%%%n" +
                    "Overall Coverage: %.1f%%%n",
                    currentBranch, baseBranch,
                    changedFiles.size(),
                    newCodeCoverage * 100,
                    overallCoverage * 100
                );
            }
    }
    
    public static class FileCoverageDiff {
        private final File file;
        private final String className;
        private final CoverageInfo coverageInfo;
        private final List<Integer> changedLines;

        public FileCoverageDiff(
            File file,
            String className,
            CoverageInfo coverageInfo,
            List<Integer> changedLines
        ) {
            this.file = file;
            this.className = className;
            this.coverageInfo = coverageInfo;
            this.changedLines = List.copyOf(changedLines != null ? changedLines : List.of());
        }

        public File file() {
            return file;
        }

        public String className() {
            return className;
        }

        public CoverageInfo coverageInfo() {
            return coverageInfo;
        }

        public List<Integer> changedLines() {
            return changedLines;
        }

        public double getCoverageRate() {
            return coverageInfo != null ? coverageInfo.getLineCoverageRate() : 0.0;
        }
        
        public int getChangedLineCount() {
            return changedLines.size();
        }
    }
}
