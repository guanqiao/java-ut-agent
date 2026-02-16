package com.utagent.assertion;

import com.utagent.model.MethodInfo;
import com.utagent.model.ParameterInfo;

import java.util.ArrayList;
import java.util.List;

public class SmartAssertionGenerator {

    public String generateAssertion(MethodInfo method) {
        String returnType = method.returnType();
        
        if (returnType.equals("void")) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        
        if (returnType.equals("boolean") || returnType.equals("Boolean")) {
            sb.append("        assertThat(result).isTrue();\n");
        } else if (returnType.equals("String") || returnType.equals("java.lang.String")) {
            sb.append("        assertThat(result).isNotNull();\n");
            sb.append("        assertThat(result).isNotEmpty();\n");
        } else if (isNumericType(returnType)) {
            sb.append("        assertThat(result).isNotNull();\n");
            sb.append("        assertThat(result).isGreaterThanOrEqualTo(0);\n");
        } else if (returnType.equals("java.util.List") || returnType.equals("List")) {
            sb.append("        assertThat(result).isNotNull();\n");
            sb.append("        assertThat(result).isNotEmpty();\n");
        } else if (returnType.equals("java.util.Map") || returnType.equals("Map")) {
            sb.append("        assertThat(result).isNotNull();\n");
            sb.append("        assertThat(result).isNotEmpty();\n");
        } else if (returnType.equals("java.util.Optional") || returnType.equals("Optional")) {
            sb.append("        assertThat(result).isPresent();\n");
        } else {
            sb.append("        assertThat(result).isNotNull();\n");
        }
        
        return sb.toString();
    }

    public List<String> generateFieldAssertions(MethodInfo method, String objectName) {
        List<String> assertions = new ArrayList<>();
        String returnType = method.returnType();
        
        assertions.add("assertThat(" + objectName + ").isNotNull();");
        
        if (returnType.contains("User")) {
            assertions.add("assertThat(" + objectName + ".getId()).isNotNull();");
            assertions.add("assertThat(" + objectName + ".getName()).isNotEmpty();");
        } else if (returnType.contains("Entity")) {
            assertions.add("assertThat(" + objectName + ".getId()).isNotNull();");
        }
        
        return assertions;
    }

    public String generateExceptionAssertion(MethodInfo method) {
        List<String> exceptions = method.thrownExceptions();
        
        if (exceptions.isEmpty()) {
            return "";
        }
        
        String exceptionType = exceptions.get(0);
        String simpleName = exceptionType.substring(exceptionType.lastIndexOf('.') + 1);
        
        StringBuilder sb = new StringBuilder();
        sb.append("        assertThrows(").append(simpleName).append(".class, () -> {\n");
        sb.append("            // Invoke method that should throw\n");
        sb.append("        });\n");
        
        return sb.toString();
    }

    public String generateAssertionWithMessage(MethodInfo method, String message) {
        String baseAssertion = generateAssertion(method);
        
        if (baseAssertion.isEmpty()) {
            return "";
        }
        
        return baseAssertion.replace("assertThat(result)", 
            "assertThat(result).as(\"" + message + "\")");
    }

    public String generateAutoMessage(MethodInfo method) {
        String methodName = method.name();
        
        if (methodName.startsWith("get")) {
            String fieldName = methodName.substring(3);
            return "should return " + camelToWords(fieldName);
        } else if (methodName.startsWith("is")) {
            String fieldName = methodName.substring(2);
            return camelToWords(fieldName) + " should be correct";
        } else if (methodName.startsWith("has")) {
            String fieldName = methodName.substring(3);
            return "should have " + camelToWords(fieldName);
        } else if (methodName.startsWith("can")) {
            String action = methodName.substring(3);
            return "should be able to " + camelToWords(action);
        }
        
        return "should work correctly for " + camelToWords(methodName);
    }

    public String generateBehaviorVerification(MethodInfo method, String mockName) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("        verify(").append(mockName).append(").").append(method.name()).append("();\n");
        
        return sb.toString();
    }

    private boolean isNumericType(String type) {
        return type.equals("int") || type.equals("Integer") ||
               type.equals("long") || type.equals("Long") ||
               type.equals("double") || type.equals("Double") ||
               type.equals("float") || type.equals("Float") ||
               type.equals("short") || type.equals("Short") ||
               type.equals("byte") || type.equals("Byte");
    }

    private String camelToWords(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return "";
        }
        
        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(camelCase.charAt(0)));
        
        for (int i = 1; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append(' ');
                result.append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        
        return result.toString();
    }
}
