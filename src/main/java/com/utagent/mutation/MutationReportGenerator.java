package com.utagent.mutation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MutationReportGenerator {

    private static final Logger logger = LoggerFactory.getLogger(MutationReportGenerator.class);

    public String generateSummary(MutationReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("=" .repeat(50)).append("\n");
        sb.append("Mutation Test Summary\n");
        sb.append("=" .repeat(50)).append("\n");
        sb.append(String.format("Target Class: %s%n", report.getTargetClass()));
        sb.append(String.format("Mutation Score: %.2f%%%n", report.getMutationScore() * 100));
        sb.append(String.format("Total Mutants: %d%n", report.getTotalMutants()));
        sb.append(String.format("Killed: %d%n", report.getKilledMutants()));
        sb.append(String.format("Survived: %d%n", report.getSurvivedMutants()));
        sb.append(String.format("Timed Out: %d%n", report.getTimeoutMutants()));
        sb.append(String.format("Memory Errors: %d%n", report.getMemoryErrorMutants()));
        sb.append("=" .repeat(50)).append("\n");
        return sb.toString();
    }

    public String generateDetailed(MutationReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(generateSummary(report));
        sb.append("\nMutant Details:\n");
        sb.append("-" .repeat(50)).append("\n");

        int index = 1;
        for (Mutant mutant : report.getMutants()) {
            sb.append(String.format("Mutant #%d%n", index++));
            sb.append(String.format("  ID: %s%n", mutant.id()));
            sb.append(String.format("  Mutator: %s%n", mutant.mutator()));
            sb.append(String.format("  Method: %s%n", mutant.method()));
            sb.append(String.format("  Line: %d%n", mutant.lineNumber()));
            sb.append(String.format("  Status: %s%n", mutant.status()));
            sb.append(String.format("  Description: %s%n", mutant.description()));
            sb.append("\n");
        }

        return sb.toString();
    }

    public File generateHtml(MutationReport report, File outputDir) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File htmlFile = new File(outputDir, "mutation-report.html");
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>Mutation Test Report</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 20px; background-color: #f5f5f5; }\n");
        html.append("    .container { max-width: 800px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n");
        html.append("    .score { font-size: 48px; font-weight: bold; color: #4CAF50; text-align: center; }\n");
        html.append("    table { border-collapse: collapse; width: 100%; margin-top: 20px; }\n");
        html.append("    th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n");
        html.append("    th { background-color: #4CAF50; color: white; }\n");
        html.append("    tr:nth-child(even) { background-color: #f9f9f9; }\n");
        html.append("    .killed { color: #4CAF50; }\n");
        html.append("    .survived { color: #f44336; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <div class=\"container\">\n");
        html.append("    <h1>Mutation Test Report</h1>\n");
        html.append("    <p><strong>Target Class:</strong> ").append(escapeHtml(report.getTargetClass())).append("</p>\n");
        html.append("    <p class=\"score\">").append(String.format("%.2f%%", report.getMutationScore() * 100)).append("</p>\n");
        html.append("    <table>\n");
        html.append("      <tr><th>Metric</th><th>Value</th></tr>\n");
        html.append("      <tr><td>Total Mutants</td><td>").append(report.getTotalMutants()).append("</td></tr>\n");
        html.append("      <tr><td>Killed</td><td class=\"killed\">").append(report.getKilledMutants()).append("</td></tr>\n");
        html.append("      <tr><td>Survived</td><td class=\"survived\">").append(report.getSurvivedMutants()).append("</td></tr>\n");
        html.append("      <tr><td>Timed Out</td><td>").append(report.getTimeoutMutants()).append("</td></tr>\n");
        html.append("      <tr><td>Memory Errors</td><td>").append(report.getMemoryErrorMutants()).append("</td></tr>\n");
        html.append("    </table>\n");
        html.append("    <p><em>Generated at: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</em></p>\n");
        html.append("  </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        Files.writeString(htmlFile.toPath(), html.toString(), StandardCharsets.UTF_8);
        logger.info("Generated HTML report: {}", htmlFile.getAbsolutePath());
        
        return htmlFile;
    }

    public File generateJson(MutationReport report, File outputDir) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File jsonFile = new File(outputDir, "mutation-report.json");
        StringBuilder json = new StringBuilder();
        
        json.append("{\n");
        json.append(String.format("  \"targetClass\": \"%s\",\n", report.getTargetClass()));
        json.append(String.format("  \"mutationScore\": %.4f,\n", report.getMutationScore()));
        json.append(String.format("  \"totalMutants\": %d,\n", report.getTotalMutants()));
        json.append(String.format("  \"killedMutants\": %d,\n", report.getKilledMutants()));
        json.append(String.format("  \"survivedMutants\": %d,\n", report.getSurvivedMutants()));
        json.append(String.format("  \"timeoutMutants\": %d,\n", report.getTimeoutMutants()));
        json.append(String.format("  \"memoryErrorMutants\": %d,\n", report.getMemoryErrorMutants()));
        json.append(String.format("  \"timestamp\": \"%s\"%n", LocalDateTime.now().toString()));
        json.append("}\n");

        Files.writeString(jsonFile.toPath(), json.toString(), StandardCharsets.UTF_8);
        logger.info("Generated JSON report: {}", jsonFile.getAbsolutePath());
        
        return jsonFile;
    }

    public File generateCsv(MutationReport report, File outputDir) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File csvFile = new File(outputDir, "mutation-report.csv");
        StringBuilder csv = new StringBuilder();
        
        csv.append("ID,Mutator,Method,Line,Status,Description\n");
        
        for (Mutant mutant : report.getMutants()) {
            csv.append(String.format("%s,%s,%s,%d,%s,\"%s\"%n",
                escapeCsv(mutant.id()),
                escapeCsv(mutant.mutator()),
                escapeCsv(mutant.method()),
                mutant.lineNumber(),
                mutant.status(),
                escapeCsv(mutant.description())
            ));
        }

        Files.writeString(csvFile.toPath(), csv.toString(), StandardCharsets.UTF_8);
        logger.info("Generated CSV report: {}", csvFile.getAbsolutePath());
        
        return csvFile;
    }

    public File generateMarkdown(MutationReport report, File outputDir) throws IOException {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File mdFile = new File(outputDir, "mutation-report.md");
        StringBuilder md = new StringBuilder();
        
        md.append("# Mutation Test Report\n\n");
        md.append(String.format("**Target Class:** %s%n%n", report.getTargetClass()));
        md.append(String.format("**Mutation Score:** %.2f%%%n%n", report.getMutationScore() * 100));
        md.append("## Summary\n\n");
        md.append("| Metric | Value |\n");
        md.append("|--------|-------|\n");
        md.append(String.format("| Total Mutants | %d |\n", report.getTotalMutants()));
        md.append(String.format("| Killed | %d |\n", report.getKilledMutants()));
        md.append(String.format("| Survived | %d |\n", report.getSurvivedMutants()));
        md.append(String.format("| Timed Out | %d |\n", report.getTimeoutMutants()));
        md.append(String.format("| Memory Errors | %d |\n", report.getMemoryErrorMutants()));

        Files.writeString(mdFile.toPath(), md.toString(), StandardCharsets.UTF_8);
        logger.info("Generated Markdown report: {}", mdFile.getAbsolutePath());
        
        return mdFile;
    }

    public String generateComparison(MutationReport report1, MutationReport report2) {
        StringBuilder sb = new StringBuilder();
        sb.append("=" .repeat(50)).append("\n");
        sb.append("Mutation Score Comparison\n");
        sb.append("=" .repeat(50)).append("\n");
        sb.append(String.format("%s: %.2f%%%n", report1.getTargetClass(), report1.getMutationScore() * 100));
        sb.append(String.format("%s: %.2f%%%n", report2.getTargetClass(), report2.getMutationScore() * 100));
        sb.append(String.format("Difference: %.2f%%%n", 
            (report2.getMutationScore() - report1.getMutationScore()) * 100));
        sb.append("=" .repeat(50)).append("\n");
        return sb.toString();
    }

    public String generateTrend(List<MutationReport> reports) {
        StringBuilder sb = new StringBuilder();
        sb.append("=" .repeat(50)).append("\n");
        sb.append("Mutation Score Trend\n");
        sb.append("=" .repeat(50)).append("\n");
        
        int index = 1;
        for (MutationReport report : reports) {
            sb.append(String.format("Run %d: %.2f%%%n", index++, report.getMutationScore() * 100));
        }
        
        if (reports.size() >= 2) {
            double trend = reports.get(reports.size() - 1).getMutationScore() 
                - reports.get(0).getMutationScore();
            sb.append(String.format("%nOverall Trend: %+.2f%%%n", trend * 100));
        }
        
        sb.append("=" .repeat(50)).append("\n");
        return sb.toString();
    }

    public String generateQualityAssessment(MutationReport report) {
        MutationScoreCalculator calculator = new MutationScoreCalculator();
        MutationQualityLevel level = calculator.determineQualityLevel(report.getMutationScore());
        
        StringBuilder sb = new StringBuilder();
        sb.append("=" .repeat(50)).append("\n");
        sb.append("Quality Assessment\n");
        sb.append("=" .repeat(50)).append("\n");
        sb.append(String.format("Target Class: %s%n", report.getTargetClass()));
        sb.append(String.format("Mutation Score: %.2f%%%n", report.getMutationScore() * 100));
        sb.append(String.format("Quality Level: %s%n", level.label()));
        sb.append("=" .repeat(50)).append("\n");
        return sb.toString();
    }

    public List<String> generateRecommendations(MutationReport report) {
        List<String> recommendations = new ArrayList<>();
        
        for (Mutant mutant : report.getMutants()) {
            if (mutant.status() == MutantStatus.SURVIVED) {
                recommendations.add(String.format(
                    "Add test for method '%s' (line %d) to kill mutant: %s",
                    mutant.method(),
                    mutant.lineNumber(),
                    mutant.description()
                ));
            }
        }
        
        if (report.getMutationScore() < 0.8) {
            recommendations.add("Consider adding more comprehensive tests to improve mutation score");
        }
        
        return recommendations;
    }

    public String generateWithTemplate(MutationReport report, String template) {
        return template
            .replace("{{targetClass}}", report.getTargetClass() != null ? report.getTargetClass() : "")
            .replace("{{mutationScore}}", String.format("%.2f", report.getMutationScore() * 100))
            .replace("{{totalMutants}}", String.valueOf(report.getTotalMutants()))
            .replace("{{killedMutants}}", String.valueOf(report.getKilledMutants()))
            .replace("{{survivedMutants}}", String.valueOf(report.getSurvivedMutants()));
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

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
