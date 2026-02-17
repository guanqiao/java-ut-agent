package com.utagent.report;

public class TestReport {
    private final String title;
    private final CoverageData coverage;
    private final QualityMetrics quality;
    private final String generatedAt;

    private TestReport(Builder builder) {
        this.title = builder.title;
        this.coverage = builder.coverage;
        this.quality = builder.quality;
        this.generatedAt = java.time.LocalDateTime.now().toString();
    }

    public String getTitle() {
        return title;
    }

    public CoverageData getCoverage() {
        return coverage;
    }

    public QualityMetrics getQuality() {
        return quality;
    }

    public String getGeneratedAt() {
        return generatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String title;
        private CoverageData coverage;
        private QualityMetrics quality;

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder coverage(CoverageData coverage) {
            this.coverage = coverage;
            return this;
        }

        public Builder quality(QualityMetrics quality) {
            this.quality = quality;
            return this;
        }

        public TestReport build() {
            return new TestReport(this);
        }
    }
}
