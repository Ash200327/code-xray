package com.codeassistant.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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
    private final VectorStore vectorStore;
    private final HybridRetrievalService hybridRetrievalService;

    public Flux<String> streamAnswer(String question, String repoUrl) {
        return streamAnswer(question, repoUrl, null);
    }

    public Flux<String> streamAnswer(String question, String repoUrl, List<String> memoryTurns) {
        log.debug("Chat request: question={}, repoUrl={}", question, repoUrl);

        String normalizedRepoUrl = normalizeRepoUrl(repoUrl);
        if (!shouldUseGroundedRetrieval(question, normalizedRepoUrl)) {
            String conversationalPrompt = withConversationMemory(question, memoryTurns);
            return chatClient.prompt()
                    .user(conversationalPrompt)
                    .stream()
                    .content();
        }

        SearchRequest searchRequest = buildSearchRequest(question, normalizedRepoUrl);
        List<Map<String, Object>> hybridMatches = hybridRetrievalService.retrieve(question, searchRequest, normalizedRepoUrl);
        String resolvedQuestion = withMemoryAndHybridHints(question, memoryTurns, hybridMatches);

        return chatClient.prompt()
                .user(resolvedQuestion)
                .stream()
                .content();
    }

    public List<Map<String, Object>> retrieveCitations(String question, String repoUrl) {
        String normalizedRepoUrl = normalizeRepoUrl(repoUrl);
        if (!shouldUseGroundedRetrieval(question, normalizedRepoUrl)) {
            return List.of();
        }
        SearchRequest searchRequest = buildSearchRequest(question, normalizedRepoUrl);
        return hybridRetrievalService.retrieve(question, searchRequest, normalizedRepoUrl);
    }

    private SearchRequest buildSearchRequest(String query, String normalizedRepoUrl) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(8)
                .similarityThreshold(0.2);

        if (normalizedRepoUrl != null) {
            builder.filterExpression("repo_url == '" + normalizedRepoUrl.replace("'", "\\'") + "'");
        }

        return builder.build();
    }

    private String normalizeRepoUrl(String repoUrl) {
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
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }

    private String withMemoryAndHybridHints(String question, List<String> memoryTurns, List<Map<String, Object>> hybridMatches) {
        StringBuilder sb = new StringBuilder();

        if (memoryTurns != null && !memoryTurns.isEmpty()) {
            sb.append("Conversation history (chronological order):\n");
            for (String turn : memoryTurns) {
                sb.append(turn).append("\n");
            }
            sb.append("\n");
        }

        if (hybridMatches != null && !hybridMatches.isEmpty()) {
            sb.append("Codebase Context:\n");
            for (Map<String, Object> hit : hybridMatches.stream().limit(5).toList()) {
                String filePath = String.valueOf(hit.getOrDefault("file_path", "unknown"));
                Object startLine = hit.getOrDefault("start_line", "?");
                Object endLine = hit.getOrDefault("end_line", "?");
                String source = String.valueOf(hit.getOrDefault("retrieval_source", "vector"));
                String confidence = String.valueOf(hit.getOrDefault("retrieval_confidence", "low"));
                String content = String.valueOf(hit.getOrDefault("content", ""));

                sb.append("<context>\n");
                sb.append("File: ").append(filePath).append(" (lines ").append(startLine).append("-").append(endLine).append(")\n");
                sb.append("Source: ").append(source).append(" (Confidence: ").append(confidence).append(")\n");
                sb.append("--------------------------------------------------\n");
                sb.append(content).append("\n");
                sb.append("--------------------------------------------------\n");
                sb.append("</context>\n\n");
            }
        }

        sb.append("Current User Question:\n").append(question);
        return sb.toString();
    }

    private String withConversationMemory(String question, List<String> memoryTurns) {
        StringBuilder sb = new StringBuilder();
        if (memoryTurns != null && !memoryTurns.isEmpty()) {
            sb.append("Conversation context (most recent first):\n");
            for (String turn : memoryTurns) {
                sb.append(turn).append("\n");
            }
            sb.append("\n");
        }
        sb.append("Current user question:\n").append(question);
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
