package com.utagent.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record ParsedTestFile(
    String packageName,
    String className,
    String fullyQualifiedName,
    List<String> imports,
    List<ParsedTestMethod> testMethods,
    List<String> classAnnotations,
    String classBody,
    Set<String> testedMethods
) {
    public ParsedTestFile(String packageName, String className) {
        this(packageName, className, packageName + "." + className,
             new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "", new HashSet<>());
    }

    public List<ParsedTestMethod> testMethods() {
        return Collections.unmodifiableList(testMethods);
    }

    public List<String> imports() {
        return Collections.unmodifiableList(imports);
    }

    public List<String> classAnnotations() {
        return Collections.unmodifiableList(classAnnotations);
    }

    public Set<String> testedMethods() {
        return Collections.unmodifiableSet(testedMethods);
    }

    public int getTestMethodCount() {
        return testMethods.size();
    }

    public boolean hasTestsForMethod(String methodName) {
        return testedMethods.contains(methodName);
    }

    public List<ParsedTestMethod> getTestsForMethod(String methodName) {
        return testMethods.stream()
            .filter(m -> methodName.equals(m.testedMethodName()))
            .toList();
    }

    public boolean hasClassAnnotation(String annotationName) {
        return classAnnotations.stream()
            .anyMatch(a -> a.contains(annotationName));
    }

    public boolean isSpringBootTest() {
        return hasClassAnnotation("@SpringBootTest") ||
               hasClassAnnotation("@WebMvcTest") ||
               hasClassAnnotation("@DataJpaTest") ||
               hasClassAnnotation("@MybatisTest");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String packageName;
        private String className;
        private String fullyQualifiedName;
        private List<String> imports = new ArrayList<>();
        private List<ParsedTestMethod> testMethods = new ArrayList<>();
        private List<String> classAnnotations = new ArrayList<>();
        private String classBody = "";
        private Set<String> testedMethods = new HashSet<>();

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder className(String className) {
            this.className = className;
            return this;
        }

        public Builder fullyQualifiedName(String fullyQualifiedName) {
            this.fullyQualifiedName = fullyQualifiedName;
            return this;
        }

        public Builder imports(List<String> imports) {
            this.imports = new ArrayList<>(imports);
            return this;
        }

        public Builder addImport(String imp) {
            this.imports.add(imp);
            return this;
        }

        public Builder testMethods(List<ParsedTestMethod> testMethods) {
            this.testMethods = new ArrayList<>(testMethods);
            return this;
        }

        public Builder addTestMethod(ParsedTestMethod method) {
            this.testMethods.add(method);
            return this;
        }

        public Builder classAnnotations(List<String> classAnnotations) {
            this.classAnnotations = new ArrayList<>(classAnnotations);
            return this;
        }

        public Builder addClassAnnotation(String annotation) {
            this.classAnnotations.add(annotation);
            return this;
        }

        public Builder classBody(String classBody) {
            this.classBody = classBody;
            return this;
        }

        public Builder testedMethods(Set<String> testedMethods) {
            this.testedMethods = new HashSet<>(testedMethods);
            return this;
        }

        public Builder addTestedMethod(String methodName) {
            this.testedMethods.add(methodName);
            return this;
        }

        public ParsedTestFile build() {
            if (fullyQualifiedName == null && packageName != null && className != null) {
                fullyQualifiedName = packageName + "." + className;
            }
            return new ParsedTestFile(packageName, className, fullyQualifiedName,
                                     imports, testMethods, classAnnotations, classBody, testedMethods);
        }
    }
}
