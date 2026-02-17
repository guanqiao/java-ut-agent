package com.utagent.generator;

import com.utagent.model.ClassInfo;
import com.utagent.model.CoverageInfo;
import com.utagent.model.MethodInfo;
import com.utagent.model.ParsedTestFile;
import com.utagent.model.ParsedTestMethod;
import com.utagent.parser.FrameworkDetector;
import com.utagent.parser.FrameworkType;
import com.utagent.parser.TestFileParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IncrementalTestGeneratorTest {

    private TestGenerator testGenerator;
    private TestFileParser testFileParser;
    private FrameworkDetector frameworkDetector;
    private IncrementalTestGenerator incrementalGenerator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        testGenerator = mock(TestGenerator.class);
        testFileParser = new TestFileParser();
        frameworkDetector = new FrameworkDetector();
        incrementalGenerator = new IncrementalTestGenerator(testGenerator, testFileParser, frameworkDetector);
    }

    @Test
    @DisplayName("Should generate new test class when no existing test file")
    void shouldGenerateNewTestClassWhenNoExistingTestFile() {
        ClassInfo classInfo = createClassInfo("MyService", "processData", "getData");
        
        when(testGenerator.generateTestClass(any())).thenReturn("new test code");
        
        IncrementalTestGenerator.IncrementalGenerationResult result = 
            incrementalGenerator.generateIncremental(classInfo, null, List.of());
        
        assertEquals(IncrementalTestGenerator.GenerationType.NEW, result.getGenerationType());
        assertEquals("new test code", result.getGeneratedTestCode());
        verify(testGenerator).generateTestClass(classInfo);
    }

    @Test
    @DisplayName("Should generate incremental tests for untested methods")
    void shouldGenerateIncrementalTestsForUntestedMethods() throws IOException {
        ClassInfo classInfo = createClassInfo("MyService", "processData", "getData", "deleteData");
        
        String existingTestContent = """
            package com.example;
            
            import org.junit.jupiter.api.Test;
            
            class MyServiceTest {
                @Test
                void shouldProcessDataSuccessfully() {
                }
            }
            """;
        
        Path testFile = tempDir.resolve("MyServiceTest.java");
        Files.writeString(testFile, existingTestContent);
        
        when(testGenerator.generateAdditionalTests(any(), any())).thenReturn("additional test methods");
        
        IncrementalTestGenerator.IncrementalGenerationResult result = 
            incrementalGenerator.generateIncremental(classInfo, testFile.toFile(), List.of());
        
        assertEquals(IncrementalTestGenerator.GenerationType.INCREMENTAL, result.getGenerationType());
        assertNotNull(result.getGeneratedTestCode());
    }

    @Test
    @DisplayName("Should return NONE when all methods are tested")
    void shouldReturnNoneWhenAllMethodsTested() throws IOException {
        ClassInfo classInfo = createClassInfo("MyService", "processData");
        
        String existingTestContent = """
            package com.example;
            
            import org.junit.jupiter.api.Test;
            
            class MyServiceTest {
                @Test
                void shouldProcessDataSuccessfully() {
                }
            }
            """;
        
        Path testFile = tempDir.resolve("MyServiceTest.java");
        Files.writeString(testFile, existingTestContent);
        
        when(testGenerator.generateAdditionalTests(any(), any())).thenReturn("");
        
        IncrementalTestGenerator.IncrementalGenerationResult result = 
            incrementalGenerator.generateIncremental(classInfo, testFile.toFile(), List.of());
        
        assertEquals(IncrementalTestGenerator.GenerationType.NONE, result.getGenerationType());
    }

    @Test
    @DisplayName("Should check if test file has existing tests")
    void shouldCheckIfTestFileHasExistingTests() throws IOException {
        String testContent = """
            package com.example;
            
            import org.junit.jupiter.api.Test;
            
            class MyServiceTest {
                @Test
                void shouldDoSomething() {
                }
            }
            """;
        
        Path testFile = tempDir.resolve("MyServiceTest.java");
        Files.writeString(testFile, testContent);
        
        assertTrue(incrementalGenerator.hasExistingTests(testFile.toFile()));
    }

    @Test
    @DisplayName("Should return false for non-existent test file")
    void shouldReturnFalseForNonExistentTestFile() {
        File nonExistent = new File("non-existent.java");
        
        assertFalse(incrementalGenerator.hasExistingTests(nonExistent));
    }

    @Test
    @DisplayName("Should get tested method names from test file")
    void shouldGetTestedMethodNames() throws IOException {
        String testContent = """
            package com.example;
            
            import org.junit.jupiter.api.Test;
            
            class MyServiceTest {
                @Test
                void shouldProcessDataSuccessfully() {
                }
                
                @Test
                void shouldGetDataSuccessfully() {
                }
            }
            """;
        
        Path testFile = tempDir.resolve("MyServiceTest.java");
        Files.writeString(testFile, testContent);
        
        Set<String> testedMethods = incrementalGenerator.getTestedMethodNames(testFile.toFile());
        
        assertTrue(testedMethods.contains("processData"));
        assertTrue(testedMethods.contains("getData"));
    }

    @Test
    @DisplayName("Should handle coverage info for incremental generation")
    void shouldHandleCoverageInfoForIncrementalGeneration() throws IOException {
        ClassInfo classInfo = createClassInfo("MyService", "processData");
        
        String existingTestContent = """
            package com.example;
            
            import org.junit.jupiter.api.Test;
            
            class MyServiceTest {
                @Test
                void shouldProcessDataSuccessfully() {
                }
            }
            """;
        
        Path testFile = tempDir.resolve("MyServiceTest.java");
        Files.writeString(testFile, existingTestContent);
        
        List<CoverageInfo> uncoveredInfo = List.of(
            new CoverageInfo("MyService", "processData", 10, 2, 1, 20, 5, 10, 3)
        );
        
        when(testGenerator.generateAdditionalTests(any(), any())).thenReturn("additional tests");
        
        IncrementalTestGenerator.IncrementalGenerationResult result = 
            incrementalGenerator.generateIncremental(classInfo, testFile.toFile(), uncoveredInfo);
        
        assertNotNull(result);
    }

    private ClassInfo createClassInfo(String className, String... methodNames) {
        List<MethodInfo> methods = new ArrayList<>();
        for (String methodName : methodNames) {
            methods.add(createMethodInfo(methodName));
        }
        
        return new ClassInfo(
            "com.example",
            className,
            "com.example." + className,
            methods,
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false,
            false,
            false,
            new java.util.HashMap<>()
        );
    }

    private MethodInfo createMethodInfo(String name) {
        return new MethodInfo(
            name,
            "void",
            new ArrayList<>(),
            new ArrayList<>(),
            null,
            0,
            0,
            new ArrayList<>(),
            false,
            false,
            false,
            true,
            false,
            false
        );
    }
}
