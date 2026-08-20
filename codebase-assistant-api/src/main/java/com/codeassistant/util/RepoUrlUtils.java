package com.codeassistant.util;

import java.util.Locale;

public final class RepoUrlUtils {

    private RepoUrlUtils() {
        // Utility class
    }

    /**
     * Normalizes a repository URL by trimming whitespace, stripping trailing slashes,
     * and removing '.git' suffixes.
     */
    public static String normalizeRepoUrl(String repoUrl) {
        if (repoUrl == null) {
            return null;
        }
        String normalized = repoUrl.trim();
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.toLowerCase(Locale.ROOT).endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }

    /**
     * Extracts a human-friendly repository name from a URL or returns a fallback.
     */
    public static String deriveRepoName(String repoUrl) {
        String normalized = normalizeRepoUrl(repoUrl);
        if (normalized == null || normalized.isBlank()) {
            return "repository";
        }
        String[] parts = normalized.split("/");
        return parts[parts.length - 1];
    }
}
