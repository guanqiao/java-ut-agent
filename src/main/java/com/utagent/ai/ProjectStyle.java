package com.utagent.ai;

public class ProjectStyle {
    private final String namingConvention;
    private final boolean useAssertJ;
    private final boolean useNestedTests;
    private final boolean useParameterizedTests;

    private ProjectStyle(Builder builder) {
        this.namingConvention = builder.namingConvention;
        this.useAssertJ = builder.useAssertJ;
        this.useNestedTests = builder.useNestedTests;
        this.useParameterizedTests = builder.useParameterizedTests;
    }

    public String getNamingConvention() {
        return namingConvention;
    }

    public boolean isUseAssertJ() {
        return useAssertJ;
    }

    public boolean isUseNestedTests() {
        return useNestedTests;
    }

    public boolean isUseParameterizedTests() {
        return useParameterizedTests;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String namingConvention = "should{MethodName}";
        private boolean useAssertJ = true;
        private boolean useNestedTests = true;
        private boolean useParameterizedTests = true;

        public Builder namingConvention(String namingConvention) {
            this.namingConvention = namingConvention;
            return this;
        }

        public Builder useAssertJ(boolean useAssertJ) {
            this.useAssertJ = useAssertJ;
            return this;
        }

        public Builder useNestedTests(boolean useNestedTests) {
            this.useNestedTests = useNestedTests;
            return this;
        }

        public Builder useParameterizedTests(boolean useParameterizedTests) {
            this.useParameterizedTests = useParameterizedTests;
            return this;
        }

        public ProjectStyle build() {
            return new ProjectStyle(this);
        }
    }
}
