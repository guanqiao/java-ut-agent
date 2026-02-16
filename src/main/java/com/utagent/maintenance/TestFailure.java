package com.utagent.maintenance;

public record TestFailure(
    FailureType type,
    FailureSeverity severity,
    String testClass,
    String testMethod,
    int lineNumber,
    String message,
    String stackTrace
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private FailureType type;
        private FailureSeverity severity;
        private String testClass;
        private String testMethod;
        private int lineNumber;
        private String message;
        private String stackTrace;

        public Builder type(FailureType type) {
            this.type = type;
            return this;
        }

        public Builder severity(FailureSeverity severity) {
            this.severity = severity;
            return this;
        }

        public Builder testClass(String testClass) {
            this.testClass = testClass;
            return this;
        }

        public Builder testMethod(String testMethod) {
            this.testMethod = testMethod;
            return this;
        }

        public Builder lineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder stackTrace(String stackTrace) {
            this.stackTrace = stackTrace;
            return this;
        }

        public TestFailure build() {
            return new TestFailure(type, severity, testClass, testMethod, lineNumber, message, stackTrace);
        }
    }
}
