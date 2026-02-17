package com.utagent.parser;

import com.utagent.model.ParsedTestFile;
import com.utagent.model.ParsedTestMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TestFileParserTest {

    private TestFileParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        parser = new TestFileParser();
    }

    @Test
    @DisplayName("Should parse package name correctly")
    void shouldParsePackageName() {
        String content = """
            package com.example.service;
            
            class MyServiceTest {
            }
            """;

        ParsedTestFile result = parser.parseContent(content);
        
        assertEquals("com.example.service", result.packageName());
    }

    @Test
    @DisplayName("Should parse class name correctly")
    void shouldParseClassName() {
        String content = """
            package com.example;
            
            class MyServiceTest {
            }
            """;

        ParsedTestFile result = parser.parseContent(content);
        
        assertEquals("MyServiceTest", result.className());
    }

    @Test
    @DisplayName("Should parse imports correctly")
    void shouldParseImports() {
        String content = """
            package com.example;
            
            import org.junit.jupiter.api.Test;
            import static org.junit.jupiter.api.Assertions.*;
            import java.util.List;
            
            class MyServiceTest {
            }
            """;

        ParsedTestFile result = parser.parseContent(content);
        
        assertTrue(result.imports().size() >= 2, "Should have at least 2 imports");
        assertTrue(result.imports().stream().anyMatch(i -> i.contains("junit")), 
                  "Should contain junit import");
    }

    @Test
    @DisplayName("Should parse test methods correctly")
    void shouldParseTestMethods() {
        String content = """
            package com.example;
            
            import org.junit.jupiter.api.Test;
            
            class MyServiceTest {
                @Test
                void shouldDoSomething() {
                }
                
                @Test
                void shouldDoAnotherThing() {
                }
            }
            """;

        ParsedTestFile result = parser.parseContent(content);
        
        assertEquals(2, result.getTestMethodCount());
    }

    @Test
    @DisplayName("Should parse BeforeEach method")
    void shouldParseBeforeEachMethod() {
        String content = """
            package com.example;
            
            import org.junit.jupiter.api.BeforeEach;
            import org.junit.jupiter.api.Test;
            
            class MyServiceTest {
                @BeforeEach
                void setUp() {
                }
                
                @Test
                void shouldDoSomething() {
                }
            }
            """;

        ParsedTestFile result = parser.parseContent(content);
        
        assertEquals(2, result.getTestMethodCount());
    }

    @Test
    @DisplayName("Should parse class annotations")
    void shouldParseClassAnnotations() {
        String content = """
            package com.example;
            
            import org.junit.jupiter.api.extension.ExtendWith;
            import org.mockito.junit.jupiter.MockitoExtension;
            
            @ExtendWith(MockitoExtension.class)
            class MyServiceTest {
            }
            """;

        ParsedTestFile result = parser.parseContent(content);
        
        assertTrue(result.hasClassAnnotation("ExtendWith"));
    }

    @Test
    @DisplayName("Should identify Spring Boot test")
    void shouldIdentifySpringBootTest() {
        String content = """
            package com.example;
            
            import org.springframework.boot.test.context.SpringBootTest;
            
            @SpringBootTest
            class MyServiceTest {
            }
            """;

        ParsedTestFile result = parser.parseContent(content);
        
        assertTrue(result.isSpringBootTest());
    }

    @Test
    @DisplayName("Should infer tested method name from test method name")
    void shouldInferTestedMethodName() {
        String content = """
            package com.example;
            
            import org.junit.jupiter.api.Test;
            
            class MyServiceTest {
                @Test
                void shouldGetDataSuccessfully() {
                }
                
                @Test
                void testProcessData() {
                }
            }
            """;

        ParsedTestFile result = parser.parseContent(content);
        
        assertTrue(result.testedMethods().contains("getData"));
        assertTrue(result.testedMethods().contains("ProcessData"));
    }

    @Test
    @DisplayName("Should parse test file from file system")
    void shouldParseTestFileFromFileSystem() throws IOException {
        String content = """
            package com.example;
            
            import org.junit.jupiter.api.Test;
            
            class MyServiceTest {
                @Test
                void shouldDoSomething() {
                }
            }
            """;
        
        Path testFile = tempDir.resolve("MyServiceTest.java");
        Files.writeString(testFile, content);
        
        Optional<ParsedTestFile> result = parser.parse(testFile.toFile());
        
        assertTrue(result.isPresent());
        assertEquals("MyServiceTest", result.get().className());
        assertEquals(1, result.get().getTestMethodCount());
    }

    @Test
    @DisplayName("Should return empty for non-existent file")
    void shouldReturnEmptyForNonExistentFile() {
        File nonExistent = new File("non-existent-file.java");
        
        Optional<ParsedTestFile> result = parser.parse(nonExistent);
        
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should identify test file correctly")
    void shouldIdentifyTestFile() throws IOException {
        Path testFile = tempDir.resolve("MyServiceTest.java");
        Path regularFile = tempDir.resolve("MyService.java");
        
        Files.writeString(testFile, "class MyServiceTest {}");
        Files.writeString(regularFile, "class MyService {}");
        
        assertTrue(parser.isTestFile(testFile.toFile()));
        assertFalse(parser.isTestFile(regularFile.toFile()));
    }

    @Test
    @DisplayName("Should find existing test file")
    void shouldFindExistingTestFile() throws IOException {
        Path sourceDir = tempDir.resolve("src/main/java/com/example");
        Path testDir = tempDir.resolve("src/test/java/com/example");
        
        Files.createDirectories(sourceDir);
        Files.createDirectories(testDir);
        
        Path sourceFile = sourceDir.resolve("MyService.java");
        Path testFile = testDir.resolve("MyServiceTest.java");
        
        Files.writeString(sourceFile, "package com.example; class MyService {}");
        Files.writeString(testFile, "package com.example; class MyServiceTest {}");
        
        Optional<File> found = parser.findExistingTestFile(
            sourceFile.toFile(), 
            testDir.getParent().toFile()
        );
        
        assertTrue(found.isPresent());
        assertEquals("MyServiceTest.java", found.get().getName());
    }

    @Test
    @DisplayName("Should return empty when test file not found")
    void shouldReturnEmptyWhenTestFileNotFound() throws IOException {
        Path sourceDir = tempDir.resolve("src/main/java/com/example");
        Path testDir = tempDir.resolve("src/test/java/com/example");
        
        Files.createDirectories(sourceDir);
        Files.createDirectories(testDir);
        
        Path sourceFile = sourceDir.resolve("MyService.java");
        Files.writeString(sourceFile, "package com.example; class MyService {}");
        
        Optional<File> found = parser.findExistingTestFile(
            sourceFile.toFile(), 
            testDir.getParent().toFile()
        );
        
        assertTrue(found.isEmpty());
    }

    @Test
    @DisplayName("Should parse method with annotations")
    void shouldParseMethodWithAnnotations() {
        String content = """
            package com.example;
            
            import org.junit.jupiter.api.Test;
            import org.junit.jupiter.api.DisplayName;
            
            class MyServiceTest {
                @Test
                @DisplayName("Should process data correctly")
                void shouldProcessDataCorrectly() {
                }
            }
            """;

        ParsedTestFile result = parser.parseContent(content);
        
        assertEquals(1, result.getTestMethodCount());
        ParsedTestMethod method = result.testMethods().get(0);
        assertTrue(method.hasAnnotation("@Test"));
        assertTrue(method.hasAnnotation("@DisplayName"));
    }
}
