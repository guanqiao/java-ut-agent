package com.utagent.i18n;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("I18nSupport Tests")
class I18nSupportTest {

    private I18nSupport i18n;

    @BeforeEach
    void setUp() {
        i18n = new I18nSupport();
    }

    @Nested
    @DisplayName("Message Translation")
    class MessageTranslation {

        @Test
        @DisplayName("Should get message in Chinese")
        void shouldGetMessageInChinese() {
            i18n.setLocale(Locale.CHINESE);
            
            String message = i18n.getMessage("test.generation.success");
            
            assertThat(message).isNotEmpty();
        }

        @Test
        @DisplayName("Should get message in English")
        void shouldGetMessageInEnglish() {
            i18n.setLocale(Locale.ENGLISH);
            
            String message = i18n.getMessage("test.generation.success");
            
            assertThat(message).isNotEmpty();
        }

        @Test
        @DisplayName("Should format message with parameters")
        void shouldFormatMessageWithParameters() {
            i18n.setLocale(Locale.ENGLISH);
            i18n.addCustomMessages(Map.of("test.coverage.report", "Coverage: {0}% of {1}"));
            
            String message = i18n.getMessage("test.coverage.report", 85, 100);
            
            assertThat(message).contains("85");
            assertThat(message).contains("100");
        }

        @Test
        @DisplayName("Should return key when message not found")
        void shouldReturnKeyWhenMessageNotFound() {
            i18n.setLocale(Locale.ENGLISH);
            
            String message = i18n.getMessage("nonexistent.key");
            
            assertThat(message).isEqualTo("nonexistent.key");
        }
    }

    @Nested
    @DisplayName("Locale Management")
    class LocaleManagement {

        @Test
        @DisplayName("Should set and get locale")
        void shouldSetAndGetLocale() {
            Locale locale = Locale.JAPANESE;
            
            i18n.setLocale(locale);
            
            assertThat(i18n.getLocale()).isEqualTo(locale);
        }

        @Test
        @DisplayName("Should default to system locale")
        void shouldDefaultToSystemLocale() {
            I18nSupport newI18n = new I18nSupport();
            
            assertThat(newI18n.getLocale()).isNotNull();
        }

        @Test
        @DisplayName("Should get available locales")
        void shouldGetAvailableLocales() {
            List<Locale> locales = i18n.getAvailableLocales();
            
            assertThat(locales).isNotEmpty();
            assertThat(locales).contains(Locale.CHINESE, Locale.ENGLISH);
        }
    }

    @Nested
    @DisplayName("Resource Bundle Management")
    class ResourceBundleManagement {

        @Test
        @DisplayName("Should load custom messages")
        void shouldLoadCustomMessages() {
            Map<String, String> customMessages = Map.of(
                "custom.key1", "Custom Value 1",
                "custom.key2", "Custom Value 2"
            );
            
            i18n.addCustomMessages(customMessages);
            
            assertThat(i18n.getMessage("custom.key1")).isEqualTo("Custom Value 1");
        }

        @Test
        @DisplayName("Should override existing messages")
        void shouldOverrideExistingMessages() {
            i18n.setLocale(Locale.ENGLISH);
            String original = i18n.getMessage("test.generation.success");
            
            i18n.addCustomMessages(Map.of("test.generation.success", "Overridden"));
            
            assertThat(i18n.getMessage("test.generation.success")).isEqualTo("Overridden");
        }
    }

    @Nested
    @DisplayName("Pluralization")
    class Pluralization {

        @Test
        @DisplayName("Should handle singular form")
        void shouldHandleSingularForm() {
            i18n.setLocale(Locale.ENGLISH);
            i18n.addCustomMessages(Map.of("test.count.singular", "{0} test"));
            
            String message = i18n.getPluralMessage("test.count", 1);
            
            assertThat(message).contains("1");
        }

        @Test
        @DisplayName("Should handle plural form")
        void shouldHandlePluralForm() {
            i18n.setLocale(Locale.ENGLISH);
            i18n.addCustomMessages(Map.of("test.count.plural", "{0} tests"));
            
            String message = i18n.getPluralMessage("test.count", 5);
            
            assertThat(message).contains("5");
        }
    }

    @Nested
    @DisplayName("Test Code Generation I18n")
    class TestCodeGenerationI18n {

        @Test
        @DisplayName("Should generate localized display name")
        void shouldGenerateLocalizedDisplayName() {
            i18n.setLocale(Locale.CHINESE);
            
            String displayName = i18n.generateTestDisplayName("add", "Calculator");
            
            assertThat(displayName).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate localized assertion message")
        void shouldGenerateLocalizedAssertionMessage() {
            i18n.setLocale(Locale.CHINESE);
            
            String message = i18n.generateAssertionMessage("expected", "actual", "add");
            
            assertThat(message).isNotEmpty();
        }

        @Test
        @DisplayName("Should generate localized comment")
        void shouldGenerateLocalizedComment() {
            i18n.setLocale(Locale.CHINESE);
            
            String comment = i18n.generateComment("arrange");
            
            assertThat(comment).isNotEmpty();
        }
    }
}
