package com.utagent.util;

import com.utagent.llm.LLMProviderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ApiKeyResolver Tests")
class ApiKeyResolverTest {

    @Nested
    @DisplayName("resolveFromEnv method tests")
    class ResolveFromEnvTests {

        @Test
        @DisplayName("Should return null for Ollama provider")
        void shouldReturnNullForOllamaProvider() {
            String apiKey = ApiKeyResolver.resolveFromEnv(LLMProviderType.OLLAMA);
            assertNull(apiKey);
        }

        @Test
        @DisplayName("Should return OPENAI_API_KEY environment variable for OpenAI")
        void shouldReturnOpenAiEnvVarForOpenAi() {
            String apiKey = ApiKeyResolver.resolveFromEnv(LLMProviderType.OPENAI);
            String expectedKey = System.getenv("OPENAI_API_KEY");
            assertEquals(expectedKey, apiKey);
        }

        @Test
        @DisplayName("Should return ANTHROPIC_API_KEY environment variable for Claude")
        void shouldReturnAnthropicEnvVarForClaude() {
            String apiKey = ApiKeyResolver.resolveFromEnv(LLMProviderType.CLAUDE);
            String expectedKey = System.getenv("ANTHROPIC_API_KEY");
            assertEquals(expectedKey, apiKey);
        }

        @Test
        @DisplayName("Should return DEEPSEEK_API_KEY environment variable for DeepSeek")
        void shouldReturnDeepSeekEnvVarForDeepSeek() {
            String apiKey = ApiKeyResolver.resolveFromEnv(LLMProviderType.DEEPSEEK);
            String expectedKey = System.getenv("DEEPSEEK_API_KEY");
            assertEquals(expectedKey, apiKey);
        }

        @ParameterizedTest
        @EnumSource(LLMProviderType.class)
        @DisplayName("Should not throw exception for any provider type")
        void shouldNotThrowExceptionForAnyProviderType(LLMProviderType providerType) {
            assertDoesNotThrow(() -> ApiKeyResolver.resolveFromEnv(providerType));
        }
    }

    @Nested
    @DisplayName("getEnvVarName method tests")
    class GetEnvVarNameTests {

        @Test
        @DisplayName("Should return OPENAI_API_KEY for OpenAI")
        void shouldReturnOpenAiEnvVarName() {
            String envVarName = ApiKeyResolver.getEnvVarName(LLMProviderType.OPENAI);
            assertEquals("OPENAI_API_KEY", envVarName);
        }

        @Test
        @DisplayName("Should return ANTHROPIC_API_KEY for Claude")
        void shouldReturnAnthropicEnvVarName() {
            String envVarName = ApiKeyResolver.getEnvVarName(LLMProviderType.CLAUDE);
            assertEquals("ANTHROPIC_API_KEY", envVarName);
        }

        @Test
        @DisplayName("Should return DEEPSEEK_API_KEY for DeepSeek")
        void shouldReturnDeepSeekEnvVarName() {
            String envVarName = ApiKeyResolver.getEnvVarName(LLMProviderType.DEEPSEEK);
            assertEquals("DEEPSEEK_API_KEY", envVarName);
        }

        @Test
        @DisplayName("Should return null for Ollama")
        void shouldReturnNullForOllama() {
            String envVarName = ApiKeyResolver.getEnvVarName(LLMProviderType.OLLAMA);
            assertNull(envVarName);
        }
    }

    @Nested
    @DisplayName("resolve method tests")
    class ResolveTests {

        @Test
        @DisplayName("Should return provided API key when not null")
        void shouldReturnProvidedApiKeyWhenNotNull() {
            String providedKey = "my-custom-api-key";
            String result = ApiKeyResolver.resolve(providedKey, LLMProviderType.OPENAI);
            assertEquals(providedKey, result);
        }

        @Test
        @DisplayName("Should resolve from environment when provided key is null")
        void shouldResolveFromEnvWhenProvidedKeyIsNull() {
            String result = ApiKeyResolver.resolve(null, LLMProviderType.OPENAI);
            String expectedKey = System.getenv("OPENAI_API_KEY");
            assertEquals(expectedKey, result);
        }

        @Test
        @DisplayName("Should return null for Ollama when provided key is null")
        void shouldReturnNullForOllamaWhenProvidedKeyIsNull() {
            String result = ApiKeyResolver.resolve(null, LLMProviderType.OLLAMA);
            assertNull(result);
        }

        @Test
        @DisplayName("Should return provided key for Ollama even if it's not null")
        void shouldReturnProvidedKeyForOllama() {
            String providedKey = "ollama-custom-key";
            String result = ApiKeyResolver.resolve(providedKey, LLMProviderType.OLLAMA);
            assertEquals(providedKey, result);
        }
    }

    @Nested
    @DisplayName("resolveByProviderId method tests")
    class ResolveByProviderIdTests {

        @Test
        @DisplayName("Should resolve by provider ID string - openai")
        void shouldResolveByProviderIdOpenAi() {
            String result = ApiKeyResolver.resolveByProviderId(null, "openai");
            String expectedKey = System.getenv("OPENAI_API_KEY");
            assertEquals(expectedKey, result);
        }

        @Test
        @DisplayName("Should resolve by provider ID string - claude")
        void shouldResolveByProviderIdClaude() {
            String result = ApiKeyResolver.resolveByProviderId(null, "claude");
            String expectedKey = System.getenv("ANTHROPIC_API_KEY");
            assertEquals(expectedKey, result);
        }

        @Test
        @DisplayName("Should resolve by provider ID string - deepseek")
        void shouldResolveByProviderIdDeepSeek() {
            String result = ApiKeyResolver.resolveByProviderId(null, "deepseek");
            String expectedKey = System.getenv("DEEPSEEK_API_KEY");
            assertEquals(expectedKey, result);
        }

        @Test
        @DisplayName("Should return null for unknown provider ID")
        void shouldReturnNullForUnknownProviderId() {
            String result = ApiKeyResolver.resolveByProviderId(null, "unknown");
            assertNull(result);
        }

        @Test
        @DisplayName("Should be case insensitive for provider ID")
        void shouldBeCaseInsensitiveForProviderId() {
            String result1 = ApiKeyResolver.resolveByProviderId(null, "OPENAI");
            String result2 = ApiKeyResolver.resolveByProviderId(null, "OpenAI");
            String expectedKey = System.getenv("OPENAI_API_KEY");
            assertEquals(expectedKey, result1);
            assertEquals(expectedKey, result2);
        }
    }

    @Nested
    @DisplayName("isApiKeyRequired method tests")
    class IsApiKeyRequiredTests {

        @Test
        @DisplayName("Should return true for OpenAI")
        void shouldReturnTrueForOpenAi() {
            assertTrue(ApiKeyResolver.isApiKeyRequired(LLMProviderType.OPENAI));
        }

        @Test
        @DisplayName("Should return true for Claude")
        void shouldReturnTrueForClaude() {
            assertTrue(ApiKeyResolver.isApiKeyRequired(LLMProviderType.CLAUDE));
        }

        @Test
        @DisplayName("Should return true for DeepSeek")
        void shouldReturnTrueForDeepSeek() {
            assertTrue(ApiKeyResolver.isApiKeyRequired(LLMProviderType.DEEPSEEK));
        }

        @Test
        @DisplayName("Should return false for Ollama")
        void shouldReturnFalseForOllama() {
            assertFalse(ApiKeyResolver.isApiKeyRequired(LLMProviderType.OLLAMA));
        }
    }

    @Nested
    @DisplayName("hasApiKeyInEnv method tests")
    class HasApiKeyInEnvTests {

        @Test
        @DisplayName("Should return true when environment variable is set")
        void shouldReturnTrueWhenEnvVarIsSet() {
            boolean hasKey = ApiKeyResolver.hasApiKeyInEnv(LLMProviderType.OPENAI);
            String envKey = System.getenv("OPENAI_API_KEY");
            if (envKey != null && !envKey.isEmpty()) {
                assertTrue(hasKey);
            } else {
                assertFalse(hasKey);
            }
        }

        @Test
        @DisplayName("Should always return true for Ollama")
        void shouldAlwaysReturnTrueForOllama() {
            assertTrue(ApiKeyResolver.hasApiKeyInEnv(LLMProviderType.OLLAMA));
        }
    }
}
