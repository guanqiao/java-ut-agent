package com.utagent.patterns;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TestPattern {
    private final String name;
    private final PatternCategory category;
    private final String description;
    private final String template;
    private final List<String> tags;
    private final List<String> applicableClassTypes;

    private TestPattern(Builder builder) {
        this.name = builder.name;
        this.category = builder.category;
        this.description = builder.description;
        this.template = builder.template;
        this.tags = builder.tags;
        this.applicableClassTypes = builder.applicableClassTypes;
    }

    public String getName() {
        return name;
    }

    public PatternCategory getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getTemplate() {
        return template;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getApplicableClassTypes() {
        return applicableClassTypes;
    }

    public String generateTemplate(String className, String methodName) {
        return template
            .replace("{{className}}", className)
            .replace("{{methodName}}", methodName)
            .replace("{{instanceName}}", toCamelCase(className));
    }

    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toLowerCase(str.charAt(0)) + str.substring(1);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private PatternCategory category;
        private String description;
        private String template;
        private List<String> tags = new ArrayList<>();
        private List<String> applicableClassTypes = new ArrayList<>();

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder category(PatternCategory category) {
            this.category = category;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder template(String template) {
            this.template = template;
            return this;
        }

        public Builder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }

        public Builder applicableClassTypes(List<String> types) {
            this.applicableClassTypes = types;
            return this;
        }

        public TestPattern build() {
            return new TestPattern(this);
        }
    }
}
