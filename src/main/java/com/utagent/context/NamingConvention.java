package com.utagent.context;

public record NamingConvention(
    String testClassSuffix,
    String testMethodPrefix,
    boolean useDisplayName
) {
    public static NamingConvention defaultConvention() {
        return new NamingConvention("Test", "should", true);
    }
}
