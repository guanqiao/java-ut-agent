package com.utagent.generator;

import com.utagent.model.ClassInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TestGenerator Tests")
class TestGeneratorTest {

    private TestGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new TestGenerator();
    }

    @Test
    @DisplayName("Should generate test class without AI")
    void shouldGenerateTestClassWithoutAI() {
        ClassInfo classInfo = new ClassInfo(
            "com.example",
            "Calculator",
            "com.example.Calculator",
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = generator.generateTestClass(classInfo);
        
        assertNotNull(testCode);
        assertTrue(testCode.contains("CalculatorTest"));
    }

    @Test
    @DisplayName("Should generate test with JUnit 5 annotations")
    void shouldGenerateTestWithJUnit5Annotations() {
        ClassInfo classInfo = new ClassInfo(
            "com.example",
            "SimpleService",
            "com.example.SimpleService",
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = generator.generateTestClass(classInfo);
        
        assertNotNull(testCode);
        assertTrue(testCode.contains("Test"));
    }

    @Test
    @DisplayName("Should not be AI enabled without API key")
    void shouldNotBeAIEnabledWithoutApiKey() {
        assertFalse(generator.isAIEnabled());
    }
}
