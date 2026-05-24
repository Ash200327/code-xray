package com.codeassistant.ingestion;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileFilterServiceTest {

    @Test
    void shouldExcludeNonAcceptedExtension() throws Exception {
        IngestionProperties properties = defaultProperties();
        FileFilterService service = new FileFilterService(properties);

        Path repo = Files.createTempDirectory("repo");
        Path file = repo.resolve("README.txt");
        Files.writeString(file, "hello");
        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);

        boolean included = service.shouldIncludeFile(repo, file, attrs, List.of());
        assertFalse(included);
    }

    @Test
    void shouldExcludeByGlobRule() throws Exception {
        IngestionProperties properties = defaultProperties();
        FileFilterService service = new FileFilterService(properties);

        Path repo = Files.createTempDirectory("repo");
        Path file = repo.resolve("app.min.js");
        Files.writeString(file, "console.log('x');");
        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);

        boolean included = service.shouldIncludeFile(repo, file, attrs, List.of());
        assertFalse(included);
    }

    @Test
    void shouldRespectGitIgnoreWithNegation() throws Exception {
        IngestionProperties properties = defaultProperties();
        FileFilterService service = new FileFilterService(properties);

        Path repo = Files.createTempDirectory("repo");
        Files.writeString(repo.resolve(".gitignore"), """
                docs/**
                !docs/keep.md
                """);

        Path ignoredFile = repo.resolve("docs").resolve("skip.md");
        Files.createDirectories(ignoredFile.getParent());
        Files.writeString(ignoredFile, "ignore me");

        Path keptFile = repo.resolve("docs").resolve("keep.md");
        Files.writeString(keptFile, "keep me");

        List<FileFilterService.GitIgnoreRule> rules = service.loadGitIgnoreRules(repo);
        BasicFileAttributes ignoredAttrs = Files.readAttributes(ignoredFile, BasicFileAttributes.class);
        BasicFileAttributes keptAttrs = Files.readAttributes(keptFile, BasicFileAttributes.class);

        boolean ignoredIncluded = service.shouldIncludeFile(repo, ignoredFile, ignoredAttrs, rules);
        boolean keptIncluded = service.shouldIncludeFile(repo, keptFile, keptAttrs, rules);

        assertFalse(ignoredIncluded);
        assertTrue(keptIncluded);
    }

    private IngestionProperties defaultProperties() {
        IngestionProperties properties = new IngestionProperties();
        properties.setAcceptedExtensions(Set.of("java", "md", "js"));
        properties.setSkippedDirectories(Set.of(".git", "node_modules", "dist", "build"));
        properties.setExcludedGlobs(List.of("**/*.min.js", "**/generated/**"));
        properties.setMaxFileSizeBytes(500 * 1024);
        return properties;
    }
}

