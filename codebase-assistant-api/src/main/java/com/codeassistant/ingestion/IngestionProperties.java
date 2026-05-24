package com.codeassistant.ingestion;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "codeassistant.ingestion")
public class IngestionProperties {

    private long maxFileSizeBytes = 500 * 1024;

    private Set<String> skippedDirectories = Set.of(
            ".git", "node_modules", "__pycache__", "target", "build",
            ".gradle", ".idea", ".vscode", "dist", "out", ".mvn"
    );

    private Set<String> acceptedExtensions = Set.of(
            "java", "kt", "py", "ts", "tsx", "js", "jsx",
            "go", "cs", "rs", "cpp", "c", "h",
            "md", "yaml", "yml", "xml", "sql", "sh", "json"
    );

    private List<String> excludedGlobs = List.of(
            "**/*.min.js",
            "**/*.map",
            "**/*.class",
            "**/*.jar",
            "**/*.war",
            "**/*.ear",
            "**/*.exe",
            "**/*.dll",
            "**/*.so",
            "**/*.dylib",
            "**/generated/**"
    );

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public Set<String> getSkippedDirectories() {
        return skippedDirectories;
    }

    public void setSkippedDirectories(Set<String> skippedDirectories) {
        this.skippedDirectories = skippedDirectories;
    }

    public Set<String> getAcceptedExtensions() {
        return acceptedExtensions;
    }

    public void setAcceptedExtensions(Set<String> acceptedExtensions) {
        this.acceptedExtensions = acceptedExtensions;
    }

    public List<String> getExcludedGlobs() {
        return excludedGlobs;
    }

    public void setExcludedGlobs(List<String> excludedGlobs) {
        this.excludedGlobs = excludedGlobs;
    }
}

