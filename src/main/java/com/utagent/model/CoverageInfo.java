package com.utagent.model;

import java.util.ArrayList;
import java.util.List;

public record CoverageInfo(
    String className,
    String methodName,
    int lineNumber,
    int branchCount,
    int branchMissed,
    int instructionCount,
    int instructionMissed,
    int lineCount,
    int lineMissed
) {
    public double getLineCoverageRate() {
        if (lineCount == 0) return 1.0;
        return (double) (lineCount - lineMissed) / lineCount;
    }

    public double getBranchCoverageRate() {
        if (branchCount == 0) return 1.0;
        return (double) (branchCount - branchMissed) / branchCount;
    }

    public double getInstructionCoverageRate() {
        if (instructionCount == 0) return 1.0;
        return (double) (instructionCount - instructionMissed) / instructionCount;
    }
}
