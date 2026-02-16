package com.utagent.quality;

public record TestSmell(
    TestSmellType type,
    TestSmellSeverity severity,
    int lineNumber,
    String message,
    String suggestion
) {
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private TestSmellType type;
        private TestSmellSeverity severity;
        private int lineNumber;
        private String message;
        private String suggestion;

        public Builder type(TestSmellType type) {
            this.type = type;
            return this;
        }

        public Builder severity(TestSmellSeverity severity) {
            this.severity = severity;
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

        public Builder suggestion(String suggestion) {
            this.suggestion = suggestion;
            return this;
        }

        public TestSmell build() {
            return new TestSmell(type, severity, lineNumber, message, suggestion);
        }
    }
}
