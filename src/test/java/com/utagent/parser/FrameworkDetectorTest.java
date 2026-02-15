package com.utagent.parser;

import com.utagent.model.AnnotationInfo;
import com.utagent.model.ClassInfo;
import com.utagent.model.FieldInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FrameworkDetector Tests")
class FrameworkDetectorTest {

    private FrameworkDetector detector;

    @BeforeEach
    void setUp() {
        detector = new FrameworkDetector();
    }

    @Test
    @DisplayName("Should detect Spring MVC from annotations")
    void shouldDetectSpringMvcFromAnnotations() {
        ClassInfo classInfo = new ClassInfo(
            "com.example",
            "UserController",
            "com.example.UserController",
            new ArrayList<>(),
            new ArrayList<>(),
            List.of(new AnnotationInfo("RestController")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        Set<FrameworkType> frameworks = detector.detectFrameworks(classInfo);
        
        assertTrue(frameworks.contains(FrameworkType.SPRING_MVC));
    }

    @Test
    @DisplayName("Should detect Spring Boot from annotations")
    void shouldDetectSpringBootFromAnnotations() {
        ClassInfo classInfo = new ClassInfo(
            "com.example",
            "Application",
            "com.example.Application",
            new ArrayList<>(),
            new ArrayList<>(),
            List.of(new AnnotationInfo("SpringBootApplication")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        Set<FrameworkType> frameworks = detector.detectFrameworks(classInfo);
        
        assertTrue(frameworks.contains(FrameworkType.SPRING_BOOT));
    }

    @Test
    @DisplayName("Should detect MyBatis from Mapper annotation")
    void shouldDetectMyBatisFromMapperAnnotation() {
        ClassInfo classInfo = new ClassInfo(
            "com.example.mapper",
            "UserMapper",
            "com.example.mapper.UserMapper",
            new ArrayList<>(),
            new ArrayList<>(),
            List.of(new AnnotationInfo("Mapper")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            true, false, false, new java.util.HashMap<>()
        );

        Set<FrameworkType> frameworks = detector.detectFrameworks(classInfo);
        
        assertTrue(frameworks.contains(FrameworkType.MYBATIS));
    }

    @Test
    @DisplayName("Should detect MyBatis Plus from TableName annotation")
    void shouldDetectMyBatisPlusFromTableNameAnnotation() {
        ClassInfo classInfo = new ClassInfo(
            "com.example.entity",
            "User",
            "com.example.entity.User",
            new ArrayList<>(),
            new ArrayList<>(),
            List.of(new AnnotationInfo("TableName", java.util.Map.of("value", "t_user"))),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        Set<FrameworkType> frameworks = detector.detectFrameworks(classInfo);
        
        assertTrue(frameworks.contains(FrameworkType.MYBATIS_PLUS));
    }

    @Test
    @DisplayName("Should detect from imports")
    void shouldDetectFromImports() {
        ClassInfo classInfo = new ClassInfo(
            "com.example",
            "UserService",
            "com.example.UserService",
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            List.of(
                "org.springframework.stereotype.Service",
                "org.springframework.beans.factory.annotation.Autowired"
            ),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        Set<FrameworkType> frameworks = detector.detectFrameworks(classInfo);
        
        assertTrue(frameworks.contains(FrameworkType.SPRING_MVC));
    }

    @Test
    @DisplayName("Should identify controller class")
    void shouldIdentifyControllerClass() {
        ClassInfo classInfo = new ClassInfo(
            "com.example.controller",
            "UserController",
            "com.example.controller.UserController",
            new ArrayList<>(),
            new ArrayList<>(),
            List.of(new AnnotationInfo("RestController")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        assertTrue(detector.isController(classInfo));
    }

    @Test
    @DisplayName("Should identify service class")
    void shouldIdentifyServiceClass() {
        ClassInfo classInfo = new ClassInfo(
            "com.example.service",
            "UserService",
            "com.example.service.UserService",
            new ArrayList<>(),
            new ArrayList<>(),
            List.of(new AnnotationInfo("Service")),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        assertTrue(detector.isService(classInfo));
    }

    @Test
    @DisplayName("Should identify dependency injection")
    void shouldIdentifyDependencyInjection() {
        List<FieldInfo> fields = new ArrayList<>();
        fields.add(new FieldInfo("repository", "UserRepository", 
            List.of(new AnnotationInfo("Autowired")), false, false, true, false, false));
        
        ClassInfo classInfo = new ClassInfo(
            "com.example.service",
            "UserService",
            "com.example.service.UserService",
            new ArrayList<>(),
            fields,
            new ArrayList<>(),
            new ArrayList<>(),
            null,
            new ArrayList<>(),
            false, false, false, new java.util.HashMap<>()
        );

        assertTrue(detector.hasDependencyInjection(classInfo));
        assertEquals(1, detector.getInjectedDependencies(classInfo).size());
    }
}
