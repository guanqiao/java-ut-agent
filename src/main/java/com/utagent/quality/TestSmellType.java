package com.utagent.quality;

public enum TestSmellType {
    DUPLICATE_ASSERTION("Duplicate Assertion", "Same assertion appears multiple times"),
    MAGIC_NUMBER("Magic Number", "Hard-coded numbers without explanation"),
    LONG_TEST_METHOD("Long Test Method", "Test method exceeds recommended length"),
    EMPTY_TEST("Empty Test", "Test method has no implementation"),
    MISSING_ASSERTION("Missing Assertion", "Test lacks any assertions"),
    MULTIPLE_ASSERTIONS("Multiple Assertions", "Too many assertions in single test"),
    PRINT_STATEMENT("Print Statement", "Debug print statements in test"),
    IGNORED_TEST("Ignored Test", "Test is disabled or ignored"),
    COMPLEX_SETUP("Complex Setup", "Setup method is too complex"),
    ASSERTION_ROULETTE("Assertion Roulette", "Assertions without descriptive messages"),
    EAGER_TEST("Eager Test", "Test verifies multiple behaviors"),
    GENERAL_FIXTURE("General Fixture", "Setup creates objects not used by all tests");

    private final String name;
    private final String description;

    TestSmellType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String displayName() { return name; }
    public String description() { return description; }
}
