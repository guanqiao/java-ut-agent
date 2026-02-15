package com.utagent.model;

import java.util.ArrayList;
import java.util.List;

public record FieldInfo(
    String name,
    String type,
    List<AnnotationInfo> annotations,
    boolean isStatic,
    boolean isFinal,
    boolean isPrivate,
    boolean isProtected,
    boolean isPublic
) {
    public FieldInfo(String name, String type) {
        this(name, type, new ArrayList<>(), false, false, true, false, false);
    }

    public boolean hasAnnotation(String annotationName) {
        return annotations.stream()
            .anyMatch(a -> a.name().equals(annotationName) || 
                          a.name().endsWith("." + annotationName));
    }

    public boolean isDependencyInjection() {
        return hasAnnotation("Autowired") || hasAnnotation("Resource") ||
               hasAnnotation("Inject") || hasAnnotation("Value");
    }
}
