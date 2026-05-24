package com.codeassistant.ingestion;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileWalker {

    private final FileFilterService fileFilterService;

    public List<Path> walk(Path repoRoot) throws IOException {
        List<Path> files = new ArrayList<>();
        List<FileFilterService.GitIgnoreRule> gitIgnoreRules = fileFilterService.loadGitIgnoreRules(repoRoot);

        Files.walkFileTree(repoRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                if (!dir.equals(repoRoot) && fileFilterService.shouldSkipDirectory(dir)) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (!fileFilterService.shouldIncludeFile(repoRoot, file, attrs, gitIgnoreRules)) {
                    return FileVisitResult.CONTINUE;
                }
                files.add(file);
                return FileVisitResult.CONTINUE;
            }
        });

        return files;
    }

    public String detectLanguage(String filename) {
        String ext = extension(filename);
        return switch (ext) {
            case "java" -> "java";
            case "kt" -> "kotlin";
            case "py" -> "python";
            case "ts", "tsx" -> "typescript";
            case "js", "jsx" -> "javascript";
            case "go" -> "go";
            case "cs" -> "csharp";
            case "rs" -> "rust";
            case "cpp", "c", "h" -> "cpp";
            case "md" -> "markdown";
            case "yaml", "yml" -> "yaml";
            case "xml" -> "xml";
            case "sql" -> "sql";
            case "sh" -> "bash";
            case "json" -> "json";
            default -> "text";
        };
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }
}
