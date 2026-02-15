package com.utagent.parser;

import java.util.HashSet;
import java.util.Set;

public enum FrameworkType {
    SPRING_MVC("Spring MVC"),
    SPRING_BOOT("Spring Boot"),
    MYBATIS("MyBatis"),
    MYBATIS_PLUS("MyBatis Plus"),
    SPRING_DATA_JPA("Spring Data JPA"),
    NONE("None");

    private final String displayName;

    FrameworkType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
