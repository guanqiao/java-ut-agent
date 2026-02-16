package com.utagent.ide;

import com.utagent.model.ClassInfo;
import com.utagent.model.MethodInfo;

public class IDERequest {
    private final ClassInfo targetClass;
    private final MethodInfo targetMethod;
    private final IDEOptions options;

    public IDERequest(ClassInfo targetClass, MethodInfo targetMethod, IDEOptions options) {
        this.targetClass = targetClass;
        this.targetMethod = targetMethod;
        this.options = options != null ? options : IDEOptions.builder().build();
    }

    public ClassInfo getTargetClass() {
        return targetClass;
    }

    public MethodInfo getTargetMethod() {
        return targetMethod;
    }

    public IDEOptions getOptions() {
        return options;
    }
}
