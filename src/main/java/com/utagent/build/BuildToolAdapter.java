package com.utagent.build;

import java.io.File;

public interface BuildToolAdapter {
    
    String name();
    
    boolean detect(File projectRoot);
    
    String getTestCommand();
    
    String getCoverageCommand();
    
    String getCompileCommand();
    
    File getClassesDirectory(File projectRoot);
    
    File getTestClassesDirectory(File projectRoot);
    
    File getCoverageReportFile(File projectRoot);
    
    File getCoverageExecFile(File projectRoot);
    
    File getSourceDirectory(File projectRoot);
    
    File getTestSourceDirectory(File projectRoot);
    
    default boolean isMultiModule(File projectRoot) {
        return false;
    }
    
    default java.util.List<File> getModules(File projectRoot) {
        return java.util.Collections.emptyList();
    }
}
