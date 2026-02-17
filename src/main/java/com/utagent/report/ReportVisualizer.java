package com.utagent.report;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class ReportVisualizer {

    private static final Logger logger = LoggerFactory.getLogger(ReportVisualizer.class);
    private final ObjectMapper objectMapper;

    public ReportVisualizer() {
        this.objectMapper = new ObjectMapper();
    }

    public String generateCoverageChart(CoverageData data) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"coverage-chart\">\n");
        sb.append("  <h3>Coverage Report</h3>\n");
        sb.append("  <table>\n");
        sb.append("    <tr><th>Type</th><th>Coverage</th><th>Bar</th></tr>\n");
        
        for (Map.Entry<String, Double> entry : data.getMetrics().entrySet()) {
            String type = entry.getKey();
            double value = entry.getValue();
            String color = value >= 0.8 ? "green" : value >= 0.6 ? "yellow" : "red";
            
            sb.append(String.format("    <tr><td>%s</td><td>%.0f%%</td>", type, value * 100));
            sb.append(String.format("<td><div style=\"background:%s;width:%.0f%%\">&nbsp;</div></td></tr>\n", 
                color, value * 100));
        }
        
        sb.append("  </table>\n");
        sb.append(String.format("  <p><strong>Overall:</strong> %s</p>\n", data.getOverallPercent()));
        sb.append("</div>\n");
        
        return sb.toString();
    }

    public String generateTrendChart(List<CoverageData> trend) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"trend-chart\">\n");
        sb.append("  <h3>Coverage Trend</h3>\n");
        sb.append("  <svg width=\"400\" height=\"200\">\n");
        
        int x = 50;
        int step = 100;
        for (int i = 0; i < trend.size(); i++) {
            CoverageData data = trend.get(i);
            int y = (int) (180 - data.getOverall() * 150);
            
            sb.append(String.format("    <circle cx=\"%d\" cy=\"%d\" r=\"5\" fill=\"blue\"/>\n", x, y));
            
            if (i > 0) {
                int prevY = (int) (180 - trend.get(i - 1).getOverall() * 150);
                sb.append(String.format("    <line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"blue\"/>\n", 
                    x - step, prevY, x, y));
            }
            
            sb.append(String.format("    <text x=\"%d\" y=\"195\" font-size=\"10\">%d</text>\n", x - 10, i + 1));
            x += step;
        }
        
        sb.append("  </svg>\n");
        sb.append("</div>\n");
        
        return sb.toString();
    }

    public String generateCoverageHeatmap(Map<String, Double> classCoverage) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"coverage-heatmap\">\n");
        sb.append("  <h3>Coverage Heatmap</h3>\n");
        sb.append("  <table>\n");
        sb.append("    <tr><th>Class</th><th>Coverage</th><th>Status</th></tr>\n");
        
        for (Map.Entry<String, Double> entry : classCoverage.entrySet()) {
            String className = entry.getKey();
            double coverage = entry.getValue();
            String color = coverage >= 0.8 ? "green" : coverage >= 0.6 ? "yellow" : "red";
            String emoji = coverage >= 0.8 ? "🟢" : coverage >= 0.6 ? "🟡" : "🔴";
            
            sb.append(String.format("    <tr style=\"background:%s\"><td>%s</td><td>%.0f%%</td><td>%s</td></tr>\n", 
                color, className, coverage * 100, emoji));
        }
        
        sb.append("  </table>\n");
        sb.append("</div>\n");
        
        return sb.toString();
    }

    public String generateQualityRadarChart(QualityMetrics metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"quality-radar\">\n");
        sb.append("  <h3>Quality Metrics</h3>\n");
        sb.append("  <table>\n");
        sb.append("    <tr><th>Metric</th><th>Score</th></tr>\n");
        sb.append(String.format("    <tr><td>Coverage</td><td>%.0f%%</td></tr>\n", metrics.getCoverage() * 100));
        sb.append(String.format("    <tr><td>Mutation</td><td>%.0f%%</td></tr>\n", metrics.getMutation() * 100));
        sb.append(String.format("    <tr><td>Maintainability</td><td>%.0f%%</td></tr>\n", metrics.getMaintainability() * 100));
        sb.append(String.format("    <tr><td>Readability</td><td>%.0f%%</td></tr>\n", metrics.getReadability() * 100));
        sb.append(String.format("    <tr><td>Performance</td><td>%.0f%%</td></tr>\n", metrics.getPerformance() * 100));
        sb.append(String.format("    <tr><td><strong>Overall</strong></td><td><strong>%.0f%%</strong></td></tr>\n", 
            metrics.getOverall() * 100));
        sb.append("  </table>\n");
        sb.append("</div>\n");
        
        return sb.toString();
    }

    public String generateQualityGauge(double score) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"quality-gauge\">\n");
        sb.append("  <h3>Quality Score</h3>\n");
        
        String color = score >= 80 ? "green" : score >= 60 ? "yellow" : "red";
        int angle = (int) (score * 1.8);
        
        sb.append("  <svg width=\"200\" height=\"120\">\n");
        sb.append("    <path d=\"M 20 100 A 80 80 0 0 1 180 100\" fill=\"none\" stroke=\"lightgray\" stroke-width=\"10\"/>\n");
        sb.append(String.format("    <path d=\"M 20 100 A 80 80 0 0 1 %d %d\" fill=\"none\" stroke=\"%s\" stroke-width=\"10\"/>\n", 
            100 + (int)(80 * Math.cos(Math.toRadians(180 - angle))), 
            100 - (int)(80 * Math.sin(Math.toRadians(180 - angle))), 
            color));
        sb.append(String.format("    <text x=\"100\" y=\"90\" text-anchor=\"middle\" font-size=\"24\">%.0f</text>\n", score));
        sb.append("  </svg>\n");
        sb.append("</div>\n");
        
        return sb.toString();
    }

    public String generateTestTypePieChart(Map<String, Integer> distribution) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"test-type-pie\">\n");
        sb.append("  <h3>Test Distribution</h3>\n");
        sb.append("  <table>\n");
        
        int total = distribution.values().stream().mapToInt(Integer::intValue).sum();
        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            String type = entry.getKey();
            int count = entry.getValue();
            double percent = (double) count / total * 100;
            
            sb.append(String.format("    <tr><td>%s</td><td>%d</td><td>%.1f%%</td></tr>\n", 
                type, count, percent));
        }
        
        sb.append("  </table>\n");
        sb.append("</div>\n");
        
        return sb.toString();
    }

    public String generateDurationBarChart(Map<String, Long> durations) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"duration-bar\">\n");
        sb.append("  <h3>Test Duration</h3>\n");
        
        long maxDuration = durations.values().stream().mapToLong(Long::longValue).max().orElse(1);
        
        sb.append("  <table>\n");
        for (Map.Entry<String, Long> entry : durations.entrySet()) {
            String test = entry.getKey();
            long duration = entry.getValue();
            int width = (int) ((double) duration / maxDuration * 100);
            String color = duration < 200 ? "green" : duration < 500 ? "yellow" : "red";
            
            sb.append(String.format("    <tr><td>%s</td><td>%dms</td>", test, duration));
            sb.append(String.format("<td><div style=\"background:%s;width:%d%%\">&nbsp;</div></td></tr>\n", 
                color, width));
        }
        sb.append("  </table>\n");
        sb.append("</div>\n");
        
        return sb.toString();
    }

    public String exportToHtml(TestReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html>\n<head>\n");
        sb.append("  <title>").append(report.getTitle()).append("</title>\n");
        sb.append("  <style>\n");
        sb.append("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
        sb.append("    table { border-collapse: collapse; width: 100%; }\n");
        sb.append("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        sb.append("    .green { background-color: #90EE90; }\n");
        sb.append("    .yellow { background-color: #FFFFE0; }\n");
        sb.append("    .red { background-color: #FFB6C1; }\n");
        sb.append("  </style>\n");
        sb.append("</head>\n<body>\n");
        sb.append("  <h1>").append(report.getTitle()).append("</h1>\n");
        sb.append("  <p>Generated at: ").append(report.getGeneratedAt()).append("</p>\n");
        
        if (report.getCoverage() != null) {
            sb.append(generateCoverageChart(report.getCoverage()));
        }
        
        if (report.getQuality() != null) {
            sb.append(generateQualityRadarChart(report.getQuality()));
        }
        
        sb.append("</body>\n</html>");
        
        return sb.toString();
    }

    public String exportToMarkdown(TestReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(report.getTitle()).append("\n\n");
        sb.append("Generated at: ").append(report.getGeneratedAt()).append("\n\n");
        
        if (report.getCoverage() != null) {
            sb.append("## Coverage\n\n");
            sb.append("| Type | Coverage |\n");
            sb.append("|------|----------|\n");
            for (Map.Entry<String, Double> entry : report.getCoverage().getMetrics().entrySet()) {
                sb.append(String.format("| %s | %.0f%% |\n", entry.getKey(), entry.getValue() * 100));
            }
            sb.append(String.format("\n**Overall:** %s\n\n", report.getCoverage().getOverallPercent()));
        }
        
        return sb.toString();
    }

    public String exportToJson(TestReport report) {
        try {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("title", report.getTitle());
            map.put("generatedAt", report.getGeneratedAt());
            
            if (report.getCoverage() != null) {
                Map<String, Object> coverageMap = new java.util.HashMap<>();
                coverageMap.put("metrics", report.getCoverage().getMetrics());
                coverageMap.put("overall", report.getCoverage().getOverall());
                map.put("coverage", coverageMap);
            }
            
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(map);
        } catch (JsonProcessingException e) {
            logger.error("Failed to export report to JSON", e);
            return "{}";
        }
    }

    public String generateDashboard(DashboardConfig config) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html>\n<head>\n");
        sb.append("  <title>Test Dashboard</title>\n");
        sb.append("  <style>\n");
        sb.append("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
        sb.append("    .dashboard { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }\n");
        sb.append("    .card { border: 1px solid #ddd; padding: 15px; border-radius: 5px; }\n");
        sb.append("    table { border-collapse: collapse; width: 100%; }\n");
        sb.append("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        sb.append("  </style>\n");
        sb.append("</head>\n<body>\n");
        sb.append("  <h1>Test Dashboard</h1>\n");
        sb.append("  <div class=\"dashboard\">\n");
        
        if (config.isIncludeCoverage()) {
            sb.append("    <div class=\"card\">\n");
            sb.append("      <h2>Coverage</h2>\n");
            sb.append("      <p>Coverage metrics will be displayed here</p>\n");
            sb.append("    </div>\n");
        }
        
        if (config.isIncludeQuality()) {
            sb.append("    <div class=\"card\">\n");
            sb.append("      <h2>Quality</h2>\n");
            sb.append("      <p>Quality metrics will be displayed here</p>\n");
            sb.append("    </div>\n");
        }
        
        if (config.isIncludeTrends()) {
            sb.append("    <div class=\"card\">\n");
            sb.append("      <h2>Trends</h2>\n");
            sb.append("      <p>Trend charts will be displayed here</p>\n");
            sb.append("    </div>\n");
        }
        
        for (String section : config.getCustomSections()) {
            sb.append("    <div class=\"card\">\n");
            sb.append("      <h2>").append(section).append("</h2>\n");
            sb.append("      <p>").append(section).append(" data will be displayed here</p>\n");
            sb.append("    </div>\n");
        }
        
        sb.append("  </div>\n");
        sb.append("</body>\n</html>");
        
        return sb.toString();
    }
}
