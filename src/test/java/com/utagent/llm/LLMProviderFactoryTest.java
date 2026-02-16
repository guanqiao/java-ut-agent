package com.utagent.llm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LLMProviderFactory Tests")
class LLMProviderFactoryTest {

    @Nested
    @DisplayName("create method tests")
    class CreateMethodTests {

        @Test
        @DisplayName("Should create OpenAI provider")
        void shouldCreateOpenAIProvider() {
            LLMProvider provider = LLMProviderFactory.create(
                LLMProviderType.OPENAI, 
                "test-api-key"
            );
            
            assertNotNull(provider);
            assertTrue(provider instanceof CachedLLMProvider);
        }

        @Test
        @DisplayName("Should create Claude provider")
        void shouldCreateClaudeProvider() {
            LLMProvider provider = LLMProviderFactory.create(
                LLMProviderType.CLAUDE, 
                "test-api-key"
            );
            
            assertNotNull(provider);
            assertTrue(provider instanceof CachedLLMProvider);
        }

        @Test
        @DisplayName("Should create Ollama provider without API key")
        void shouldCreateOllamaProviderWithoutApiKey() {
            LLMProvider provider = LLMProviderFactory.create(
                LLMProviderType.OLLAMA, 
                null
            );
            
            assertNotNull(provider);
            assertTrue(provider instanceof CachedLLMProvider);
        }

        @Test
        @DisplayName("Should create DeepSeek provider")
        void shouldCreateDeepSeekProvider() {
            LLMProvider provider = LLMProviderFactory.create(
                LLMProviderType.DEEPSEEK, 
                "test-api-key"
            );
            
            assertNotNull(provider);
            assertTrue(provider instanceof CachedLLMProvider);
        }

        @Test
        @DisplayName("Should create provider with all parameters")
        void shouldCreateProviderWithAllParameters() {
            LLMProvider provider = LLMProviderFactory.create(
                LLMProviderType.OPENAI,
                "test-api-key",
                "https://custom.api.url",
                "gpt-4",
                "/path/to/cert.pem"
            );
            
            assertNotNull(provider);
        }

        @Test
        @DisplayName("Should create provider with config")
        void shouldCreateProviderWithConfig() {
            LLMConfig config = LLMConfig.builder()
                .provider("openai")
                .apiKey("test-api-key")
                .model("gpt-4")
                .build();
            
            LLMProvider provider = LLMProviderFactory.create(config);
            
            assertNotNull(provider);
        }
    }

    @Nested
    @DisplayName("getAvailableProviders tests")
    class GetAvailableProvidersTests {

        @Test
        @DisplayName("Should return all provider types")
        void shouldReturnAllProviderTypes() {
            Set<String> providers = LLMProviderFactory.getAvailableProviders();
            
            assertNotNull(providers);
            assertEquals(4, providers.size());
            assertTrue(providers.contains("openai"));
            assertTrue(providers.contains("claude"));
            assertTrue(providers.contains("ollama"));
            assertTrue(providers.contains("deepseek"));
        }
    }

    @Nested
    @DisplayName("isProviderAvailable tests")
    class IsProviderAvailableTests {

        @Test
        @DisplayName("Ollama should always be available")
        void ollamaShouldAlwaysBeAvailable() {
            boolean available = LLMProviderFactory.isProviderAvailable(LLMProviderType.OLLAMA);
            
            assertTrue(available);
        }

        @Test
        @DisplayName("OpenAI availability depends on environment variable")
        void openAiAvailabilityDependsOnEnvVar() {
            boolean available = LLMProviderFactory.isProviderAvailable(LLMProviderType.OPENAI);
            
            String envKey = System.getenv("OPENAI_API_KEY");
            if (envKey != null && !envKey.isEmpty()) {
                assertTrue(available);
            } else {
                assertFalse(available);
            }
        }

        @Test
        @DisplayName("Claude availability depends on environment variable")
        void claudeAvailabilityDependsOnEnvVar() {
            boolean available = LLMProviderFactory.isProviderAvailable(LLMProviderType.CLAUDE);
            
            String envKey = System.getenv("ANTHROPIC_API_KEY");
            if (envKey != null && !envKey.isEmpty()) {
                assertTrue(available);
            } else {
                assertFalse(available);
            }
        }

        @Test
        @DisplayName("DeepSeek availability depends on environment variable")
        void deepSeekAvailabilityDependsOnEnvVar() {
            boolean available = LLMProviderFactory.isProviderAvailable(LLMProviderType.DEEPSEEK);
            
            String envKey = System.getenv("DEEPSEEK_API_KEY");
            if (envKey != null && !envKey.isEmpty()) {
                assertTrue(available);
            } else {
                assertFalse(available);
            }
        }
    }

    @Nested
    @DisplayName("createFromEnv tests")
    class CreateFromEnvTests {

        @Test
        @DisplayName("Should create Ollama provider from environment")
        void shouldCreateOllamaProviderFromEnv() {
            LLMProvider provider = LLMProviderFactory.createFromEnv(LLMProviderType.OLLAMA);
            
            assertNotNull(provider);
        }
    }
}
