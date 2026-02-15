package com.utagent.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record ClassInfo(
    String packageName,
    String className,
    String fullyQualifiedName,
    List<MethodInfo> methods,
    List<FieldInfo> fields,
    List<AnnotationInfo> annotations,
    List<String> imports,
    String superClass,
    List<String> interfaces,
    boolean isInterface,
    boolean isEnum,
    boolean isRecord,
    Map<String, Object> metadata
) {
    public ClassInfo(String packageName, String className, String fullyQualifiedName) {
        this(packageName, className, fullyQualifiedName, 
             new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
             new ArrayList<>(), null, new ArrayList<>(),
             false, false, false, new HashMap<>());
    }

    public boolean hasAnnotation(String annotationName) {
        return annotations.stream()
            .anyMatch(a -> a.name().equals(annotationName) || 
                          a.name().endsWith("." + annotationName));
    }

    public boolean isSpringComponent() {
        return hasAnnotation("Component") || hasAnnotation("Service") ||
               hasAnnotation("Repository") || hasAnnotation("Controller") ||
               hasAnnotation("RestController");
    }

    public boolean isMyBatisMapper() {
        return hasAnnotation("Mapper");
    }
}
