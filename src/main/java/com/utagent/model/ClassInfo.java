package com.utagent.model;

import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * Returns an unmodifiable list of methods.
     */
    public List<MethodInfo> methods() {
        return Collections.unmodifiableList(methods);
    }

    /**
     * Returns an unmodifiable list of fields.
     */
    public List<FieldInfo> fields() {
        return Collections.unmodifiableList(fields);
    }

    /**
     * Returns an unmodifiable list of annotations.
     */
    public List<AnnotationInfo> annotations() {
        return Collections.unmodifiableList(annotations);
    }

    /**
     * Returns an unmodifiable list of imports.
     */
    public List<String> imports() {
        return Collections.unmodifiableList(imports);
    }

    /**
     * Returns an unmodifiable list of interfaces.
     */
    public List<String> interfaces() {
        return Collections.unmodifiableList(interfaces);
    }

    /**
     * Returns an unmodifiable map of metadata.
     */
    public Map<String, Object> metadata() {
        return Collections.unmodifiableMap(metadata);
    }
}
