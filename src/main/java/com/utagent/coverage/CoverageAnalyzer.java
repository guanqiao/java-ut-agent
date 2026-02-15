package com.utagent.coverage;

import com.utagent.model.CoverageInfo;
import com.utagent.model.CoverageReport;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.IMethodCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.data.SessionInfoStore;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportGroupVisitor;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CoverageAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(CoverageAnalyzer.class);

    private final File projectRoot;
    private final File classesDirectory;
    private final File sourceDirectory;
    private final File reportDirectory;

    public CoverageAnalyzer(File projectRoot) {
        this.projectRoot = projectRoot;
        this.classesDirectory = new File(projectRoot, "target/classes");
        this.sourceDirectory = new File(projectRoot, "src/main/java");
        this.reportDirectory = new File(projectRoot, "target/site/jacoco");
    }

    public CoverageAnalyzer(File projectRoot, String classesPath, String sourcePath) {
        this.projectRoot = projectRoot;
        this.classesDirectory = new File(projectRoot, classesPath);
        this.sourceDirectory = new File(projectRoot, sourcePath);
        this.reportDirectory = new File(projectRoot, "target/site/jacoco");
    }

    public CoverageReport analyzeCoverage(File execDataFile) {
        try {
            ExecutionDataStore executionData = new ExecutionDataStore();
            SessionInfoStore sessionInfo = new SessionInfoStore();
            
            if (execDataFile != null && execDataFile.exists()) {
                org.jacoco.core.data.ExecutionDataReader reader = 
                    new org.jacoco.core.data.ExecutionDataReader(
                        new FileInputStream(execDataFile));
                reader.setExecutionDataVisitor(executionData);
                reader.setSessionInfoVisitor(sessionInfo);
                reader.read();
            }

            CoverageBuilder coverageBuilder = new CoverageBuilder();
            Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
            
            if (classesDirectory.exists()) {
                analyzer.analyzeAll(classesDirectory);
            }

            List<CoverageInfo> classCoverages = new ArrayList<>();
            double totalLineCoverage = 0;
            double totalBranchCoverage = 0;
            double totalInstructionCoverage = 0;
            int classCount = 0;

            for (IClassCoverage classCoverage : coverageBuilder.getClasses()) {
                CoverageInfo info = new CoverageInfo(
                    classCoverage.getName().replace('/', '.'),
                    "",
                    0,
                    classCoverage.getBranchCounter().getTotalCount(),
                    classCoverage.getBranchCounter().getMissedCount(),
                    classCoverage.getInstructionCounter().getTotalCount(),
                    classCoverage.getInstructionCounter().getMissedCount(),
                    classCoverage.getLineCounter().getTotalCount(),
                    classCoverage.getLineCounter().getMissedCount()
                );
                
                classCoverages.add(info);
                totalLineCoverage += info.getLineCoverageRate();
                totalBranchCoverage += info.getBranchCoverageRate();
                totalInstructionCoverage += info.getInstructionCoverageRate();
                classCount++;

                for (IMethodCoverage methodCoverage : classCoverage.getMethods()) {
                    CoverageInfo methodInfo = new CoverageInfo(
                        classCoverage.getName().replace('/', '.'),
                        methodCoverage.getName(),
                        methodCoverage.getFirstLine(),
                        methodCoverage.getBranchCounter().getTotalCount(),
                        methodCoverage.getBranchCounter().getMissedCount(),
                        methodCoverage.getInstructionCounter().getTotalCount(),
                        methodCoverage.getInstructionCounter().getMissedCount(),
                        methodCoverage.getLineCounter().getTotalCount(),
                        methodCoverage.getLineCounter().getMissedCount()
                    );
                    classCoverages.add(methodInfo);
                }
            }

            double avgLineCoverage = classCount > 0 ? totalLineCoverage / classCount : 0;
            double avgBranchCoverage = classCount > 0 ? totalBranchCoverage / classCount : 0;
            double avgInstructionCoverage = classCount > 0 ? totalInstructionCoverage / classCount : 0;

            return new CoverageReport(
                avgLineCoverage,
                avgBranchCoverage,
                avgInstructionCoverage,
                classCoverages,
                new ArrayList<>()
            );
        } catch (IOException e) {
            logger.error("Error analyzing coverage", e);
            return new CoverageReport();
        }
    }

    public CoverageReport analyzeFromJacocoXml(File xmlReport) {
        if (!xmlReport.exists()) {
            logger.warn("JaCoCo XML report not found: {}", xmlReport.getAbsolutePath());
            return new CoverageReport();
        }

        try {
            JacocoXmlParser parser = new JacocoXmlParser();
            return parser.parse(xmlReport);
        } catch (Exception e) {
            logger.error("Error parsing JaCoCo XML report", e);
            return new CoverageReport();
        }
    }

    public void generateHtmlReport(File execDataFile) throws IOException {
        ExecutionDataStore executionData = new ExecutionDataStore();
        SessionInfoStore sessionInfo = new SessionInfoStore();
        
        if (execDataFile != null && execDataFile.exists()) {
            org.jacoco.core.data.ExecutionDataReader reader = 
                new org.jacoco.core.data.ExecutionDataReader(
                    new FileInputStream(execDataFile));
            reader.setExecutionDataVisitor(executionData);
            reader.setSessionInfoVisitor(sessionInfo);
            reader.read();
        }

        CoverageBuilder coverageBuilder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(executionData, coverageBuilder);
        
        if (classesDirectory.exists()) {
            analyzer.analyzeAll(classesDirectory);
        }

        HTMLFormatter htmlFormatter = new HTMLFormatter();
        IReportVisitor visitor = htmlFormatter.createVisitor(
            new FileMultiReportOutput(reportDirectory));
        
        visitor.visitInfo(sessionInfo.getInfos(), executionData.getContents());
        
        if (sourceDirectory.exists()) {
            visitor.visitBundle(
                coverageBuilder.getBundle("Coverage Report"),
                new DirectorySourceFileLocator(sourceDirectory, "UTF-8", 4));
        }
        
        visitor.visitEnd();
        
        logger.info("HTML coverage report generated at: {}", reportDirectory.getAbsolutePath());
    }

    public List<Integer> getUncoveredLines(CoverageReport report, String className) {
        List<Integer> uncoveredLines = new ArrayList<>();
        
        for (CoverageInfo info : report.classCoverages()) {
            if (info.className().equals(className)) {
                int missed = info.lineMissed();
                int total = info.lineCount();
                if (missed > 0) {
                    for (int i = 0; i < missed; i++) {
                        uncoveredLines.add(info.lineNumber() + i);
                    }
                }
            }
        }
        
        return uncoveredLines;
    }

    public List<CoverageInfo> getUncoveredMethods(CoverageReport report, String className) {
        List<CoverageInfo> uncoveredMethods = new ArrayList<>();
        
        for (CoverageInfo info : report.classCoverages()) {
            if (info.className().equals(className) && 
                !info.methodName().isEmpty() &&
                info.getLineCoverageRate() < 1.0) {
                uncoveredMethods.add(info);
            }
        }
        
        return uncoveredMethods;
    }

    public boolean meetsTarget(CoverageReport report, double targetRate) {
        return report.overallLineCoverage() >= targetRate &&
               report.overallBranchCoverage() >= targetRate;
    }

    public String formatCoverageReport(CoverageReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Coverage Report ===\n");
        sb.append(String.format("Line Coverage: %.2f%%\n", report.overallLineCoverage() * 100));
        sb.append(String.format("Branch Coverage: %.2f%%\n", report.overallBranchCoverage() * 100));
        sb.append(String.format("Instruction Coverage: %.2f%%\n", report.overallInstructionCoverage() * 100));
        sb.append("\nClass Details:\n");
        
        for (CoverageInfo info : report.classCoverages()) {
            if (info.methodName().isEmpty()) {
                sb.append(String.format("  %s: %.1f%% line, %.1f%% branch\n",
                    info.className(),
                    info.getLineCoverageRate() * 100,
                    info.getBranchCoverageRate() * 100));
            }
        }
        
        return sb.toString();
    }
}
