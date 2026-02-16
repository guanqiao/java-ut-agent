package com.utagent.testdata;

public record TestDataValue(
    Object value,
    String description,
    TestDataScenario scenario
) {
    public static TestDataValue of(Object value) {
        return new TestDataValue(value, "Generated test data", TestDataScenario.NORMAL);
    }

    public static TestDataValue of(Object value, String description) {
        return new TestDataValue(value, description, TestDataScenario.NORMAL);
    }

    public static TestDataValue of(Object value, TestDataScenario scenario) {
        return new TestDataValue(value, "Test data for " + scenario, scenario);
    }

    public static TestDataValue nullValue() {
        return new TestDataValue(null, "Null value", TestDataScenario.NULL);
    }

    public static TestDataValue empty(String description) {
        return new TestDataValue(null, description, TestDataScenario.EMPTY);
    }

    public boolean isNull() {
        return value == null;
    }

    public <T> T getValueAs(Class<T> type) {
        if (value == null) {
            return null;
        }
        return type.cast(value);
    }
}
