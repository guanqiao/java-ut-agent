package com.utagent.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

public record ParsedTestMethod(
    String methodName,
    String testedMethodName,
    List<String> annotations,
    String testCode,
    Set<String> coveredMethodSignatures
) {
    public ParsedTestMethod(String methodName, String testedMethodName, 
                           List<String> annotations, String testCode) {
        this(methodName, testedMethodName, annotations, testCode, new HashSet<>());
    }

    public boolean hasAnnotation(String annotationName) {
        return annotations.stream()
            .anyMatch(a -> a.contains(annotationName));
    }

    public boolean isTest() {
        return hasAnnotation("@Test");
    }

    public boolean isParameterizedTest() {
        return hasAnnotation("@ParameterizedTest");
    }

    public boolean isBeforeEach() {
        return hasAnnotation("@BeforeEach");
    }

    public boolean isAfterEach() {
        return hasAnnotation("@AfterEach");
    }

    public List<String> annotations() {
        return Collections.unmodifiableList(annotations);
    }

    public Set<String> coveredMethodSignatures() {
        return Collections.unmodifiableSet(coveredMethodSignatures);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String methodName;
        private String testedMethodName;
        private List<String> annotations = new ArrayList<>();
        private String testCode;
        private Set<String> coveredMethodSignatures = new HashSet<>();

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder testedMethodName(String testedMethodName) {
            this.testedMethodName = testedMethodName;
            return this;
        }

        public Builder annotations(List<String> annotations) {
            this.annotations = new ArrayList<>(annotations);
            return this;
        }

        public Builder addAnnotation(String annotation) {
            this.annotations.add(annotation);
            return this;
        }

        public Builder testCode(String testCode) {
            this.testCode = testCode;
            return this;
        }

        public Builder coveredMethodSignatures(Set<String> coveredMethodSignatures) {
            this.coveredMethodSignatures = new HashSet<>(coveredMethodSignatures);
            return this;
        }

        public Builder addCoveredMethodSignature(String signature) {
            this.coveredMethodSignatures.add(signature);
            return this;
        }

        public ParsedTestMethod build() {
            return new ParsedTestMethod(methodName, testedMethodName, 
                                       annotations, testCode, coveredMethodSignatures);
        }
    }
}
