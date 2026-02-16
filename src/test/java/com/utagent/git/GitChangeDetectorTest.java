package com.utagent.git;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GitChangeDetector Tests")
class GitChangeDetectorTest {

    @TempDir
    Path tempDir;
    
    private File projectRoot;
    private GitChangeDetector detector;

    @BeforeEach
    void setUp() {
        projectRoot = tempDir.toFile();
        detector = new GitChangeDetector(projectRoot);
    }

    @Test
    @DisplayName("Should detect non-git repository")
    void shouldDetectNonGitRepository() {
        assertFalse(detector.isGitRepository());
    }

    @Test
    @DisplayName("Should return empty set for non-git repository when getting changed files")
    void shouldReturnEmptySetForNonGitRepository() {
        Set<File> changedFiles = detector.getChangedFiles("main");
        
        assertTrue(changedFiles.isEmpty());
    }

    @Test
    @DisplayName("Should return empty set for non-git repository when getting uncommitted changes")
    void shouldReturnEmptySetForUncommittedChangesNonGit() {
        Set<File> uncommittedChanges = detector.getUncommittedChanges();
        
        assertTrue(uncommittedChanges.isEmpty());
    }

    @Test
    @DisplayName("Should return empty set for non-git repository when getting staged changes")
    void shouldReturnEmptySetForStagedChangesNonGit() {
        Set<File> stagedChanges = detector.getStagedChanges();
        
        assertTrue(stagedChanges.isEmpty());
    }

    @Test
    @DisplayName("Should return unknown branch for non-git repository")
    void shouldReturnUnknownBranchForNonGitRepository() {
        String branch = detector.getCurrentBranch();
        
        assertEquals("unknown", branch);
    }

    @Test
    @DisplayName("Should return unknown commit hash for non-git repository")
    void shouldReturnUnknownCommitHashForNonGitRepository() {
        String hash = detector.getLastCommitHash();
        
        assertEquals("unknown", hash);
    }

    @Test
    @DisplayName("Should return empty message for non-git repository")
    void shouldReturnEmptyMessageForNonGitRepository() {
        String message = detector.getLastCommitMessage();
        
        assertEquals("", message);
    }

    @Test
    @DisplayName("Should return empty list for changed lines in non-git repository")
    void shouldReturnEmptyListForChangedLinesNonGit() throws IOException {
        File testFile = createTestFile("Test.java", "public class Test {}");
        
        List<Integer> changedLines = detector.getChangedLines(testFile, "main");
        
        assertTrue(changedLines.isEmpty());
    }

    @Test
    @DisplayName("Should detect git repository when .git directory exists")
    void shouldDetectGitRepositoryWhenGitDirExists() throws IOException {
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectories(gitDir);
        
        assertTrue(detector.isGitRepository());
    }

    @Test
    @DisplayName("ChangeInfo should store file and type correctly")
    void changeInfoShouldStoreFileAndTypeCorrectly() throws IOException {
        File testFile = createTestFile("Test.java", "public class Test {}");
        List<Integer> changedLines = List.of(1, 2, 3);
        
        GitChangeDetector.ChangeInfo changeInfo = new GitChangeDetector.ChangeInfo(
            testFile, 
            GitChangeDetector.ChangeType.MODIFIED, 
            changedLines
        );
        
        assertEquals(testFile, changeInfo.getFile());
        assertEquals(GitChangeDetector.ChangeType.MODIFIED, changeInfo.getType());
        assertEquals(3, changeInfo.getChangedLines().size());
    }

    @Test
    @DisplayName("ChangeType enum should have all expected values")
    void changeTypeEnumShouldHaveAllExpectedValues() {
        GitChangeDetector.ChangeType[] types = GitChangeDetector.ChangeType.values();
        
        assertEquals(4, types.length);
        assertTrue(java.util.Arrays.asList(types).contains(GitChangeDetector.ChangeType.ADDED));
        assertTrue(java.util.Arrays.asList(types).contains(GitChangeDetector.ChangeType.DELETED));
        assertTrue(java.util.Arrays.asList(types).contains(GitChangeDetector.ChangeType.MODIFIED));
        assertTrue(java.util.Arrays.asList(types).contains(GitChangeDetector.ChangeType.RENAMED));
    }

    @Test
    @DisplayName("Should handle default getChangedFiles without base branch")
    void shouldHandleDefaultGetChangedFiles() {
        Set<File> changedFiles = detector.getChangedFiles();
        
        assertTrue(changedFiles.isEmpty());
    }

    @Test
    @DisplayName("Should handle non-existent files gracefully")
    void shouldHandleNonExistentFilesGracefully() {
        File nonExistentFile = new File(projectRoot, "NonExistent.java");
        
        List<Integer> changedLines = detector.getChangedLines(nonExistentFile, "main");
        
        assertTrue(changedLines.isEmpty());
    }

    private File createTestFile(String name, String content) throws IOException {
        Path filePath = tempDir.resolve(name);
        Files.writeString(filePath, content);
        return filePath.toFile();
    }
}
