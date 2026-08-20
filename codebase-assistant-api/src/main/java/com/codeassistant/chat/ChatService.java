package com.codeassistant.chat;

import com.codeassistant.config.RagProperties;
import com.codeassistant.util.RepoUrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final HybridRetrievalService hybridRetrievalService;
    private final JdbcTemplate jdbcTemplate;
    private final RagProperties ragProperties;

    public record ChatContext(String promptText, List<Map<String, Object>> citations) {}

    public ChatContext prepareChatContext(String question, String repoUrl, List<String> memoryTurns) {
        String normalizedRepoUrl = RepoUrlUtils.normalizeRepoUrl(repoUrl);
        if (!shouldUseGroundedRetrieval(question, normalizedRepoUrl)) {
            String conversationalPrompt = withConversationMemory(question, memoryTurns);
            return new ChatContext(conversationalPrompt, List.of());
        }

        SearchRequest searchRequest = buildSearchRequest(question, normalizedRepoUrl);
        List<Map<String, Object>> hybridMatches = hybridRetrievalService.retrieve(question, searchRequest, normalizedRepoUrl);
        String manifest = buildRepositoryManifest(normalizedRepoUrl);
        String resolvedQuestion = withMemoryAndHybridHints(question, memoryTurns, manifest, hybridMatches);

        return new ChatContext(resolvedQuestion, hybridMatches);
    }

    public Flux<String> streamAnswer(String question, String repoUrl, List<String> memoryTurns) {
        ChatContext context = prepareChatContext(question, repoUrl, memoryTurns);
        return streamAnswerWithContext(context);
    }

    public Flux<String> streamAnswerWithContext(ChatContext context) {
        return chatClient.prompt()
                .user(context.promptText())
                .stream()
                .content();
    }

    public List<Map<String, Object>> retrieveCitations(String question, String repoUrl) {
        String normalizedRepoUrl = RepoUrlUtils.normalizeRepoUrl(repoUrl);
        if (!shouldUseGroundedRetrieval(question, normalizedRepoUrl)) {
            return List.of();
        }
        SearchRequest searchRequest = buildSearchRequest(question, normalizedRepoUrl);
        return hybridRetrievalService.retrieve(question, searchRequest, normalizedRepoUrl);
    }

    private SearchRequest buildSearchRequest(String query, String normalizedRepoUrl) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(ragProperties.getTopK())
                .similarityThreshold(ragProperties.getSimilarityThreshold());

        if (normalizedRepoUrl != null) {
            builder.filterExpression("repo_url == '" + normalizedRepoUrl.replace("'", "\\'") + "'");
        }

        return builder.build();
    }

    private String buildRepositoryManifest(String normalizedRepoUrl) {
        if (normalizedRepoUrl == null || normalizedRepoUrl.isBlank()) {
            return "";
        }
        try {
            // Fetch top distinct files in the repository
            String filesSql = """
                    SELECT DISTINCT metadata->>'file_path' AS file_path
                    FROM vector_store
                    WHERE metadata->>'repo_url' = ?
                    ORDER BY metadata->>'file_path' ASC
                    LIMIT 30
                    """;
            List<String> files = jdbcTemplate.queryForList(filesSql, String.class, normalizedRepoUrl);

            // Fetch README preview if present
            String readmeSql = """
                    SELECT content
                    FROM vector_store
                    WHERE metadata->>'repo_url' = ?
                      AND LOWER(metadata->>'file_path') LIKE '%readme%'
                    LIMIT 1
                    """;
            List<String> readmeChunks = jdbcTemplate.queryForList(readmeSql, String.class, normalizedRepoUrl);

            StringBuilder sb = new StringBuilder();
            sb.append("=== REPOSITORY PROFILE & MANIFEST ===\n");
            sb.append("Repository: ").append(normalizedRepoUrl).append("\n");

            if (!files.isEmpty()) {
                sb.append("Key Files In Repository:\n");
                for (String f : files) {
                    if (f != null && !f.isBlank()) {
                        sb.append("- ").append(f).append("\n");
                    }
                }
            }

            if (!readmeChunks.isEmpty() && readmeChunks.get(0) != null) {
                String snippet = readmeChunks.get(0).trim();
                if (snippet.length() > 800) {
                    snippet = snippet.substring(0, 800) + "...";
                }
                sb.append("\nREADME / Project Summary Preview:\n").append(snippet).append("\n");
            }
            sb.append("=====================================\n\n");
            return sb.toString();
        } catch (Exception e) {
            log.warn("Could not build repository manifest for {}: {}", normalizedRepoUrl, e.getMessage());
            return "";
        }
    }

    private String withMemoryAndHybridHints(String question, List<String> memoryTurns, String manifest, List<Map<String, Object>> hybridMatches) {
        StringBuilder sb = new StringBuilder();

        if (manifest != null && !manifest.isBlank()) {
            sb.append(manifest);
        }

        if (memoryTurns != null && !memoryTurns.isEmpty()) {
            sb.append("### Conversation History (chronological order):\n");
            for (String turn : memoryTurns) {
                sb.append(turn).append("\n");
            }
            sb.append("\n");
        }

        if (hybridMatches != null && !hybridMatches.isEmpty()) {
            sb.append("### Retrieved Codebase Snippets:\n");
            int limit = Math.min(hybridMatches.size(), ragProperties.getMaxContextChunks());
            for (Map<String, Object> hit : hybridMatches.stream().limit(limit).toList()) {
                String filePath = String.valueOf(hit.getOrDefault("file_path", "unknown"));
                Object startLine = hit.getOrDefault("start_line", "?");
                Object endLine = hit.getOrDefault("end_line", "?");
                String source = String.valueOf(hit.getOrDefault("retrieval_source", "vector"));
                String confidence = String.valueOf(hit.getOrDefault("retrieval_confidence", "low"));
                String content = String.valueOf(hit.getOrDefault("content", ""));

                sb.append("<code_snippet file=\"").append(filePath)
                        .append("\" lines=\"").append(startLine).append("-").append(endLine)
                        .append("\" source=\"").append(source)
                        .append("\" confidence=\"").append(confidence).append("\">\n");
                sb.append(content).append("\n");
                sb.append("</code_snippet>\n\n");
            }
        }

        sb.append("### User Question:\n").append(question);
        return sb.toString();
    }

    private String withConversationMemory(String question, List<String> memoryTurns) {
        StringBuilder sb = new StringBuilder();
        if (memoryTurns != null && !memoryTurns.isEmpty()) {
            sb.append("### Conversation Context:\n");
            for (String turn : memoryTurns) {
                sb.append(turn).append("\n");
            }
            sb.append("\n");
        }
        sb.append("### User Question:\n").append(question);
        return sb.toString();
    }

    private boolean shouldUseGroundedRetrieval(String question, String normalizedRepoUrl) {
        if (normalizedRepoUrl == null || normalizedRepoUrl.isBlank()) {
            return false;
        }
        String q = question == null ? "" : question.trim().toLowerCase(Locale.ROOT);
        if (q.isBlank()) {
            return false;
        }

        // Personal/chit-chat intents should stay conversational and memory-first.
        if (q.contains("my name is") || q.startsWith("hi") || q.startsWith("hello") || q.startsWith("hey")) {
            return false;
        }

        return true;
    }
}
