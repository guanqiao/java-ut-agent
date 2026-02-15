package com.utagent.parser;

import com.utagent.model.AnnotationInfo;
import com.utagent.model.ClassInfo;
import com.utagent.model.FieldInfo;
import com.utagent.model.MethodInfo;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class FrameworkDetector {

    private static final Set<String> SPRING_ANNOTATIONS = Set.of(
        "Controller", "RestController", "Service", "Repository", "Component",
        "Configuration", "Bean", "Autowired", "Qualifier", "Value",
        "RequestMapping", "GetMapping", "PostMapping", "PutMapping", "DeleteMapping",
        "RequestParam", "PathVariable", "RequestBody", "ResponseBody"
    );

    private static final Set<String> SPRING_BOOT_ANNOTATIONS = Set.of(
        "SpringBootApplication", "EnableAutoConfiguration", "ConfigurationProperties",
        "ConditionalOnProperty", "ConditionalOnClass", "ConditionalOnBean"
    );

    private static final Set<String> MYBATIS_ANNOTATIONS = Set.of(
        "Mapper", "Select", "Insert", "Update", "Delete", "SelectProvider",
        "InsertProvider", "UpdateProvider", "DeleteProvider", "Options", "ResultMap"
    );

    private static final Set<String> MYBATIS_PLUS_ANNOTATIONS = Set.of(
        "TableName", "TableId", "TableField", "Version", "TableLogic",
        "InterceptorIgnore", "DS"
    );

    private static final Set<String> JPA_ANNOTATIONS = Set.of(
        "Entity", "Table", "Id", "GeneratedValue", "Column", "OneToMany",
        "ManyToOne", "ManyToMany", "OneToOne", "JoinColumn", "Query"
    );
    
    private static final Set<String> DUBBO_ANNOTATIONS = Set.of(
        "DubboService", "DubboReference", "Service", "Reference",
        "DubboComponentScan", "EnableDubbo"
    );
    
    private static final Set<String> LOMBOK_ANNOTATIONS = Set.of(
        "Data", "Getter", "Setter", "Builder", "AllArgsConstructor",
        "NoArgsConstructor", "RequiredArgsConstructor", "ToString",
        "EqualsAndHashCode", "Value", "Slf4j", "Log", "Log4j", "Log4j2",
        "FieldNameConstants", "With", "Singular", "Cleanup", "Synchronized",
        "Delegate", "Accessors", "FieldDefaults", "Builder.Default"
    );
    
    private static final Set<String> REACTIVE_ANNOTATIONS = Set.of(
        "RestController", "GetMapping", "PostMapping", "PutMapping", "DeleteMapping"
    );
    
    private static final Set<String> GRPC_ANNOTATIONS = Set.of(
        "GrpcService"
    );
    
    private static final Set<String> MAPSTRUCT_ANNOTATIONS = Set.of(
        "Mapper", "Mapping", "Mappings", "MappingTarget", "InheritInverseConfiguration",
        "ValueMappings", "ValueMapping", "Named", "AfterMapping", "BeforeMapping"
    );

    private final Set<FrameworkType> detectedFrameworks = EnumSet.noneOf(FrameworkType.class);
    private final List<String> detectedImports = new ArrayList<>();

    public Set<FrameworkType> detectFrameworks(ClassInfo classInfo) {
        detectedFrameworks.clear();
        
        detectFromAnnotations(classInfo.annotations());
        
        for (MethodInfo method : classInfo.methods()) {
            detectFromAnnotations(method.annotations());
        }
        
        for (FieldInfo field : classInfo.fields()) {
            detectFromAnnotations(field.annotations());
        }
        
        detectFromImports(classInfo.imports());
        
        return EnumSet.copyOf(detectedFrameworks);
    }

    public Set<FrameworkType> detectFrameworks(List<ClassInfo> classes) {
        Set<FrameworkType> allFrameworks = EnumSet.noneOf(FrameworkType.class);
        
        for (ClassInfo classInfo : classes) {
            allFrameworks.addAll(detectFrameworks(classInfo));
        }
        
        return allFrameworks;
    }

    private void detectFromAnnotations(List<AnnotationInfo> annotations) {
        for (AnnotationInfo annotation : annotations) {
            String annotationName = extractSimpleName(annotation.name());
            
            if (SPRING_BOOT_ANNOTATIONS.contains(annotationName)) {
                detectedFrameworks.add(FrameworkType.SPRING_BOOT);
            }
            if (SPRING_ANNOTATIONS.contains(annotationName)) {
                detectedFrameworks.add(FrameworkType.SPRING_MVC);
            }
            if (MYBATIS_PLUS_ANNOTATIONS.contains(annotationName)) {
                detectedFrameworks.add(FrameworkType.MYBATIS_PLUS);
                detectedFrameworks.add(FrameworkType.MYBATIS);
            }
            if (MYBATIS_ANNOTATIONS.contains(annotationName)) {
                detectedFrameworks.add(FrameworkType.MYBATIS);
            }
            if (JPA_ANNOTATIONS.contains(annotationName)) {
                detectedFrameworks.add(FrameworkType.SPRING_DATA_JPA);
            }
            if (DUBBO_ANNOTATIONS.contains(annotationName)) {
                detectedFrameworks.add(FrameworkType.DUBBO);
            }
            if (LOMBOK_ANNOTATIONS.contains(annotationName)) {
                detectedFrameworks.add(FrameworkType.LOMBOK);
            }
            if (GRPC_ANNOTATIONS.contains(annotationName)) {
                detectedFrameworks.add(FrameworkType.GRPC);
            }
            if (MAPSTRUCT_ANNOTATIONS.contains(annotationName)) {
                detectedFrameworks.add(FrameworkType.MAPSTRUCT);
            }
        }
    }

    private void detectFromImports(List<String> imports) {
        for (String importStatement : imports) {
            if (importStatement.startsWith("org.springframework.boot")) {
                detectedFrameworks.add(FrameworkType.SPRING_BOOT);
            }
            if (importStatement.startsWith("org.springframework.web") ||
                importStatement.startsWith("org.springframework.stereotype")) {
                detectedFrameworks.add(FrameworkType.SPRING_MVC);
            }
            if (importStatement.startsWith("com.baomidou.mybatisplus")) {
                detectedFrameworks.add(FrameworkType.MYBATIS_PLUS);
                detectedFrameworks.add(FrameworkType.MYBATIS);
            }
            if (importStatement.startsWith("org.apache.ibatis")) {
                detectedFrameworks.add(FrameworkType.MYBATIS);
            }
            if (importStatement.startsWith("org.springframework.data.jpa") ||
                importStatement.startsWith("jakarta.persistence")) {
                detectedFrameworks.add(FrameworkType.SPRING_DATA_JPA);
            }
            if (importStatement.startsWith("org.apache.dubbo") ||
                importStatement.startsWith("com.alibaba.dubbo")) {
                detectedFrameworks.add(FrameworkType.DUBBO);
            }
            if (importStatement.startsWith("lombok")) {
                detectedFrameworks.add(FrameworkType.LOMBOK);
            }
            if (importStatement.startsWith("org.springframework.web.reactive") ||
                importStatement.startsWith("reactor.") ||
                importStatement.startsWith("io.reactivex")) {
                detectedFrameworks.add(FrameworkType.REACTIVE);
            }
            if (importStatement.startsWith("io.grpc")) {
                detectedFrameworks.add(FrameworkType.GRPC);
            }
            if (importStatement.startsWith("org.mapstruct")) {
                detectedFrameworks.add(FrameworkType.MAPSTRUCT);
            }
        }
    }

    private String extractSimpleName(String fullName) {
        if (fullName == null) return "";
        int lastDot = fullName.lastIndexOf('.');
        return lastDot >= 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    public boolean isController(ClassInfo classInfo) {
        return classInfo.hasAnnotation("Controller") || 
               classInfo.hasAnnotation("RestController");
    }

    public boolean isService(ClassInfo classInfo) {
        return classInfo.hasAnnotation("Service") ||
               classInfo.hasAnnotation("DubboService");
    }

    public boolean isRepository(ClassInfo classInfo) {
        return classInfo.hasAnnotation("Repository") ||
               classInfo.hasAnnotation("Mapper") ||
               classInfo.fullyQualifiedName().endsWith("Repository") ||
               classInfo.fullyQualifiedName().endsWith("Dao");
    }

    public boolean isMyBatisMapper(ClassInfo classInfo) {
        return classInfo.hasAnnotation("Mapper") ||
               detectedFrameworks.contains(FrameworkType.MYBATIS) &&
               classInfo.isInterface();
    }

    public boolean isEntity(ClassInfo classInfo) {
        return classInfo.hasAnnotation("Entity") ||
               classInfo.hasAnnotation("TableName");
    }
    
    public boolean isDubboService(ClassInfo classInfo) {
        return classInfo.hasAnnotation("DubboService") ||
               classInfo.hasAnnotation("Service") && 
               detectedFrameworks.contains(FrameworkType.DUBBO);
    }
    
    public boolean isReactiveController(ClassInfo classInfo) {
        return isController(classInfo) && 
               detectedFrameworks.contains(FrameworkType.REACTIVE);
    }
    
    public boolean isMapStructMapper(ClassInfo classInfo) {
        return classInfo.hasAnnotation("Mapper") && 
               detectedFrameworks.contains(FrameworkType.MAPSTRUCT);
    }
    
    public boolean hasLombok(ClassInfo classInfo) {
        return detectedFrameworks.contains(FrameworkType.LOMBOK);
    }
    
    public boolean hasBuilder(ClassInfo classInfo) {
        return classInfo.hasAnnotation("Builder");
    }
    
    public boolean hasData(ClassInfo classInfo) {
        return classInfo.hasAnnotation("Data");
    }

    public boolean hasDependencyInjection(ClassInfo classInfo) {
        for (FieldInfo field : classInfo.fields()) {
            if (field.isDependencyInjection()) {
                return true;
            }
        }
        return false;
    }

    public List<FieldInfo> getInjectedDependencies(ClassInfo classInfo) {
        List<FieldInfo> dependencies = new ArrayList<>();
        for (FieldInfo field : classInfo.fields()) {
            if (field.isDependencyInjection()) {
                dependencies.add(field);
            }
        }
        return dependencies;
    }
}
