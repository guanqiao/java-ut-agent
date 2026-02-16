package com.utagent.team;

public class ConfigChange {
    private final String field;
    private final Object oldValue;
    private final Object newValue;

    public ConfigChange(String field, Object oldValue, Object newValue) {
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getField() {
        return field;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }
}
