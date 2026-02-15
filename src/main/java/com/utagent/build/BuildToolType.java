package com.utagent.build;

public enum BuildToolType {
    MAVEN("maven", "Maven", "pom.xml"),
    GRADLE("gradle", "Gradle", "build.gradle"),
    GRADLE_KOTLIN("gradle-kotlin", "Gradle (Kotlin DSL)", "build.gradle.kts"),
    UNKNOWN("unknown", "Unknown", null);
    
    private final String id;
    private final String displayName;
    private final String buildFileName;
    
    BuildToolType(String id, String displayName, String buildFileName) {
        this.id = id;
        this.displayName = displayName;
        this.buildFileName = buildFileName;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getBuildFileName() {
        return buildFileName;
    }
    
    public static BuildToolType fromId(String id) {
        for (BuildToolType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
