package com.utagent.quality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestQualityScorer {

    private static final Logger logger = LoggerFactory.getLogger(TestQualityScorer.class);

    private static final double LINE_COVERAGE_WEIGHT = 0.25;
    private static final double BRANCH_COVERAGE_WEIGHT = 0.20;
    private static final double MUTATION_SCORE_WEIGHT = 0.30;
    private static final double READABILITY_WEIGHT = 0.15;
    private static final double MAINTAINABILITY_WEIGHT = 0.10;

    public double calculateOverallScore(TestQualityMetrics metrics) {
        double score = 0.0;

        if (metrics.lineCoverage() > 0) {
            score += metrics.lineCoverage() * LINE_COVERAGE_WEIGHT;
        }
        if (metrics.branchCoverage() > 0) {
            score += metrics.branchCoverage() * BRANCH_COVERAGE_WEIGHT;
        }
        if (metrics.mutationScore() > 0) {
            score += metrics.mutationScore() * MUTATION_SCORE_WEIGHT;
        }
        if (metrics.readabilityScore() > 0) {
            score += metrics.readabilityScore() * READABILITY_WEIGHT;
        }
        if (metrics.maintainabilityScore() > 0) {
            score += metrics.maintainabilityScore() * MAINTAINABILITY_WEIGHT;
        }

        double totalWeight = calculateTotalWeight(metrics);
        return totalWeight > 0 ? score / totalWeight : 0.0;
    }

    private double calculateTotalWeight(TestQualityMetrics metrics) {
        double weight = 0.0;
        if (metrics.lineCoverage() > 0) weight += LINE_COVERAGE_WEIGHT;
        if (metrics.branchCoverage() > 0) weight += BRANCH_COVERAGE_WEIGHT;
        if (metrics.mutationScore() > 0) weight += MUTATION_SCORE_WEIGHT;
        if (metrics.readabilityScore() > 0) weight += READABILITY_WEIGHT;
        if (metrics.maintainabilityScore() > 0) weight += MAINTAINABILITY_WEIGHT;
        return weight;
    }

    public QualityGrade determineGrade(double score) {
        if (score >= 0.90) return QualityGrade.A;
        if (score >= 0.80) return QualityGrade.B;
        if (score >= 0.65) return QualityGrade.C;
        if (score >= 0.50) return QualityGrade.D;
        return QualityGrade.F;
    }

    public double calculateReadability(String testCode) {
        if (testCode == null || testCode.isEmpty()) {
            return 0.0;
        }

        double score = 1.0;

        score -= calculateLengthPenalty(testCode);
        score += calculateNamingBonus(testCode);
        score -= calculateComplexityPenalty(testCode);
        score += calculateCommentBonus(testCode);

        return Math.max(0.0, Math.min(1.0, score));
    }

    private double calculateLengthPenalty(String code) {
        int lines = code.split("\n").length;
        if (lines > 50) {
            return 0.3;
        } else if (lines > 30) {
            return 0.15;
        } else if (lines > 20) {
            return 0.05;
        }
        return 0.0;
    }

    private double calculateNamingBonus(String code) {
        double bonus = 0.0;

        Pattern goodMethodPattern = Pattern.compile("void\\s+should[A-Z]|void\\s+test[A-Z]|void\\s+when[A-Z]");
        Matcher matcher = goodMethodPattern.matcher(code);
        int goodMethodCount = 0;
        while (matcher.find()) {
            goodMethodCount++;
        }

        Pattern poorMethodPattern = Pattern.compile("void\\s+test\\d+|void\\s+test\\(\\)");
        matcher = poorMethodPattern.matcher(code);
        int poorMethodCount = 0;
        while (matcher.find()) {
            poorMethodCount++;
        }

        if (goodMethodCount > 0) {
            bonus += 0.1 * Math.min(goodMethodCount, 3);
        }
        if (poorMethodCount > 0) {
            bonus -= 0.1 * Math.min(poorMethodCount, 3);
        }

        return bonus;
    }

    private double calculateComplexityPenalty(String code) {
        int nestingLevel = calculateNestingLevel(code);
        if (nestingLevel > 4) {
            return 0.2;
        } else if (nestingLevel > 2) {
            return 0.1;
        }
        return 0.0;
    }

    private int calculateNestingLevel(String code) {
        int maxLevel = 0;
        int currentLevel = 0;
        for (char c : code.toCharArray()) {
            if (c == '{') {
                currentLevel++;
                maxLevel = Math.max(maxLevel, currentLevel);
            } else if (c == '}') {
                currentLevel--;
            }
        }
        return maxLevel;
    }

    private double calculateCommentBonus(String code) {
        int commentLines = 0;
        String[] lines = code.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                commentLines++;
            }
        }
        return commentLines > 0 ? 0.05 : 0.0;
    }

    public double calculateAssertionDensity(String testCode) {
        if (testCode == null || testCode.isEmpty()) {
            return 0.0;
        }

        String[] lines = testCode.split("\n");
        int assertionCount = 0;
        int codeLineCount = 0;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("/*")) {
                continue;
            }
            codeLineCount++;
            if (trimmed.contains("assert") || trimmed.contains("expect") || trimmed.contains("verify")) {
                assertionCount++;
            }
        }

        return codeLineCount > 0 ? (double) assertionCount / codeLineCount : 0.0;
    }

    public double calculateIndependence(String testCode) {
        if (testCode == null || testCode.isEmpty()) {
            return 1.0;
        }

        double score = 1.0;

        if (testCode.contains("@BeforeEach") || testCode.contains("@Before")) {
            String setUpContent = extractSetUpContent(testCode);
            if (setUpContent != null && setUpContent.contains("setState")) {
                score -= 0.2;
            }
        }

        if (testCode.contains("static") && testCode.contains("shared")) {
            score -= 0.3;
        }

        return Math.max(0.0, score);
    }

    private String extractSetUpContent(String testCode) {
        int setUpStart = testCode.indexOf("@BeforeEach");
        if (setUpStart == -1) {
            setUpStart = testCode.indexOf("@Before");
        }
        if (setUpStart == -1) {
            return null;
        }

        int methodStart = testCode.indexOf("{", setUpStart);
        if (methodStart == -1) {
            return null;
        }

        int braceCount = 1;
        int pos = methodStart + 1;
        while (pos < testCode.length() && braceCount > 0) {
            if (testCode.charAt(pos) == '{') braceCount++;
            if (testCode.charAt(pos) == '}') braceCount--;
            pos++;
        }

        return testCode.substring(methodStart + 1, pos - 1);
    }

    public QualityReport generateReport(TestQualityMetrics metrics) {
        double overallScore = calculateOverallScore(metrics);
        QualityGrade grade = determineGrade(overallScore);
        List<String> recommendations = getImprovementSuggestions(metrics);

        return QualityReport.builder()
            .overallScore(overallScore)
            .grade(grade)
            .lineCoverage(metrics.lineCoverage())
            .branchCoverage(metrics.branchCoverage())
            .mutationScore(metrics.mutationScore())
            .readabilityScore(metrics.readabilityScore())
            .recommendations(recommendations)
            .build();
    }

    public List<String> getImprovementSuggestions(TestQualityMetrics metrics) {
        List<String> suggestions = new ArrayList<>();

        if (metrics.lineCoverage() < 0.80) {
            suggestions.add("Increase line coverage to at least 80% (current: " + 
                String.format("%.1f%%", metrics.lineCoverage() * 100) + ")");
        }

        if (metrics.branchCoverage() < 0.70) {
            suggestions.add("Add more branch coverage tests (current: " + 
                String.format("%.1f%%", metrics.branchCoverage() * 100) + ")");
        }

        if (metrics.mutationScore() < 0.80) {
            suggestions.add("Improve mutation score by adding stronger assertions (current: " + 
                String.format("%.1f%%", metrics.mutationScore() * 100) + ")");
        }

        if (metrics.readabilityScore() < 0.70) {
            suggestions.add("Improve test readability with better naming and structure");
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Test quality is good! Keep maintaining high standards.");
        }

        return suggestions;
    }

    public int compare(TestQualityMetrics metrics1, TestQualityMetrics metrics2) {
        double score1 = calculateOverallScore(metrics1);
        double score2 = calculateOverallScore(metrics2);
        return Double.compare(score1, score2);
    }

    public double calculateMaintainability(String testCode) {
        if (testCode == null || testCode.isEmpty()) {
            return 0.0;
        }

        double score = 1.0;

        int lines = testCode.split("\n").length;
        if (lines > 30) {
            score -= 0.2;
        }

        int magicNumbers = countMagicNumbers(testCode);
        score -= magicNumbers * 0.02;

        if (testCode.contains("assert") || testCode.contains("expect")) {
            score += 0.1;
        }

        return Math.max(0.0, Math.min(1.0, score));
    }

    private int countMagicNumbers(String code) {
        Pattern pattern = Pattern.compile("\\b\\d{2,}\\b");
        Matcher matcher = pattern.matcher(code);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public double calculateOrganization(String testCode) {
        if (testCode == null || testCode.isEmpty()) {
            return 0.0;
        }

        double score = 0.5;

        if (testCode.contains("@Nested")) {
            score += 0.2;
        }

        if (testCode.contains("@DisplayName")) {
            score += 0.1;
        }

        if (testCode.contains("@Tag") || testCode.contains("@Tags")) {
            score += 0.1;
        }

        Pattern methodPattern = Pattern.compile("@Test\\s+void\\s+\\w+");
        Matcher matcher = methodPattern.matcher(testCode);
        int testCount = 0;
        while (matcher.find()) {
            testCount++;
        }
        if (testCount > 1) {
            score += 0.1;
        }

        return Math.min(1.0, score);
    }

    public double calculateTestEffectivenessRatio(int testsCount, int defectsFound) {
        if (testsCount == 0) {
            return 0.0;
        }
        return (double) defectsFound / testsCount;
    }
}
