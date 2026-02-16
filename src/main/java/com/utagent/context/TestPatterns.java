package com.utagent.context;

import java.util.Set;

public record TestPatterns(
    Set<String> assertionStyles,
    Set<String> mockFrameworks,
    boolean usesBeforeEach,
    boolean usesParameterizedTests,
    boolean usesNestedTests
) {
    public static TestPatterns defaultPatterns() {
        return new TestPatterns(
            Set.of("AssertJ", "JUnit"),
            Set.of("Mockito"),
            true,
            true,
            true
        );
    }
}
