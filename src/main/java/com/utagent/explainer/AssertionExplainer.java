package com.utagent.explainer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssertionExplainer {

    private static final Pattern ASSERT_EQUALS = Pattern.compile("assertEquals\\(([^,]+),\\s*([^)]+)\\)");
    private static final Pattern ASSERT_TRUE = Pattern.compile("assertTrue\\(([^)]+)\\)");
    private static final Pattern ASSERT_FALSE = Pattern.compile("assertFalse\\(([^)]+)\\)");
    private static final Pattern ASSERT_NULL = Pattern.compile("assertNull\\(([^)]+)\\)");
    private static final Pattern ASSERT_NOT_NULL = Pattern.compile("assertNotNull\\(([^)]+)\\)");
    private static final Pattern ASSERT_THROWS = Pattern.compile("assertThrows\\(([^,]+),\\s*([^)]+)\\)");
    private static final Pattern ASSERT_TIMEOUT = Pattern.compile("assertTimeout\\(([^,]+),\\s*([^)]+)\\)");
    private static final Pattern ASSERT_THAT = Pattern.compile("assertThat\\(([^)]+)\\)\\.([a-zA-Z]+)\\(([^)]*)\\)");
    private static final Pattern HAS_SIZE = Pattern.compile("hasSize\\((\\d+)\\)");
    private static final Pattern CONTAINS = Pattern.compile("contains\\(([^)]+)\\)");

    public String explain(String assertion) {
        if (assertion == null || assertion.isEmpty()) {
            return "";
        }

        return explainInChinese(assertion);
    }

    public String explainInChinese(String assertion) {
        if (assertion == null || assertion.isEmpty()) {
            return "";
        }

        Matcher assertEqualsMatcher = ASSERT_EQUALS.matcher(assertion);
        if (assertEqualsMatcher.find()) {
            String expected = assertEqualsMatcher.group(1).trim();
            String actual = assertEqualsMatcher.group(2).trim();
            return String.format("期望 %s 的值等于 %s", actual, expected);
        }

        Matcher assertTrueMatcher = ASSERT_TRUE.matcher(assertion);
        if (assertTrueMatcher.find()) {
            String condition = assertTrueMatcher.group(1).trim();
            return String.format("期望 %s 为真(true)", condition);
        }

        Matcher assertFalseMatcher = ASSERT_FALSE.matcher(assertion);
        if (assertFalseMatcher.find()) {
            String condition = assertFalseMatcher.group(1).trim();
            return String.format("期望 %s 为假(false)", condition);
        }

        Matcher assertNullMatcher = ASSERT_NULL.matcher(assertion);
        if (assertNullMatcher.find()) {
            String variable = assertNullMatcher.group(1).trim();
            return String.format("期望 %s 为 null", variable);
        }

        Matcher assertNotNullMatcher = ASSERT_NOT_NULL.matcher(assertion);
        if (assertNotNullMatcher.find()) {
            String variable = assertNotNullMatcher.group(1).trim();
            return String.format("期望 %s 不为 null", variable);
        }

        Matcher assertThrowsMatcher = ASSERT_THROWS.matcher(assertion);
        if (assertThrowsMatcher.find()) {
            String exceptionType = assertThrowsMatcher.group(1).trim();
            String action = assertThrowsMatcher.group(2).trim();
            return String.format("期望执行 %s 时抛出 %s 异常", action, exceptionType);
        }

        Matcher assertTimeoutMatcher = ASSERT_TIMEOUT.matcher(assertion);
        if (assertTimeoutMatcher.find()) {
            String duration = assertTimeoutMatcher.group(1).trim();
            String action = assertTimeoutMatcher.group(2).trim();
            return String.format("期望 %s 在 %s 内完成", action, duration);
        }

        Matcher assertThatMatcher = ASSERT_THAT.matcher(assertion);
        if (assertThatMatcher.find()) {
            String actual = assertThatMatcher.group(1).trim();
            String method = assertThatMatcher.group(2).trim();
            String expected = assertThatMatcher.group(3).trim();
            return explainAssertJ(actual, method, expected);
        }

        return "断言验证测试结果是否符合预期";
    }

    public String explainInEnglish(String assertion) {
        if (assertion == null || assertion.isEmpty()) {
            return "";
        }

        Matcher assertEqualsMatcher = ASSERT_EQUALS.matcher(assertion);
        if (assertEqualsMatcher.find()) {
            String expected = assertEqualsMatcher.group(1).trim();
            String actual = assertEqualsMatcher.group(2).trim();
            return String.format("Expects %s to equal %s", actual, expected);
        }

        Matcher assertTrueMatcher = ASSERT_TRUE.matcher(assertion);
        if (assertTrueMatcher.find()) {
            String condition = assertTrueMatcher.group(1).trim();
            return String.format("Expects %s to be true", condition);
        }

        Matcher assertFalseMatcher = ASSERT_FALSE.matcher(assertion);
        if (assertFalseMatcher.find()) {
            String condition = assertFalseMatcher.group(1).trim();
            return String.format("Expects %s to be false", condition);
        }

        Matcher assertNullMatcher = ASSERT_NULL.matcher(assertion);
        if (assertNullMatcher.find()) {
            String variable = assertNullMatcher.group(1).trim();
            return String.format("Expects %s to be null", variable);
        }

        Matcher assertNotNullMatcher = ASSERT_NOT_NULL.matcher(assertion);
        if (assertNotNullMatcher.find()) {
            String variable = assertNotNullMatcher.group(1).trim();
            return String.format("Expects %s to not be null", variable);
        }

        Matcher assertThrowsMatcher = ASSERT_THROWS.matcher(assertion);
        if (assertThrowsMatcher.find()) {
            String exceptionType = assertThrowsMatcher.group(1).trim();
            String action = assertThrowsMatcher.group(2).trim();
            return String.format("Expects %s to throw %s", action, exceptionType);
        }

        Matcher assertThatMatcher = ASSERT_THAT.matcher(assertion);
        if (assertThatMatcher.find()) {
            String actual = assertThatMatcher.group(1).trim();
            String method = assertThatMatcher.group(2).trim();
            String expected = assertThatMatcher.group(3).trim();
            return explainAssertJInEnglish(actual, method, expected);
        }

        return "Asserts that the test result meets expectations";
    }

    public String explainWithContext(String assertion, String context) {
        String baseExplanation = explain(assertion);
        if (context == null || context.isEmpty()) {
            return baseExplanation;
        }
        return String.format("【%s】%s", context, baseExplanation);
    }

    public String generateDocumentation(String testMethod) {
        if (testMethod == null || testMethod.isEmpty()) {
            return "";
        }

        StringBuilder doc = new StringBuilder();
        doc.append("/**\n");
        doc.append(" * 测试方法说明：\n");

        if (testMethod.contains("assertEquals")) {
            doc.append(" * - 验证结果值是否等于预期值\n");
        }
        if (testMethod.contains("assertTrue")) {
            doc.append(" * - 验证条件是否为真\n");
        }
        if (testMethod.contains("assertFalse")) {
            doc.append(" * - 验证条件是否为假\n");
        }
        if (testMethod.contains("assertNotNull")) {
            doc.append(" * - 验证对象不为空\n");
        }
        if (testMethod.contains("assertNull")) {
            doc.append(" * - 验证对象为空\n");
        }
        if (testMethod.contains("assertThrows")) {
            doc.append(" * - 验证是否抛出预期异常\n");
        }
        if (testMethod.contains("assertThat")) {
            doc.append(" * - 使用AssertJ进行断言验证\n");
        }

        doc.append(" */\n");
        return doc.toString();
    }

    public String extractPurpose(String assertion) {
        if (assertion == null || assertion.isEmpty()) {
            return "";
        }

        int commentStart = assertion.indexOf(", \"");
        if (commentStart > 0) {
            int commentEnd = assertion.lastIndexOf("\")");
            if (commentEnd > commentStart) {
                return assertion.substring(commentStart + 3, commentEnd);
            }
        }

        return explain(assertion);
    }

    private String explainAssertJ(String actual, String method, String expected) {
        return switch (method) {
            case "isTrue" -> String.format("期望 %s 为真(true)", actual);
            case "isFalse" -> String.format("期望 %s 为假(false)", actual);
            case "isEqualTo" -> String.format("期望 %s 等于 %s", actual, expected);
            case "isNotEqualTo" -> String.format("期望 %s 不等于 %s", actual, expected);
            case "isNull" -> String.format("期望 %s 为 null", actual);
            case "isNotNull" -> String.format("期望 %s 不为 null", actual);
            case "hasSize" -> String.format("期望 %s 的大小为 %s", actual, expected);
            case "contains" -> String.format("期望 %s 包含 %s", actual, expected);
            case "isEmpty" -> String.format("期望 %s 为空", actual);
            case "isNotEmpty" -> String.format("期望 %s 不为空", actual);
            case "isGreaterThan" -> String.format("期望 %s 大于 %s", actual, expected);
            case "isLessThan" -> String.format("期望 %s 小于 %s", actual, expected);
            case "isPositive" -> String.format("期望 %s 为正数", actual);
            case "isNegative" -> String.format("期望 %s 为负数", actual);
            case "isZero" -> String.format("期望 %s 为零", actual);
            default -> String.format("对 %s 进行断言验证", actual);
        };
    }

    private String explainAssertJInEnglish(String actual, String method, String expected) {
        return switch (method) {
            case "isTrue" -> String.format("Expects %s to be true", actual);
            case "isFalse" -> String.format("Expects %s to be false", actual);
            case "isEqualTo" -> String.format("Expects %s to equal %s", actual, expected);
            case "isNotEqualTo" -> String.format("Expects %s to not equal %s", actual, expected);
            case "isNull" -> String.format("Expects %s to be null", actual);
            case "isNotNull" -> String.format("Expects %s to not be null", actual);
            case "hasSize" -> String.format("Expects %s to have size %s", actual, expected);
            case "contains" -> String.format("Expects %s to contain %s", actual, expected);
            case "isEmpty" -> String.format("Expects %s to be empty", actual);
            case "isNotEmpty" -> String.format("Expects %s to not be empty", actual);
            case "isGreaterThan" -> String.format("Expects %s to be greater than %s", actual, expected);
            case "isLessThan" -> String.format("Expects %s to be less than %s", actual, expected);
            default -> String.format("Asserts on %s", actual);
        };
    }
}
