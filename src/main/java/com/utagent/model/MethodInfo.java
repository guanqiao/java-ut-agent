package com.utagent.model;

import java.util.ArrayList;
import java.util.List;

public record MethodInfo(
    String name,
    String returnType,
    List<ParameterInfo> parameters,
    List<AnnotationInfo> annotations,
    String body,
    int lineNumber,
    int endLineNumber,
    List<String> thrownExceptions,
    boolean isStatic,
    boolean isPrivate,
    boolean isProtected,
    boolean isPublic,
    boolean isAbstract,
    boolean isFinal
) {
    public MethodInfo(String name, String returnType) {
        this(name, returnType, new ArrayList<>(), new ArrayList<>(),
             null, 0, 0, new ArrayList<>(),
             false, false, false, true, false, false);
    }

    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        for (int i = 0; i < parameters.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameters.get(i).type()).append(" ").append(parameters.get(i).name());
        }
        sb.append(")");
        return sb.toString();
    }

    public boolean hasAnnotation(String annotationName) {
        return annotations.stream()
            .anyMatch(a -> a.name().equals(annotationName) || 
                          a.name().endsWith("." + annotationName));
    }
}
