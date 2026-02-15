package com.utagent.parser;

import com.utagent.model.ClassInfo;
import com.utagent.model.MethodInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
