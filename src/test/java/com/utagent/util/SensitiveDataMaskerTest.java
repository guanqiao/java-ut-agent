package com.utagent.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveDataMaskerTest {

    @Test
    void shouldMaskApiKey() {
        String input = "api_key=sk-1234567890abcdef";
        String result = SensitiveDataMasker.mask(input);

        assertTrue(result.contains("***"));
        assertFalse(result.contains("sk-1234567890abcdef"));
    }

    @Test
    void shouldMaskBearerToken() {
        String input = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9";
        String result = SensitiveDataMasker.mask(input);

        assertTrue(result.contains("***"));
        assertFalse(result.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"));
    }

    @Test
    void shouldMaskConfigSensitiveData() {
        String input = "apiKey: my-secret-api-key-12345";
        String result = SensitiveDataMasker.mask(input);

        assertTrue(result.contains("***"));
        assertFalse(result.contains("my-secret-api-key-12345"));
    }

    @ParameterizedTest
    @CsvSource({
        "api-key=secret123, true",
        "password=secret123, true",
        "api_token=secret123, true",
        "normal text without secrets, false"
    })
    void shouldDetectSensitiveData(String input, boolean expected) {
        assertEquals(expected, SensitiveDataMasker.containsSensitiveData(input));
    }

    @Test
    void shouldMaskPartially() {
        String value = "sk-1234567890abcdef";
        String result = SensitiveDataMasker.maskPartially(value);

        assertTrue(result.startsWith("sk-1"));
        assertTrue(result.endsWith("cdef"));
        assertTrue(result.contains("***"));
    }

    @Test
    void shouldMaskFully() {
        String value = "secret";
        String result = SensitiveDataMasker.maskFully(value);

        assertEquals("***", result);
    }

    @Test
    void shouldHandleNullInput() {
        assertNull(SensitiveDataMasker.mask(null));
        assertFalse(SensitiveDataMasker.containsSensitiveData(null));
    }

    @Test
    void shouldHandleEmptyInput() {
        assertEquals("", SensitiveDataMasker.mask(""));
        assertFalse(SensitiveDataMasker.containsSensitiveData(""));
    }
}
