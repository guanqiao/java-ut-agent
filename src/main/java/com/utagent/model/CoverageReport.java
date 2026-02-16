package com.utagent.model;

import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * Returns an unmodifiable list of class coverages.
     */
    public List<CoverageInfo> classCoverages() {
        return Collections.unmodifiableList(classCoverages);
    }

    /**
     * Returns an unmodifiable list of uncovered lines.
     */
    public List<Integer> uncoveredLines() {
        return Collections.unmodifiableList(uncoveredLines);
    }
}
