package com.utagent.parser;

import java.util.HashSet;
import java.util.Set;

public enum FrameworkType {
    SPRING_MVC("Spring MVC"),
    SPRING_BOOT("Spring Boot"),
    MYBATIS("MyBatis"),
    MYBATIS_PLUS("MyBatis Plus"),
    SPRING_DATA_JPA("Spring Data JPA"),
    DUBBO("Apache Dubbo"),
    LOMBOK("Lombok"),
    REACTIVE("Spring WebFlux"),
    GRPC("gRPC"),
    MAPSTRUCT("MapStruct"),
    NONE("None");

    private final String displayName;

    FrameworkType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public boolean requiresSpringContext() {
        return this == SPRING_MVC || this == SPRING_BOOT || 
               this == SPRING_DATA_JPA || this == REACTIVE;
    }
    
    public boolean isPersistenceFramework() {
        return this == MYBATIS || this == MYBATIS_PLUS || this == SPRING_DATA_JPA;
    }
    
    public boolean isRpcFramework() {
        return this == DUBBO || this == GRPC;
    }
}
