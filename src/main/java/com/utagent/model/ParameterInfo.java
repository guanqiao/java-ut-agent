package com.utagent.model;

public record ParameterInfo(
    String name,
    String type,
    boolean isVarArgs
) {
    public ParameterInfo(String name, String type) {
        this(name, type, false);
    }
}
