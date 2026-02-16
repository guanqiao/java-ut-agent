package com.utagent.mutation;

public final class Mutant {
    private final String id;
    private final String mutator;
    private final String method;
    private final int lineNumber;
    private final MutantStatus status;
    private final String description;

    private Mutant(Builder builder) {
        this.id = builder.id;
        this.mutator = builder.mutator;
        this.method = builder.method;
        this.lineNumber = builder.lineNumber;
        this.status = builder.status;
        this.description = builder.description;
    }

    public String id() { return id; }
    public String mutator() { return mutator; }
    public String method() { return method; }
    public int lineNumber() { return lineNumber; }
    public MutantStatus status() { return status; }
    public String description() { return description; }

    public boolean isKilled() {
        return status == MutantStatus.KILLED;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String id;
        private String mutator;
        private String method;
        private int lineNumber;
        private MutantStatus status;
        private String description;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder mutator(String mutator) {
            this.mutator = mutator;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder lineNumber(int lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public Builder status(MutantStatus status) {
            this.status = status;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Mutant build() {
            return new Mutant(this);
        }
    }
}
