package com.utagent.config;

public record OutputConfig(
    String directory,
    String format,
    Boolean verbose,
    Boolean colorOutput,
    Boolean showProgress
) {
    public static final String DEFAULT_DIRECTORY = "src/test/java";
    public static final String FORMAT_STANDARD = "standard";
    public static final String FORMAT_COMPACT = "compact";
    
    public static OutputConfig defaults() {
        return new OutputConfig(
            DEFAULT_DIRECTORY,
            FORMAT_STANDARD,
            false,
            true,
            true
        );
    }
    
    public String getDirectoryOrDefault() {
        return directory != null ? directory : DEFAULT_DIRECTORY;
    }
    
    public String getFormatOrDefault() {
        return format != null ? format : FORMAT_STANDARD;
    }
    
    public boolean getVerboseOrDefault() {
        return verbose != null ? verbose : false;
    }
    
    public boolean getColorOutputOrDefault() {
        return colorOutput != null ? colorOutput : true;
    }
    
    public boolean getShowProgressOrDefault() {
        return showProgress != null ? showProgress : true;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String directory;
        private String format;
        private Boolean verbose;
        private Boolean colorOutput;
        private Boolean showProgress;
        
        public Builder directory(String directory) {
            this.directory = directory;
            return this;
        }
        
        public Builder format(String format) {
            this.format = format;
            return this;
        }
        
        public Builder verbose(Boolean verbose) {
            this.verbose = verbose;
            return this;
        }
        
        public Builder colorOutput(Boolean colorOutput) {
            this.colorOutput = colorOutput;
            return this;
        }
        
        public Builder showProgress(Boolean showProgress) {
            this.showProgress = showProgress;
            return this;
        }
        
        public OutputConfig build() {
            return new OutputConfig(directory, format, verbose, colorOutput, showProgress);
        }
    }
}
