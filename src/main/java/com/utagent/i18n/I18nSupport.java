package com.utagent.i18n;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class I18nSupport {

    private static final Logger logger = LoggerFactory.getLogger(I18nSupport.class);

    private Locale currentLocale;
    private ResourceBundle resourceBundle;
    private final Map<String, String> customMessages;
    private final List<Locale> supportedLocales;

    public I18nSupport() {
        this.currentLocale = Locale.getDefault();
        this.customMessages = new HashMap<>();
        this.supportedLocales = new ArrayList<>();
        this.supportedLocales.add(Locale.CHINESE);
        this.supportedLocales.add(Locale.ENGLISH);
        this.supportedLocales.add(Locale.JAPANESE);
        loadResourceBundle();
    }

    private void loadResourceBundle() {
        try {
            resourceBundle = ResourceBundle.getBundle("i18n.messages", currentLocale);
        } catch (Exception e) {
            logger.warn("Could not load resource bundle for locale: {}", currentLocale);
            resourceBundle = null;
        }
    }

    public void setLocale(Locale locale) {
        this.currentLocale = locale;
        loadResourceBundle();
    }

    public Locale getLocale() {
        return currentLocale;
    }

    public String getMessage(String key) {
        if (customMessages.containsKey(key)) {
            return customMessages.get(key);
        }

        if (resourceBundle != null && resourceBundle.containsKey(key)) {
            return resourceBundle.getString(key);
        }

        return key;
    }

    public String getMessage(String key, Object... params) {
        String pattern = getMessage(key);
        if (pattern.equals(key)) {
            return key;
        }
        try {
            return MessageFormat.format(pattern, params);
        } catch (Exception e) {
            logger.warn("Failed to format message: {}", key, e);
            return pattern;
        }
    }

    public String getPluralMessage(String key, int count) {
        String pluralKey = count == 1 ? key + ".singular" : key + ".plural";
        String message = getMessage(pluralKey);
        if (!message.equals(pluralKey)) {
            return MessageFormat.format(message, count);
        }
        return getMessage(key, count);
    }

    public List<Locale> getAvailableLocales() {
        return new ArrayList<>(supportedLocales);
    }

    public void addCustomMessages(Map<String, String> messages) {
        customMessages.putAll(messages);
    }

    public String generateTestDisplayName(String methodName, String className) {
        String key = "test.displayname.format";
        String template = getMessage(key);
        
        if (template.equals(key)) {
            if (currentLocale.getLanguage().equals("zh")) {
                return String.format("测试 %s 的 %s 方法", className, methodName);
            }
            return String.format("Should %s in %s", methodName, className);
        }
        
        return MessageFormat.format(template, methodName, className);
    }

    public String generateAssertionMessage(String expected, String actual, String methodName) {
        String key = "test.assertion.message";
        String template = getMessage(key);
        
        if (template.equals(key)) {
            if (currentLocale.getLanguage().equals("zh")) {
                return String.format("在 %s 方法中，期望值为 %s，实际值为 %s", methodName, expected, actual);
            }
            return String.format("In %s, expected %s but was %s", methodName, expected, actual);
        }
        
        return MessageFormat.format(template, methodName, expected, actual);
    }

    public String generateComment(String type) {
        String key = "test.comment." + type;
        String comment = getMessage(key);
        
        if (comment.equals(key)) {
            return switch (type.toLowerCase()) {
                case "arrange" -> currentLocale.getLanguage().equals("zh") ? "准备" : "Arrange";
                case "act" -> currentLocale.getLanguage().equals("zh") ? "执行" : "Act";
                case "assert" -> currentLocale.getLanguage().equals("zh") ? "断言" : "Assert";
                default -> type;
            };
        }
        
        return comment;
    }
}
