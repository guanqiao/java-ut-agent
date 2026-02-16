package com.utagent.testdata;

import com.utagent.model.MethodInfo;
import com.utagent.model.ParameterInfo;

public class ParameterizedTestGenerator {

    public ParameterizedTestCase generate(MethodInfo method) {
        return new ParameterizedTestCase(method);
    }
}
