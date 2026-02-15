package com.utagent.model;

import java.util.HashMap;
import java.util.Map;

public record AnnotationInfo(
    String name,
    Map<String, Object> attributes
) {
    public AnnotationInfo(String name) {
        this(name, new HashMap<>());
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public String getAttributeAsString(String key) {
        Object value = attributes.get(key);
        return value != null ? value.toString() : null;
    }
}
