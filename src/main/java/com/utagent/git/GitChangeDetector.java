package com.utagent.git;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GitChangeDetector {

    private static final Logger logger = LoggerFactory.getLogger(GitChangeDetector.class);
    
    private final File projectRoot;
    
    public GitChangeDetector(File projectRoot) {
        this.projectRoot = projectRoot;
    }
    
    public boolean isGitRepository() {
        File gitDir = new File(projectRoot, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }
    
    public Set<File> getChangedFiles(String baseBranch) {
        Set<File> changedFiles = new HashSet<>();
        
        if (!isGitRepository()) {
            logger.warn("Not a git repository: {}", projectRoot.getAbsolutePath());
            return changedFiles;
        }
        
        try {
            String diffOutput = executeGitCommand("git diff --name-only " + baseBranch);
            for (String line : diffOutput.split("\n")) {
                if (line.endsWith(".java") && !line.endsWith("Test.java")) {
                    File file = new File(projectRoot, line.trim());
                    if (file.exists()) {
                        changedFiles.add(file);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get changed files: {}", e.getMessage());
        }
        
        return changedFiles;
    }
    
    public Set<File> getChangedFiles() {
        return getChangedFiles("HEAD~1");
    }
    
    public Set<File> getUncommittedChanges() {
        Set<File> changedFiles = new HashSet<>();
        
        if (!isGitRepository()) {
            return changedFiles;
        }
        
        try {
            String statusOutput = executeGitCommand("git status --porcelain");
            for (String line : statusOutput.split("\n")) {
                if (line.isEmpty()) continue;
                
                String status = line.substring(0, 2).trim();
                String filePath = line.substring(3).trim();
                
                if (filePath.endsWith(".java") && !filePath.endsWith("Test.java")) {
                    File file = new File(projectRoot, filePath);
                    if (file.exists()) {
                        changedFiles.add(file);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get uncommitted changes: {}", e.getMessage());
        }
        
        return changedFiles;
    }
    
    public Set<File> getStagedChanges() {
        Set<File> changedFiles = new HashSet<>();
        
        if (!isGitRepository()) {
            return changedFiles;
        }
        
        try {
            String diffOutput = executeGitCommand("git diff --cached --name-only");
            for (String line : diffOutput.split("\n")) {
                if (line.endsWith(".java") && !line.endsWith("Test.java")) {
                    File file = new File(projectRoot, line.trim());
                    if (file.exists()) {
                        changedFiles.add(file);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get staged changes: {}", e.getMessage());
        }
        
        return changedFiles;
    }
    
    public List<Integer> getChangedLines(File file, String baseBranch) {
        List<Integer> changedLines = new ArrayList<>();
        
        if (!isGitRepository()) {
            return changedLines;
        }
        
        try {
            String relativePath = getRelativePath(file);
            String diffOutput = executeGitCommand(
                "git diff " + baseBranch + " -- " + relativePath);
            
            Pattern hunkPattern = Pattern.compile("@@ -\\d+,?\\d* \\+(\\d+),?(\\d*) @@");
            Matcher matcher = hunkPattern.matcher(diffOutput);
            
            while (matcher.find()) {
                int startLine = Integer.parseInt(matcher.group(1));
                int lineCount = matcher.group(2).isEmpty() ? 1 : Integer.parseInt(matcher.group(2));
                
                for (int i = 0; i < lineCount; i++) {
                    changedLines.add(startLine + i);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to get changed lines for {}: {}", file.getName(), e.getMessage());
        }
        
        return changedLines;
    }
    
    public String getCurrentBranch() {
        if (!isGitRepository()) {
            return "unknown";
        }
        
        try {
            return executeGitCommand("git rev-parse --abbrev-ref HEAD").trim();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    public String getLastCommitHash() {
        if (!isGitRepository()) {
            return "unknown";
        }
        
        try {
            return executeGitCommand("git rev-parse HEAD").trim();
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    public String getLastCommitMessage() {
        if (!isGitRepository()) {
            return "";
        }
        
        try {
            return executeGitCommand("git log -1 --pretty=%B").trim();
        } catch (Exception e) {
            return "";
        }
    }
    
    private String getRelativePath(File file) {
        Path projectPath = projectRoot.toPath().toAbsolutePath();
        Path filePath = file.toPath().toAbsolutePath();
        return projectPath.relativize(filePath).toString().replace('\\', '/');
    }
    
    private String executeGitCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder pb;
        
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            pb = new ProcessBuilder("cmd", "/c", command);
        } else {
            pb = new ProcessBuilder("sh", "-c", command);
        }
        
        pb.directory(projectRoot);
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.warn("Git command exited with code {}: {}", exitCode, command);
        }
        
        return output.toString();
    }
    
    public static class ChangeInfo {
        private final File file;
        private final ChangeType type;
        private final List<Integer> changedLines;
        
        public ChangeInfo(File file, ChangeType type, List<Integer> changedLines) {
            this.file = file;
            this.type = type;
            this.changedLines = List.copyOf(changedLines != null ? changedLines : List.of());
        }
        
        public File getFile() {
            return file;
        }
        
        public ChangeType getType() {
            return type;
        }
        
        public List<Integer> getChangedLines() {
            return changedLines;
        }
    }
    
    public enum ChangeType {
        ADDED,
        MODIFIED,
        DELETED,
        RENAMED
    }
}
