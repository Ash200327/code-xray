package com.codeassistant.ingestion;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileFilterService {

    private final IngestionProperties ingestionProperties;

    public boolean shouldSkipDirectory(Path dir) {
        String dirName = dir.getFileName().toString();
        return ingestionProperties.getSkippedDirectories().contains(dirName);
    }

    public boolean shouldIncludeFile(Path repoRoot, Path file, BasicFileAttributes attrs, List<GitIgnoreRule> gitIgnoreRules) {
        if (!hasAcceptedExtension(file)) {
            return false;
        }
        if (attrs.size() > ingestionProperties.getMaxFileSizeBytes()) {
            return false;
        }
        Path relative = repoRoot.relativize(file);
        if (matchesExcludedGlobs(relative)) {
            return false;
        }
        return !isIgnoredByGitIgnore(relative, gitIgnoreRules);
    }

    public List<GitIgnoreRule> loadGitIgnoreRules(Path repoRoot) {
        Path gitIgnore = repoRoot.resolve(".gitignore");
        if (!Files.exists(gitIgnore)) {
            return List.of();
        }

        try {
            List<GitIgnoreRule> rules = new ArrayList<>();
            for (String rawLine : Files.readAllLines(gitIgnore, StandardCharsets.UTF_8)) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                boolean negated = line.startsWith("!");
                String pattern = negated ? line.substring(1).trim() : line;
                if (pattern.isEmpty()) {
                    continue;
                }

                rules.add(new GitIgnoreRule(pattern, negated));
            }
            return rules;
        } catch (IOException e) {
            log.warn("Could not read .gitignore from {}: {}", repoRoot, e.getMessage());
            return List.of();
        }
    }

    private boolean hasAcceptedExtension(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String ext = dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
        return ingestionProperties.getAcceptedExtensions().contains(ext);
    }

    private boolean matchesExcludedGlobs(Path relative) {
        String normalized = normalizePath(relative);
        for (String glob : ingestionProperties.getExcludedGlobs()) {
            if (matchesGlob(glob, relative, normalized)) {
                return true;
            }
            if (glob.startsWith("**/") && matchesGlob(glob.substring(3), relative, normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesGlob(String glob, Path relative, String normalized) {
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + glob);
        return matcher.matches(relative) || matcher.matches(Path.of(normalized));
    }

    private boolean isIgnoredByGitIgnore(Path relative, List<GitIgnoreRule> rules) {
        String normalized = normalizePath(relative);
        boolean ignored = false;

        for (GitIgnoreRule rule : rules) {
            if (rule.matches(normalized)) {
                ignored = !rule.negated();
            }
        }

        return ignored;
    }

    private String normalizePath(Path relative) {
        return relative.toString().replace("\\", "/");
    }

    public record GitIgnoreRule(String pattern, boolean negated) {
        boolean matches(String relativePath) {
            String normalizedPattern = pattern;
            if (normalizedPattern.startsWith("/")) {
                normalizedPattern = normalizedPattern.substring(1);
            }
            if (normalizedPattern.endsWith("/")) {
                normalizedPattern = normalizedPattern + "**";
            }

            List<String> candidates = new ArrayList<>();
            candidates.add("glob:" + normalizedPattern);
            candidates.add("glob:**/" + normalizedPattern);

            for (String candidate : candidates) {
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher(candidate);
                if (matcher.matches(Path.of(relativePath))) {
                    return true;
                }
            }
            return false;
        }
    }
}
