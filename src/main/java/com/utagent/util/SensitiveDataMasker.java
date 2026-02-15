package com.utagent.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 敏感数据脱敏工具类。
 * 用于在日志和输出中隐藏敏感信息如 API 密钥、密码等。
 */
public final class SensitiveDataMasker {

    private static final String MASK = "***";
    private static final int VISIBLE_PREFIX_LENGTH = 4;
    private static final int VISIBLE_SUFFIX_LENGTH = 4;

    // API Key 模式
    private static final Pattern API_KEY_PATTERN = Pattern.compile(
        "(api[_-]?key|apikey|api_token|access_token|auth_token|secret|password|passwd|pwd)" +
        "[=:]\\s*([^\\s,;\"'\\]\\}]+)",
        Pattern.CASE_INSENSITIVE
    );

    // Bearer Token 模式
    private static final Pattern BEARER_TOKEN_PATTERN = Pattern.compile(
        "(Bearer\\s+)([^\\s,;\"']+)",
        Pattern.CASE_INSENSITIVE
    );

    // Authorization Header 模式
    private static final Pattern AUTH_HEADER_PATTERN = Pattern.compile(
        "(Authorization[=:]\\s*[^\\s,;\"']+\\s+)([^\\s,;\"']+)",
        Pattern.CASE_INSENSITIVE
    );

    // 配置中的敏感字段
    private static final Pattern CONFIG_SENSITIVE_PATTERN = Pattern.compile(
        "^(\\s*(api[_-]?key|secret|password|token|auth)\\s*[=:]\\s*)(.+)$",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    private SensitiveDataMasker() {
    }

    /**
     * 对文本中的敏感信息进行脱敏处理
     */
    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String masked = text;
        masked = maskApiKeys(masked);
        masked = maskBearerTokens(masked);
        masked = maskAuthHeaders(masked);
        masked = maskConfigSensitiveData(masked);

        return masked;
    }

    /**
     * 对 API Key 进行部分脱敏，保留前后几位
     */
    public static String maskPartially(String value) {
        if (value == null || value.length() <= VISIBLE_PREFIX_LENGTH + VISIBLE_SUFFIX_LENGTH) {
            return MASK;
        }

        String prefix = value.substring(0, VISIBLE_PREFIX_LENGTH);
        String suffix = value.substring(value.length() - VISIBLE_SUFFIX_LENGTH);
        return prefix + MASK + suffix;
    }

    /**
     * 完全脱敏（不保留任何字符）
     */
    public static String maskFully(String value) {
        return MASK;
    }

    private static String maskApiKeys(String text) {
        Matcher matcher = API_KEY_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            String maskedValue = maskPartially(value);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(key + "=" + maskedValue));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String maskBearerTokens(String text) {
        Matcher matcher = BEARER_TOKEN_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String prefix = matcher.group(1);
            String token = matcher.group(2);
            String maskedToken = maskPartially(token);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + maskedToken));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String maskAuthHeaders(String text) {
        Matcher matcher = AUTH_HEADER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String prefix = matcher.group(1);
            String token = matcher.group(2);
            String maskedToken = maskPartially(token);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + maskedToken));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private static String maskConfigSensitiveData(String text) {
        Matcher matcher = CONFIG_SENSITIVE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String prefix = matcher.group(1);
            String value = matcher.group(3);
            String maskedValue = maskPartially(value);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + maskedValue));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 检查文本是否包含敏感数据
     */
    public static boolean containsSensitiveData(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        return API_KEY_PATTERN.matcher(text).find() ||
               BEARER_TOKEN_PATTERN.matcher(text).find() ||
               AUTH_HEADER_PATTERN.matcher(text).find();
    }

    /**
     * 为日志消息脱敏
     */
    public static String maskForLogging(String message) {
        return mask(message);
    }

    /**
     * 为配置显示脱敏
     */
    public static String maskForDisplay(String config) {
        return mask(config);
    }
}
