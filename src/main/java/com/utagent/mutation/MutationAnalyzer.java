package com.utagent.mutation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MutationAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(MutationAnalyzer.class);
    private static final List<String> DEFAULT_MUTATORS = List.of("DEFAULTS");

    private final File projectRoot;
    private final List<String> mutators;

    public MutationAnalyzer(File projectRoot) {
        this(projectRoot, DEFAULT_MUTATORS);
    }

    public MutationAnalyzer(File projectRoot, List<String> mutators) {
        this.projectRoot = projectRoot;
        this.mutators = List.copyOf(mutators != null ? mutators : DEFAULT_MUTATORS);
    }

    public File getProjectRoot() {
        return projectRoot;
    }

    public List<String> getMutators() {
        return mutators;
    }

    public MutationReport analyze() {
        logger.info("Starting mutation analysis for project: {}", projectRoot.getAbsolutePath());
        
        if (!projectRoot.exists() || !projectRoot.isDirectory()) {
            logger.warn("Project root does not exist or is not a directory");
            return MutationReport.builder()
                .totalMutants(0)
                .killedMutants(0)
                .survivedMutants(0)
                .build();
        }

        return MutationReport.builder()
            .totalMutants(0)
            .killedMutants(0)
            .survivedMutants(0)
            .build();
    }

    public MutationReport analyze(String targetClass) {
        logger.info("Analyzing mutations for class: {}", targetClass);
        
        return MutationReport.builder()
            .targetClass(targetClass)
            .totalMutants(0)
            .killedMutants(0)
            .survivedMutants(0)
            .build();
    }

    public MutationReport analyzeMethod(String targetClass, String methodName) {
        logger.info("Analyzing mutations for method: {} in class: {}", methodName, targetClass);
        
        return MutationReport.builder()
            .targetClass(targetClass)
            .totalMutants(0)
            .killedMutants(0)
            .survivedMutants(0)
            .build();
    }

    public File generateXmlReport(MutationReport report, File outputDir) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File xmlFile = new File(outputDir, "mutations.xml");
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<mutations>\n");
        xml.append("  <targetClass>").append(escapeXml(report.getTargetClass())).append("</targetClass>\n");
        xml.append("  <totalMutants>").append(report.getTotalMutants()).append("</totalMutants>\n");
        xml.append("  <killedMutants>").append(report.getKilledMutants()).append("</killedMutants>\n");
        xml.append("  <survivedMutants>").append(report.getSurvivedMutants()).append("</survivedMutants>\n");
        xml.append("  <mutationScore>").append(String.format("%.2f", report.getMutationScore() * 100)).append("</mutationScore>\n");
        xml.append("</mutations>\n");

        Files.writeString(xmlFile.toPath(), xml.toString(), StandardCharsets.UTF_8);
        logger.info("Generated XML report: {}", xmlFile.getAbsolutePath());
        
        return xmlFile;
    }

    public File generateHtmlReport(MutationReport report, File outputDir) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File htmlFile = new File(outputDir, "mutations.html");
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <title>Mutation Test Report</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("    .score { font-size: 24px; font-weight: bold; color: #2e7d32; }\n");
        html.append("    table { border-collapse: collapse; width: 100%; margin-top: 20px; }\n");
        html.append("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("    th { background-color: #4CAF50; color: white; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <h1>Mutation Test Report</h1>\n");
        html.append("  <h2>Target Class: ").append(escapeHtml(report.getTargetClass())).append("</h2>\n");
        html.append("  <p class=\"score\">Mutation Score: ").append(String.format("%.2f%%", report.getMutationScore() * 100)).append("</p>\n");
        html.append("  <table>\n");
        html.append("    <tr><th>Metric</th><th>Value</th></tr>\n");
        html.append("    <tr><td>Total Mutants</td><td>").append(report.getTotalMutants()).append("</td></tr>\n");
        html.append("    <tr><td>Killed</td><td>").append(report.getKilledMutants()).append("</td></tr>\n");
        html.append("    <tr><td>Survived</td><td>").append(report.getSurvivedMutants()).append("</td></tr>\n");
        html.append("    <tr><td>Timed Out</td><td>").append(report.getTimeoutMutants()).append("</td></tr>\n");
        html.append("    <tr><td>Memory Errors</td><td>").append(report.getMemoryErrorMutants()).append("</td></tr>\n");
        html.append("  </table>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        Files.writeString(htmlFile.toPath(), html.toString(), StandardCharsets.UTF_8);
        logger.info("Generated HTML report: {}", htmlFile.getAbsolutePath());
        
        return htmlFile;
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
