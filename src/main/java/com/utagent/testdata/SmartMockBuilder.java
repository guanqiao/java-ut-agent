package com.utagent.testdata;

import com.utagent.model.ClassInfo;
import com.utagent.model.FieldInfo;
import com.utagent.model.MethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SmartMockBuilder {

    private static final Logger logger = LoggerFactory.getLogger(SmartMockBuilder.class);

    public MockConfiguration buildMockConfig(Class<?> type) {
        MockConfiguration config = new MockConfiguration(type);
        
        for (Method method : type.getDeclaredMethods()) {
            config.addStubbing(method.getName(), generateDefaultReturnValue(method));
        }
        
        return config;
    }

    public MockConfiguration buildSpyConfig(Class<?> type) {
        MockConfiguration config = buildMockConfig(type);
        config.setSpy(true);
        return config;
    }

    public List<MockConfiguration> detectDependencies(ClassInfo classInfo) {
        List<MockConfiguration> configs = new ArrayList<>();
        
        for (FieldInfo field : classInfo.fields()) {
            try {
                Class<?> fieldType = Class.forName(field.type());
                if (shouldMock(fieldType)) {
                    configs.add(buildMockConfig(fieldType));
                }
            } catch (ClassNotFoundException e) {
                logger.debug("Could not load class for field: {}", field.name());
            }
        }
        
        return configs;
    }

    private boolean shouldMock(Class<?> type) {
        if (Modifier.isFinal(type.getModifiers())) {
            return false;
        }
        if (type.isPrimitive() || type.getName().startsWith("java.lang")) {
            return false;
        }
        return type.isInterface() || Modifier.isAbstract(type.getModifiers());
    }

    public String generateImports(MockConfiguration config) {
        StringBuilder sb = new StringBuilder();
        
        if (config.isSpy()) {
            sb.append("import org.mockito.Spy;\n");
        } else {
            sb.append("import org.mockito.Mock;\n");
        }
        sb.append("import org.mockito.Mockito;\n");
        sb.append("import org.mockito.MockitoAnnotations;\n");
        
        return sb.toString();
    }

    public String generateMockDeclaration(MockConfiguration config) {
        StringBuilder sb = new StringBuilder();
        
        if (config.isSpy()) {
            sb.append("    @Spy\n");
        } else {
            sb.append("    @Mock\n");
        }
        sb.append("    private ").append(config.getMockType().getSimpleName())
          .append(" ").append(config.getMockName()).append(";\n");
        
        return sb.toString();
    }

    public String generateStubCode(MockConfiguration config) {
        StringBuilder sb = new StringBuilder();
        
        for (Map.Entry<String, String> entry : config.getStubbings().entrySet()) {
            String methodName = entry.getKey();
            String returnValue = entry.getValue();
            
            if (returnValue.contains(" -> ")) {
                String[] parts = returnValue.split(" -> ");
                sb.append("        Mockito.when(")
                  .append(config.getMockName()).append(".").append(methodName)
                  .append("(").append(parts[0]).append("))")
                  .append(".thenReturn(").append(parts[1]).append(");\n");
            } else {
                sb.append("        Mockito.when(")
                  .append(config.getMockName()).append(".").append(methodName)
                  .append("())")
                  .append(".thenReturn(").append(returnValue).append(");\n");
            }
        }
        
        return sb.toString();
    }

    public String generateVerifyCode(MockConfiguration config) {
        StringBuilder sb = new StringBuilder();
        
        for (Map.Entry<String, Integer> entry : config.getVerifications().entrySet()) {
            sb.append("        Mockito.verify(")
              .append(config.getMockName()).append(", Mockito.times(")
              .append(entry.getValue()).append(")).")
              .append(entry.getKey()).append("();\n");
        }
        
        return sb.toString();
    }

    public String generateSmartStub(MockConfiguration config, String methodName) {
        try {
            for (Method method : config.getMockType().getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    String returnValue = generateDefaultReturnValue(method);
                    
                    if (method.getReturnType() == void.class) {
                        return "        Mockito.doNothing().when(" + 
                               config.getMockName() + ")." + methodName + "();\n";
                    }
                    
                    return "        Mockito.when(" + config.getMockName() + 
                           "." + methodName + "()).thenReturn(" + returnValue + ");\n";
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to generate smart stub for {}", methodName);
        }
        return "";
    }

    private String generateDefaultReturnValue(Method method) {
        Class<?> returnType = method.getReturnType();
        
        if (returnType == void.class) {
            return "";
        }
        if (returnType == boolean.class || returnType == Boolean.class) {
            return "false";
        }
        if (returnType == int.class || returnType == Integer.class) {
            return "0";
        }
        if (returnType == long.class || returnType == Long.class) {
            return "0L";
        }
        if (returnType == double.class || returnType == Double.class) {
            return "0.0";
        }
        if (returnType == String.class) {
            return "\"\"";
        }
        if (returnType == List.class) {
            return "Collections.emptyList()";
        }
        if (returnType == Map.class) {
            return "Collections.emptyMap()";
        }
        if (returnType.getName().contains("Optional")) {
            return "Optional.empty()";
        }
        if (returnType.isInterface() || Modifier.isAbstract(returnType.getModifiers())) {
            return "Mockito.mock(" + returnType.getSimpleName() + ".class)";
        }
        
        return "null";
    }
}
