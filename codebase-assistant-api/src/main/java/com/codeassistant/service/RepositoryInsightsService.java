package com.codeassistant.service;

import com.codeassistant.api.ResourceNotFoundException;
import com.codeassistant.domain.RepositoryEntity;
import com.codeassistant.domain.UserEntity;
import com.codeassistant.model.RepositoryDocsView;
import com.codeassistant.model.RepositorySummaryView;
import com.codeassistant.repository.RepositoryEntityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RepositoryInsightsService {

    private final RepositoryEntityRepository repositoryRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ChatClient chatClient;

    public RepositorySummaryView generateSummary(UUID repositoryId, UserEntity currentUser) {
        RepositoryEntity repo = getOwnedRepository(repositoryId, currentUser);
        List<String> languages = fetchTopLanguages(repo.getRepoUrl());
        int javaCount = countByLanguage(repo.getRepoUrl(), "java");
        int tsCount = countByLanguage(repo.getRepoUrl(), "typescript");
        int jsCount = countByLanguage(repo.getRepoUrl(), "javascript");
        int mdCount = countByLanguage(repo.getRepoUrl(), "markdown");

        List<String> frameworks = detectFrameworks(repo.getRepoUrl());
        String architecture = inferArchitecture(frameworks, javaCount, tsCount, jsCount);

        return RepositorySummaryView.builder()
                .repositoryName(repo.getName())
                .repoUrl(repo.getRepoUrl())
                .architectureType(architecture)
                .detectedFrameworks(frameworks)
                .moduleStructure(fetchTopDirectories(repo.getRepoUrl()))
                .apiLayers(detectApiLayers(repo.getRepoUrl()))
                .databaseLayers(detectDatabaseLayers(repo.getRepoUrl()))
                .externalIntegrations(detectIntegrations(repo.getRepoUrl(), mdCount))
                .build();
    }

    public RepositoryDocsView generateDocs(UUID repositoryId, UserEntity currentUser) {
        RepositorySummaryView summary = generateSummary(repositoryId, currentUser);

        String basePrompt = """
                You are generating concise engineering documentation.
                Use the provided repository metadata only.
                Keep each section practical and brief.
                """;

        String metadata = "Repository: " + summary.getRepositoryName() + "\n"
                + "Architecture: " + summary.getArchitectureType() + "\n"
                + "Frameworks: " + String.join(", ", summary.getDetectedFrameworks()) + "\n"
                + "Modules: " + String.join(", ", summary.getModuleStructure()) + "\n"
                + "API Layers: " + String.join(", ", summary.getApiLayers()) + "\n"
                + "Database Layers: " + String.join(", ", summary.getDatabaseLayers()) + "\n"
                + "Integrations: " + String.join(", ", summary.getExternalIntegrations());

        java.util.concurrent.CompletableFuture<String> readmeFuture = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                ask(basePrompt, "Generate a README summary section.\n" + metadata));

        java.util.concurrent.CompletableFuture<String> onboardingFuture = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                ask(basePrompt, "Generate a new engineer onboarding guide.\n" + metadata));

        java.util.concurrent.CompletableFuture<String> architectureFuture = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                ask(basePrompt, "Generate an architecture overview.\n" + metadata));

        java.util.concurrent.CompletableFuture<String> apiFuture = java.util.concurrent.CompletableFuture.supplyAsync(() ->
                ask(basePrompt, "Generate an API summary.\n" + metadata));

        String readme = readmeFuture.join();
        String onboarding = onboardingFuture.join();
        String architecture = architectureFuture.join();
        String api = apiFuture.join();

        return RepositoryDocsView.builder()
                .readmeSummary(readme)
                .onboardingGuide(onboarding)
                .architectureSummary(architecture)
                .apiSummary(api)
                .build();
    }



    private RepositoryEntity getOwnedRepository(UUID repositoryId, UserEntity currentUser) {
        RepositoryEntity repo = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found: " + repositoryId));
        if (currentUser != null && repo.getUser() != null && !repo.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Repository not found: " + repositoryId);
        }
        return repo;
    }

    private List<String> fetchTopLanguages(String repoUrl) {
        String sql = """
                SELECT metadata->>'language' AS language, COUNT(*) AS cnt
                FROM vector_store
                WHERE metadata->>'repo_url' = ?
                GROUP BY metadata->>'language'
                ORDER BY cnt DESC
                LIMIT 6
                """;
        return jdbcTemplate.queryForList(sql, repoUrl).stream()
                .map(m -> String.valueOf(m.get("language")))
                .filter(s -> s != null && !s.equals("null") && !s.isBlank())
                .toList();
    }

    private int countByLanguage(String repoUrl, String language) {
        String sql = """
                SELECT COUNT(*)
                FROM vector_store
                WHERE metadata->>'repo_url' = ?
                  AND metadata->>'language' = ?
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, repoUrl, language);
        return count == null ? 0 : count;
    }

    private List<String> fetchTopDirectories(String repoUrl) {
        String sql = """
                SELECT split_part(metadata->>'file_path', '/', 1) AS root, COUNT(*) AS cnt
                FROM vector_store
                WHERE metadata->>'repo_url' = ?
                GROUP BY split_part(metadata->>'file_path', '/', 1)
                ORDER BY cnt DESC
                LIMIT 8
                """;
        return jdbcTemplate.queryForList(sql, repoUrl).stream()
                .map(m -> String.valueOf(m.get("root")))
                .filter(s -> s != null && !s.equals("null") && !s.isBlank())
                .toList();
    }

    private List<String> detectFrameworks(String repoUrl) {
        List<String> signals = new ArrayList<>();
        if (existsPathLike(repoUrl, "%pom.xml%")) signals.add("Spring Boot / Maven");
        if (existsPathLike(repoUrl, "%build.gradle%")) signals.add("Gradle");
        if (existsPathLike(repoUrl, "%package.json%")) signals.add("Node.js");
        if (existsPathLike(repoUrl, "%vite.config%")) signals.add("Vite");
        if (existsPathLike(repoUrl, "%tailwind.config%")) signals.add("Tailwind CSS");
        if (existsPathLike(repoUrl, "%docker-compose%")) signals.add("Docker Compose");
        return signals.stream().distinct().toList();
    }

    private List<String> detectApiLayers(String repoUrl) {
        return detectByPathKeywords(repoUrl, List.of("controller", "api", "route", "handler"));
    }

    private List<String> detectDatabaseLayers(String repoUrl) {
        return detectByPathKeywords(repoUrl, List.of("repository", "dao", "migration", "entity", "model"));
    }

    private List<String> detectIntegrations(String repoUrl, int markdownCount) {
        List<String> res = new ArrayList<>();
        if (existsContentLike(repoUrl, "%openai%")) res.add("OpenAI");
        if (existsContentLike(repoUrl, "%postgres%")) res.add("PostgreSQL");
        if (existsContentLike(repoUrl, "%pgvector%")) res.add("PGVector");
        if (existsContentLike(repoUrl, "%github%")) res.add("GitHub");
        if (markdownCount > 0) res.add("Documentation files");
        return res.stream().distinct().toList();
    }

    private List<String> detectByPathKeywords(String repoUrl, List<String> keywords) {
        List<String> matched = new ArrayList<>();
        for (String keyword : keywords) {
            String sql = """
                    SELECT COUNT(*)
                    FROM vector_store
                    WHERE metadata->>'repo_url' = ?
                      AND LOWER(metadata->>'file_path') LIKE ?
                    """;
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, repoUrl, "%" + keyword + "%");
            if (count != null && count > 0) {
                matched.add(keyword);
            }
        }
        return matched;
    }

    private boolean existsPathLike(String repoUrl, String likePattern) {
        String sql = """
                SELECT COUNT(*)
                FROM vector_store
                WHERE metadata->>'repo_url' = ?
                  AND LOWER(metadata->>'file_path') LIKE LOWER(?)
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, repoUrl, likePattern);
        return count != null && count > 0;
    }

    private boolean existsContentLike(String repoUrl, String likePattern) {
        String sql = """
                SELECT COUNT(*)
                FROM vector_store
                WHERE metadata->>'repo_url' = ?
                  AND LOWER(content) LIKE LOWER(?)
                """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, repoUrl, likePattern);
        return count != null && count > 0;
    }

    private String inferArchitecture(List<String> frameworks, int javaCount, int tsCount, int jsCount) {
        boolean hasBackend = javaCount > 0;
        boolean hasFrontend = tsCount + jsCount > 0;
        if (hasBackend && hasFrontend) return "Full-stack layered architecture";
        if (hasBackend) return "Backend service architecture";
        if (hasFrontend) return "Frontend application architecture";
        if (!frameworks.isEmpty()) return "Framework-driven modular architecture";
        return "General multi-module codebase architecture";
    }

    private String ask(String systemPrompt, String userPrompt) {
        try {
            return chatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception e) {
            return "Could not generate section right now: " + e.getMessage();
        }
    }
}

