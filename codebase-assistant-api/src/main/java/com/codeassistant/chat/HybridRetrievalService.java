package com.codeassistant.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class HybridRetrievalService {

    private final VectorStore vectorStore;
    private final KeywordSearchService keywordSearchService;

    public List<Map<String, Object>> retrieve(String question, SearchRequest searchRequest, String normalizedRepoUrl) {
        List<Map<String, Object>> vectorResults = vectorStore.similaritySearch(searchRequest).stream()
                .map(doc -> {
                    Map<String, Object> meta = new HashMap<>(doc.getMetadata());
                    meta.put("content", doc.getText());
                    meta.put("retrieval_source", "vector");
                    return meta;
                })
                .toList();

        List<Map<String, Object>> keywordResults = keywordSearchService.search(question, normalizedRepoUrl, searchRequest.getTopK());
        return rerankAndMerge(vectorResults, keywordResults, searchRequest.getTopK());
    }

    private List<Map<String, Object>> rerankAndMerge(List<Map<String, Object>> vectorResults,
                                                     List<Map<String, Object>> keywordResults,
                                                     int topK) {
        LinkedHashMap<String, Map<String, Object>> merged = new LinkedHashMap<>();

        for (int i = 0; i < vectorResults.size(); i++) {
            Map<String, Object> m = new HashMap<>(vectorResults.get(i));
            double score = 1.0 - (i * 0.08);
            m.put("hybrid_score", score);
            m.put("retrieval_confidence", confidenceLabel(score));
            m.put("match_reason", "semantic similarity match");
            merged.put(key(m), m);
        }

        for (int i = 0; i < keywordResults.size(); i++) {
            Map<String, Object> incoming = keywordResults.get(i);
            String key = key(incoming);
            double keywordRankScore = 1.0 - (i * 0.08);
            Map<String, Object> existing = merged.get(key);
            if (existing == null) {
                Map<String, Object> m = new HashMap<>(incoming);
                m.put("hybrid_score", keywordRankScore);
                m.put("retrieval_confidence", confidenceLabel(keywordRankScore));
                m.put("match_reason", "keyword full-text match");
                merged.put(key, m);
            } else {
                double existingScore = asDouble(existing.get("hybrid_score"));
                double boosted = Math.max(existingScore, keywordRankScore) + 0.15;
                existing.put("hybrid_score", boosted);
                existing.put("retrieval_source", "hybrid");
                if (incoming.get("keyword_score") != null) {
                    existing.put("keyword_score", incoming.get("keyword_score"));
                }
                existing.put("retrieval_confidence", confidenceLabel(boosted));
                existing.put("match_reason", "matched by both semantic and keyword retrieval");
            }
        }

        return merged.values().stream()
                .sorted((a, b) -> Double.compare(asDouble(b.get("hybrid_score")), asDouble(a.get("hybrid_score"))))
                .limit(topK)
                .toList();
    }

    private String key(Map<String, Object> metadata) {
        return String.valueOf(metadata.get("file_path")) + ":" + String.valueOf(metadata.get("start_line"));
    }

    private double asDouble(Object value) {
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return 0.0;
    }

    private String confidenceLabel(double score) {
        if (score >= 0.9) {
            return "high";
        }
        if (score >= 0.7) {
            return "medium";
        }
        return "low";
    }

}
