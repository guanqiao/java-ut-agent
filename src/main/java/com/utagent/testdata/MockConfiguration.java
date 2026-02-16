package com.utagent.testdata;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockConfiguration {

    private final Class<?> mockType;
    private String mockName;
    private boolean isSpy;
    private final List<String> methodsToMock;
    private final Map<String, String> stubbings;
    private final Map<String, Integer> verifications;

    public MockConfiguration(Class<?> mockType) {
        this.mockType = mockType;
        this.mockName = toCamelCase(mockType.getSimpleName());
        this.isSpy = false;
        this.methodsToMock = new ArrayList<>();
        this.stubbings = new HashMap<>();
        this.verifications = new HashMap<>();
        discoverMethods();
    }

    private String toCamelCase(String className) {
        if (className == null || className.isEmpty()) {
            return "mock";
        }
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    private void discoverMethods() {
        for (Method method : mockType.getDeclaredMethods()) {
            methodsToMock.add(method.getName());
        }
        for (Method method : mockType.getMethods()) {
            if (!methodsToMock.contains(method.getName()) && 
                !method.getDeclaringClass().equals(Object.class)) {
                methodsToMock.add(method.getName());
            }
        }
    }

    public Class<?> getMockType() {
        return mockType;
    }

    public String getMockName() {
        return mockName;
    }

    public void setMockName(String mockName) {
        this.mockName = mockName;
    }

    public boolean isSpy() {
        return isSpy;
    }

    public void setSpy(boolean spy) {
        isSpy = spy;
    }

    public List<String> getMethodsToMock() {
        return new ArrayList<>(methodsToMock);
    }

    public void addStubbing(String methodName, String argument, String returnValue) {
        stubbings.put(methodName, argument + " -> " + returnValue);
    }

    public void addStubbing(String methodName, String returnValue) {
        stubbings.put(methodName, returnValue);
    }

    public Map<String, String> getStubbings() {
        return new HashMap<>(stubbings);
    }

    public void addVerification(String methodName, int times) {
        verifications.put(methodName, times);
    }

    public Map<String, Integer> getVerifications() {
        return new HashMap<>(verifications);
    }
}
