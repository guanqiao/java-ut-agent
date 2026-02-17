package com.utagent.util;

import com.utagent.llm.LLMProviderType;

public final class ApiKeyResolver {

    private ApiKeyResolver() {
    }

    public static String resolveFromEnv(LLMProviderType providerType) {
        String envVarName = getEnvVarName(providerType);
        if (envVarName == null) {
            return null;
        }
        return System.getenv(envVarName);
    }

    public static String getEnvVarName(LLMProviderType providerType) {
        return switch (providerType) {
            case OPENAI -> "OPENAI_API_KEY";
            case CLAUDE -> "ANTHROPIC_API_KEY";
            case DEEPSEEK -> "DEEPSEEK_API_KEY";
            case OLLAMA -> null;
        };
    }

    public static String resolve(String providedKey, LLMProviderType providerType) {
        if (providedKey != null) {
            return providedKey;
        }
        return resolveFromEnv(providerType);
    }

    public static String resolveByProviderId(String providedKey, String providerId) {
        if (providedKey != null) {
            return providedKey;
        }
        if (providerId == null) {
            return null;
        }
        LLMProviderType providerType = LLMProviderType.fromId(providerId.toLowerCase());
        return resolveFromEnv(providerType);
    }

    public static boolean isApiKeyRequired(LLMProviderType providerType) {
        return providerType != LLMProviderType.OLLAMA;
    }

    public static boolean hasApiKeyInEnv(LLMProviderType providerType) {
        if (providerType == LLMProviderType.OLLAMA) {
            return true;
        }
        String apiKey = resolveFromEnv(providerType);
        return apiKey != null && !apiKey.isEmpty();
    }
}
