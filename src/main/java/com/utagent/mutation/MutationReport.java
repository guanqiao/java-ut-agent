package com.utagent.mutation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MutationReport {
    private final String targetClass;
    private final int totalMutants;
    private final int killedMutants;
    private final int survivedMutants;
    private final int timeoutMutants;
    private final int memoryErrorMutants;
    private final List<Mutant> mutants;

    private MutationReport(Builder builder) {
        this.targetClass = builder.targetClass;
        this.totalMutants = builder.totalMutants;
        this.killedMutants = builder.killedMutants;
        this.survivedMutants = builder.survivedMutants;
        this.timeoutMutants = builder.timeoutMutants;
        this.memoryErrorMutants = builder.memoryErrorMutants;
        this.mutants = Collections.unmodifiableList(new ArrayList<>(builder.mutants));
    }

    public String getTargetClass() { return targetClass; }
    public int getTotalMutants() { return totalMutants; }
    public int getKilledMutants() { return killedMutants; }
    public int getSurvivedMutants() { return survivedMutants; }
    public int getTimeoutMutants() { return timeoutMutants; }
    public int getMemoryErrorMutants() { return memoryErrorMutants; }
    public List<Mutant> getMutants() { return mutants; }

    public double getMutationScore() {
        if (totalMutants == 0) {
            return 0.0;
        }
        return (double) killedMutants / totalMutants;
    }

    public String getSummary() {
        return String.format(
            "Mutation Score: %.2f%%%n" +
            "Total Mutants: %d%n" +
            "Killed: %d%n" +
            "Survived: %d%n" +
            "Timed Out: %d%n" +
            "Memory Errors: %d%n",
            getMutationScore() * 100,
            totalMutants,
            killedMutants,
            survivedMutants,
            timeoutMutants,
            memoryErrorMutants
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String targetClass;
        private int totalMutants;
        private int killedMutants;
        private int survivedMutants;
        private int timeoutMutants;
        private int memoryErrorMutants;
        private List<Mutant> mutants = new ArrayList<>();

        public Builder targetClass(String targetClass) {
            this.targetClass = targetClass;
            return this;
        }

        public Builder totalMutants(int totalMutants) {
            this.totalMutants = totalMutants;
            return this;
        }

        public Builder killedMutants(int killedMutants) {
            this.killedMutants = killedMutants;
            return this;
        }

        public Builder survivedMutants(int survivedMutants) {
            this.survivedMutants = survivedMutants;
            return this;
        }

        public Builder timeoutMutants(int timeoutMutants) {
            this.timeoutMutants = timeoutMutants;
            return this;
        }

        public Builder memoryErrorMutants(int memoryErrorMutants) {
            this.memoryErrorMutants = memoryErrorMutants;
            return this;
        }

        public Builder mutants(List<Mutant> mutants) {
            this.mutants = mutants != null ? new ArrayList<>(mutants) : new ArrayList<>();
            return this;
        }

        public Builder addMutant(Mutant mutant) {
            this.mutants.add(mutant);
            return this;
        }

        public MutationReport build() {
            return new MutationReport(this);
        }
    }
}
