package com.utagent.generator.strategy;

import com.utagent.model.ClassInfo;
import com.utagent.model.MethodInfo;
import com.utagent.model.ParameterInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("SpringMvcTestStrategy Tests")
class SpringMvcTestStrategyTest {

    private SpringMvcTestStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new SpringMvcTestStrategy();
    }

    @Test
    @DisplayName("Should generate test class")
    void shouldGenerateTestClass() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("calculate", "int", 
            List.of(new ParameterInfo("a", "int"), new ParameterInfo("b", "int")),
            new ArrayList<>(), null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example",
            "Calculator",
            "com.example.Calculator",
            methods,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);
        
        assertNotNull(testCode);
        assertTrue(testCode.contains("CalculatorTest"));
    }

    @Test
    @DisplayName("Should include Mockito annotations")
    void shouldIncludeMockitoAnnotations() {
        ClassInfo classInfo = new ClassInfo(
            "com.example",
            "SimpleClass",
            "com.example.SimpleClass",
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);
        
        assertNotNull(testCode);
        assertTrue(testCode.contains("InjectMocks") || testCode.contains("Mock"));
    }

    @Test
    @DisplayName("Should generate Given-When-Then structure")
    void shouldGenerateGivenWhenThenStructure() {
        List<MethodInfo> methods = new ArrayList<>();
        methods.add(new MethodInfo("process", "String", 
            List.of(new ParameterInfo("input", "String")),
            new ArrayList<>(), null, 0, 0, new ArrayList<>(),
            false, false, false, true, false, false));

        ClassInfo classInfo = new ClassInfo(
            "com.example",
            "Processor",
            "com.example.Processor",
            methods,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        String testCode = strategy.generateTestClass(classInfo);
        
        assertTrue(testCode.contains("// Given"));
        assertTrue(testCode.contains("// When"));
        assertTrue(testCode.contains("// Then"));
    }
}
