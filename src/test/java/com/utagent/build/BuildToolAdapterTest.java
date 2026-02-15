package com.utagent.build;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BuildToolAdapter Tests")
class BuildToolAdapterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("MavenAdapter should detect Maven project")
    void mavenAdapterShouldDetectMavenProject() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        Files.createFile(new File(projectRoot, "pom.xml").toPath());

        MavenAdapter adapter = new MavenAdapter();

        // When & Then
        assertTrue(adapter.detect(projectRoot));
        assertEquals("maven", adapter.name());
    }

    @Test
    @DisplayName("MavenAdapter should not detect non-Maven project")
    void mavenAdapterShouldNotDetectNonMavenProject() {
        // Given
        File projectRoot = tempDir.toFile();
        MavenAdapter adapter = new MavenAdapter();

        // When & Then
        assertFalse(adapter.detect(projectRoot));
    }

    @Test
    @DisplayName("MavenAdapter should return correct paths")
    void mavenAdapterShouldReturnCorrectPaths() {
        // Given
        File projectRoot = tempDir.toFile();
        MavenAdapter adapter = new MavenAdapter();

        // When & Then
        assertEquals("mvn test -q", adapter.getTestCommand());
        assertEquals("mvn test jacoco:report -q", adapter.getCoverageCommand());
        assertEquals("mvn compile -q", adapter.getCompileCommand());
        assertEquals(new File(projectRoot, "target/classes"), adapter.getClassesDirectory(projectRoot));
        assertEquals(new File(projectRoot, "target/test-classes"), adapter.getTestClassesDirectory(projectRoot));
        assertEquals(new File(projectRoot, "target/site/jacoco/jacoco.xml"), adapter.getCoverageReportFile(projectRoot));
        assertEquals(new File(projectRoot, "target/jacoco.exec"), adapter.getCoverageExecFile(projectRoot));
        assertEquals(new File(projectRoot, "src/main/java"), adapter.getSourceDirectory(projectRoot));
        assertEquals(new File(projectRoot, "src/test/java"), adapter.getTestSourceDirectory(projectRoot));
    }

    @Test
    @DisplayName("MavenAdapter should detect multi-module project")
    void mavenAdapterShouldDetectMultiModuleProject() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        Files.createFile(new File(projectRoot, "pom.xml").toPath());

        File subModule = new File(projectRoot, "submodule");
        subModule.mkdirs();
        Files.createFile(new File(subModule, "pom.xml").toPath());

        MavenAdapter adapter = new MavenAdapter();

        // When & Then
        assertTrue(adapter.isMultiModule(projectRoot));
        assertEquals(1, adapter.getModules(projectRoot).size());
        assertTrue(adapter.getModules(projectRoot).contains(subModule));
    }

    @Test
    @DisplayName("MavenAdapter should return single module for non-multi-module project")
    void mavenAdapterShouldReturnSingleModuleForNonMultiModuleProject() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        Files.createFile(new File(projectRoot, "pom.xml").toPath());

        MavenAdapter adapter = new MavenAdapter();

        // When & Then
        assertFalse(adapter.isMultiModule(projectRoot));
        assertEquals(1, adapter.getModules(projectRoot).size());
        assertTrue(adapter.getModules(projectRoot).contains(projectRoot));
    }

    @Test
    @DisplayName("GradleAdapter should detect Gradle project")
    void gradleAdapterShouldDetectGradleProject() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        Files.createFile(new File(projectRoot, "build.gradle").toPath());

        GradleAdapter adapter = new GradleAdapter();

        // When & Then
        assertTrue(adapter.detect(projectRoot));
        assertEquals("gradle", adapter.name());
    }

    @Test
    @DisplayName("GradleAdapter should not detect project with only Kotlin DSL")
    void gradleAdapterShouldNotDetectProjectWithOnlyKotlinDsl() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        Files.createFile(new File(projectRoot, "build.gradle.kts").toPath());

        GradleAdapter adapter = new GradleAdapter();

        // When & Then
        assertFalse(adapter.detect(projectRoot));
    }

    @Test
    @DisplayName("GradleAdapter should return correct paths")
    void gradleAdapterShouldReturnCorrectPaths() {
        // Given
        File projectRoot = tempDir.toFile();
        GradleAdapter adapter = new GradleAdapter();

        // When & Then
        assertEquals(new File(projectRoot, "build/classes/java/main"), adapter.getClassesDirectory(projectRoot));
        assertEquals(new File(projectRoot, "build/classes/java/test"), adapter.getTestClassesDirectory(projectRoot));
        assertEquals(new File(projectRoot, "build/reports/jacoco/test/jacocoTestReport.xml"), adapter.getCoverageReportFile(projectRoot));
        assertEquals(new File(projectRoot, "build/jacoco/test.exec"), adapter.getCoverageExecFile(projectRoot));
        assertEquals(new File(projectRoot, "src/main/java"), adapter.getSourceDirectory(projectRoot));
        assertEquals(new File(projectRoot, "src/test/java"), adapter.getTestSourceDirectory(projectRoot));
    }

    @Test
    @DisplayName("GradleAdapter should detect multi-module project")
    void gradleAdapterShouldDetectMultiModuleProject() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        Files.createFile(new File(projectRoot, "settings.gradle").toPath());

        File subModule = new File(projectRoot, "submodule");
        subModule.mkdirs();
        Files.createFile(new File(subModule, "build.gradle").toPath());

        GradleAdapter adapter = new GradleAdapter();

        // When & Then
        assertTrue(adapter.isMultiModule(projectRoot));
    }

    @Test
    @DisplayName("GradleKotlinAdapter should detect Gradle Kotlin project")
    void gradleKotlinAdapterShouldDetectGradleKotlinProject() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        Files.createFile(new File(projectRoot, "build.gradle.kts").toPath());

        GradleKotlinAdapter adapter = new GradleKotlinAdapter();

        // When & Then
        assertTrue(adapter.detect(projectRoot));
        assertEquals("gradle-kotlin", adapter.name());
    }

    @Test
    @DisplayName("GradleKotlinAdapter should not detect Groovy Gradle project")
    void gradleKotlinAdapterShouldNotDetectGroovyGradleProject() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        Files.createFile(new File(projectRoot, "build.gradle").toPath());

        GradleKotlinAdapter adapter = new GradleKotlinAdapter();

        // When & Then
        assertFalse(adapter.detect(projectRoot));
    }

    @Test
    @DisplayName("BuildToolDetector should detect Maven project")
    void buildToolDetectorShouldDetectMavenProject() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        Files.createFile(new File(projectRoot, "pom.xml").toPath());

        // When
        var result = BuildToolDetector.detect(projectRoot);

        // Then
        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof MavenAdapter);
        assertEquals(BuildToolType.MAVEN, BuildToolDetector.detectType(projectRoot));
    }

    @Test
    @DisplayName("BuildToolDetector should detect Gradle project")
    void buildToolDetectorShouldDetectGradleProject() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        Files.createFile(new File(projectRoot, "build.gradle").toPath());

        // When
        var result = BuildToolDetector.detect(projectRoot);

        // Then
        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof GradleAdapter);
        assertEquals(BuildToolType.GRADLE, BuildToolDetector.detectType(projectRoot));
    }

    @Test
    @DisplayName("BuildToolDetector should detect Gradle Kotlin project")
    void buildToolDetectorShouldDetectGradleKotlinProject() throws IOException {
        // Given
        File projectRoot = tempDir.toFile();
        Files.createFile(new File(projectRoot, "build.gradle.kts").toPath());

        // When
        var result = BuildToolDetector.detect(projectRoot);

        // Then
        assertTrue(result.isPresent());
        assertTrue(result.get() instanceof GradleKotlinAdapter);
        assertEquals(BuildToolType.GRADLE_KOTLIN, BuildToolDetector.detectType(projectRoot));
    }

    @Test
    @DisplayName("BuildToolDetector should return empty for unknown project")
    void buildToolDetectorShouldReturnEmptyForUnknownProject() {
        // Given
        File projectRoot = tempDir.toFile();

        // When
        var result = BuildToolDetector.detect(projectRoot);

        // Then
        assertTrue(result.isEmpty());
        assertEquals(BuildToolType.UNKNOWN, BuildToolDetector.detectType(projectRoot));
    }

    @Test
    @DisplayName("BuildToolDetector should return empty for null directory")
    void buildToolDetectorShouldReturnEmptyForNullDirectory() {
        // When
        var result = BuildToolDetector.detect(null);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("BuildToolDetector should return empty for file instead of directory")
    void buildToolDetectorShouldReturnEmptyForFileInsteadOfDirectory() throws IOException {
        // Given
        File file = new File(tempDir.toFile(), "somefile.txt");
        Files.createFile(file.toPath());

        // When
        var result = BuildToolDetector.detect(file);

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("BuildToolDetector should get correct adapter by type")
    void buildToolDetectorShouldGetCorrectAdapterByType() {
        // When & Then
        assertTrue(BuildToolDetector.getAdapter(BuildToolType.MAVEN) instanceof MavenAdapter);
        assertTrue(BuildToolDetector.getAdapter(BuildToolType.GRADLE) instanceof GradleAdapter);
        assertTrue(BuildToolDetector.getAdapter(BuildToolType.GRADLE_KOTLIN) instanceof GradleKotlinAdapter);
        assertTrue(BuildToolDetector.getAdapter(BuildToolType.UNKNOWN) instanceof MavenAdapter);
    }

    @Test
    @DisplayName("BuildToolDetector should find project roots recursively")
    void buildToolDetectorShouldFindProjectRootsRecursively() throws IOException {
        // Given
        File root = tempDir.toFile();

        File project1 = new File(root, "project1");
        project1.mkdirs();
        Files.createFile(new File(project1, "pom.xml").toPath());

        File nested = new File(root, "nested");
        nested.mkdirs();
        File project2 = new File(nested, "project2");
        project2.mkdirs();
        Files.createFile(new File(project2, "build.gradle").toPath());

        // When
        var roots = BuildToolDetector.findProjectRoots(root);

        // Then
        assertEquals(2, roots.size());
        assertTrue(roots.contains(project1));
        assertTrue(roots.contains(project2));
    }

    @Test
    @DisplayName("BuildToolType should return correct type from ID")
    void buildToolTypeShouldReturnCorrectTypeFromId() {
        // When & Then
        assertEquals(BuildToolType.MAVEN, BuildToolType.fromId("maven"));
        assertEquals(BuildToolType.GRADLE, BuildToolType.fromId("gradle"));
        assertEquals(BuildToolType.GRADLE_KOTLIN, BuildToolType.fromId("gradle-kotlin"));
        assertEquals(BuildToolType.UNKNOWN, BuildToolType.fromId("unknown"));
        assertEquals(BuildToolType.UNKNOWN, BuildToolType.fromId(null));
    }
}
