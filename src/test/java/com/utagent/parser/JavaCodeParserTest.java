package com.utagent.parser;

import com.utagent.model.ClassInfo;
import com.utagent.model.FieldInfo;
import com.utagent.model.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JavaCodeParser Tests")
class JavaCodeParserTest {

    private JavaCodeParser parser;

    @BeforeEach
    void setUp() {
        parser = new JavaCodeParser();
    }

    @Test
    @DisplayName("Should parse simple class")
    void shouldParseSimpleClass() {
        String code = """
            package com.example;
            
            public class SimpleClass {
                private String name;
                
                public String getName() {
                    return name;
                }
                
                public void setName(String name) {
                    this.name = name;
                }
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        assertEquals("com.example", classInfo.packageName());
        assertEquals("SimpleClass", classInfo.className());
        assertEquals("com.example.SimpleClass", classInfo.fullyQualifiedName());
        assertEquals(2, classInfo.methods().size());
        assertEquals(1, classInfo.fields().size());
    }

    @Test
    @DisplayName("Should parse class with annotations")
    void shouldParseClassWithAnnotations() {
        String code = """
            package com.example;
            
            import org.springframework.stereotype.Service;
            import org.springframework.beans.factory.annotation.Autowired;
            
            @Service
            public class UserService {
                @Autowired
                private UserRepository repository;
                
                public User findById(Long id) {
                    return repository.findById(id);
                }
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        assertTrue(classInfo.hasAnnotation("Service"));
        assertEquals(2, classInfo.imports().size());
    }

    @Test
    @DisplayName("Should parse record")
    void shouldParseRecord() {
        String code = """
            package com.example;
            
            public record Person(String name, int age) {
                public boolean isAdult() {
                    return age >= 18;
                }
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent(), "Should parse record successfully");
        
        ClassInfo classInfo = result.get();
        assertEquals("Person", classInfo.className());
        assertTrue(classInfo.isRecord());
    }

    @Test
    @DisplayName("Should parse method with parameters")
    void shouldParseMethodWithParameters() {
        String code = """
            package com.example;
            
            public class Calculator {
                public int add(int a, int b) {
                    return a + b;
                }
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        MethodInfo method = classInfo.methods().get(0);
        
        assertEquals("add", method.name());
        assertEquals("int", method.returnType());
        assertEquals(2, method.parameters().size());
        assertEquals("a", method.parameters().get(0).name());
        assertEquals("int", method.parameters().get(0).type());
    }

    @Test
    @DisplayName("Should parse enum")
    void shouldParseEnum() {
        String code = """
            package com.example;
            
            public enum Status {
                ACTIVE,
                INACTIVE,
                PENDING;
                
                public boolean isActive() {
                    return this == ACTIVE;
                }
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        assertTrue(classInfo.isEnum());
        assertEquals("Status", classInfo.className());
    }

    @Test
    @DisplayName("Should parse class with inheritance")
    void shouldParseClassWithInheritance() {
        String code = """
            package com.example;
            
            public class CustomService extends BaseService implements ServiceInterface, AnotherInterface {
                @Override
                public void doSomething() {
                }
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        assertEquals("BaseService", classInfo.superClass());
        assertEquals(2, classInfo.interfaces().size());
        assertTrue(classInfo.interfaces().contains("ServiceInterface"));
        assertTrue(classInfo.interfaces().contains("AnotherInterface"));
    }

    @Test
    @DisplayName("Should parse interface")
    void shouldParseInterface() {
        String code = """
            package com.example;
            
            public interface UserService {
                User findById(Long id);
                List<User> findAll();
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        assertTrue(classInfo.isInterface());
        assertEquals("UserService", classInfo.className());
        assertEquals(2, classInfo.methods().size());
    }

    @Test
    @DisplayName("Should parse method with annotations")
    void shouldParseMethodWithAnnotations() {
        String code = """
            package com.example;
            
            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.PathVariable;
            
            @RestController
            public class UserController {
                @GetMapping("/users/{id}")
                public User getUser(@PathVariable Long id) {
                    return new User(id);
                }
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        MethodInfo method = classInfo.methods().get(0);
        
        assertTrue(method.hasAnnotation("GetMapping"));
        assertEquals("getUser", method.name());
        assertTrue(method.isPublic());
    }

    @Test
    @DisplayName("Should parse field with annotations")
    void shouldParseFieldWithAnnotations() {
        String code = """
            package com.example;
            
            import org.springframework.beans.factory.annotation.Value;
            
            @Component
            public class Config {
                @Value("${app.name}")
                private String appName;
                
                @Value("${app.version}")
                private String appVersion;
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        assertEquals(2, classInfo.fields().size());
        
        FieldInfo field = classInfo.fields().get(0);
        assertTrue(field.isDependencyInjection());
        assertEquals("appName", field.name());
    }

    @Test
    @DisplayName("Should parse method with throws clause")
    void shouldParseMethodWithThrowsClause() {
        String code = """
            package com.example;
            
            public class FileProcessor {
                public void processFile(String path) throws IOException, FileNotFoundException {
                    // Process file
                }
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        MethodInfo method = classInfo.methods().get(0);
        
        assertEquals(2, method.thrownExceptions().size());
        assertTrue(method.thrownExceptions().contains("IOException"));
    }

    @Test
    @DisplayName("Should parse static and final fields")
    void shouldParseStaticAndFinalFields() {
        String code = """
            package com.example;
            
            public class Constants {
                public static final String VERSION = "1.0.0";
                private static final int MAX_SIZE = 100;
                private String instanceField;
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        assertEquals(3, classInfo.fields().size());
        
        FieldInfo versionField = classInfo.fields().get(0);
        assertTrue(versionField.isStatic());
        assertTrue(versionField.isFinal());
        assertTrue(versionField.isPublic());
    }

    @Test
    @DisplayName("Should parse private and protected methods")
    void shouldParsePrivateAndProtectedMethods() {
        String code = """
            package com.example;
            
            public class Service {
                private void privateMethod() {
                }
                
                protected void protectedMethod() {
                }
                
                public void publicMethod() {
                }
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        assertEquals(3, classInfo.methods().size());
        
        for (MethodInfo method : classInfo.methods()) {
            if (method.name().equals("privateMethod")) {
                assertTrue(method.isPrivate());
            } else if (method.name().equals("protectedMethod")) {
                assertTrue(method.isProtected());
            } else if (method.name().equals("publicMethod")) {
                assertTrue(method.isPublic());
            }
        }
    }

    @Test
    @DisplayName("Should return empty for invalid code")
    void shouldReturnEmptyForInvalidCode() {
        String code = "this is not valid java code";

        var result = parser.parseCode(code);
        
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("Should parse class without package")
    void shouldParseClassWithoutPackage() {
        String code = """
            public class NoPackageClass {
                public void doSomething() {
                }
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        assertEquals("", classInfo.packageName());
        assertEquals("NoPackageClass", classInfo.className());
    }

    @Test
    @DisplayName("Should parse abstract class and methods")
    void shouldParseAbstractClassAndMethods() {
        String code = """
            package com.example;
            
            public abstract class AbstractService {
                public abstract void doSomething();
                
                public void concreteMethod() {
                }
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        
        for (MethodInfo method : classInfo.methods()) {
            if (method.name().equals("doSomething")) {
                assertTrue(method.isAbstract());
            }
        }
    }

    @Test
    @DisplayName("Should parse file from disk")
    void shouldParseFileFromDisk() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        Path tempFile = tempDir.resolve("TestClass.java");
        
        String code = """
            package com.test;
            
            public class TestClass {
                private String value;
                
                public String getValue() {
                    return value;
                }
            }
            """;
        
        Files.writeString(tempFile, code);
        
        var result = parser.parseFile(tempFile.toFile());
        
        assertTrue(result.isPresent());
        assertEquals("TestClass", result.get().className());
        
        Files.deleteIfExists(tempFile);
        Files.deleteIfExists(tempDir);
    }

    @Test
    @DisplayName("Should parse directory recursively")
    void shouldParseDirectoryRecursively() throws IOException {
        Path tempDir = Files.createTempDirectory("test");
        Path subDir = tempDir.resolve("subpackage");
        Files.createDirectories(subDir);
        
        Files.writeString(tempDir.resolve("ClassA.java"), """
            package com.test;
            public class ClassA {}
            """);
        
        Files.writeString(subDir.resolve("ClassB.java"), """
            package com.test.subpackage;
            public class ClassB {}
            """);
        
        List<ClassInfo> results = parser.parseDirectory(tempDir.toFile());
        
        assertEquals(2, results.size());
        
        Files.walk(tempDir).sorted((a, b) -> -a.compareTo(b)).forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException e) {
                // Ignore
            }
        });
    }

    @Test
    @DisplayName("Should handle varargs parameter")
    void shouldHandleVarargsParameter() {
        String code = """
            package com.example;
            
            public class Formatter {
                public String format(String pattern, Object... args) {
                    return String.format(pattern, args);
                }
            }
            """;

        var result = parser.parseCode(code);
        
        assertTrue(result.isPresent());
        
        ClassInfo classInfo = result.get();
        MethodInfo method = classInfo.methods().get(0);
        
        assertEquals(2, method.parameters().size());
        assertTrue(method.parameters().get(1).isVarArgs());
    }
}
