package com.utagent.model;

import java.util.ArrayList;
import java.util.List;

public record CoverageReport(
    double overallLineCoverage,
    double overallBranchCoverage,
    double overallInstructionCoverage,
    List<CoverageInfo> classCoverages,
    List<Integer> uncoveredLines
) {
    public CoverageReport() {
        this(0.0, 0.0, 0.0, new ArrayList<>(), new ArrayList<>());
    }

    public boolean meetsTarget(double targetRate) {
        return overallLineCoverage >= targetRate && 
               overallBranchCoverage >= targetRate;
    }
}
