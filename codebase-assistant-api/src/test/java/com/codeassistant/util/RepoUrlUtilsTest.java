package com.codeassistant.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RepoUrlUtilsTest {

    @Test
    void testNormalizeRepoUrl() {
        assertNull(RepoUrlUtils.normalizeRepoUrl(null));
        assertNull(RepoUrlUtils.normalizeRepoUrl("   "));
        assertEquals("https://github.com/owner/repo", RepoUrlUtils.normalizeRepoUrl("https://github.com/owner/repo/"));
        assertEquals("https://github.com/owner/repo", RepoUrlUtils.normalizeRepoUrl("https://github.com/owner/repo.git"));
        assertEquals("https://github.com/owner/repo", RepoUrlUtils.normalizeRepoUrl(" https://github.com/owner/repo.git/ "));
    }

    @Test
    void testDeriveRepoName() {
        assertEquals("repo", RepoUrlUtils.deriveRepoName("https://github.com/owner/repo.git"));
        assertEquals("repo", RepoUrlUtils.deriveRepoName("https://github.com/owner/repo/"));
        assertEquals("repository", RepoUrlUtils.deriveRepoName(""));
        assertEquals("repository", RepoUrlUtils.deriveRepoName(null));
    }
}
