package com.codeassistant.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordSearchService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @Value("${codeassistant.retrieval.keyword.vector-table:vector_store}")
    private String vectorTable;

    public List<Map<String, Object>> search(String query, String repoUrl, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String sql = """
                SELECT content,
                       metadata::text AS metadata_text,
                       ts_rank_cd(to_tsvector('simple', content), plainto_tsquery('simple', ?)) AS keyword_score
                FROM %s
                WHERE to_tsvector('simple', content) @@ plainto_tsquery('simple', ?)
                  AND (? IS NULL OR metadata->>'repo_url' = ?)
                ORDER BY keyword_score DESC
                LIMIT ?
                """.formatted(vectorTable);

        try {
            return jdbcTemplate.query(sql, ps -> {
                ps.setString(1, query);
                ps.setString(2, query);
                ps.setString(3, repoUrl);
                ps.setString(4, repoUrl);
                ps.setInt(5, limit);
            }, (rs, rowNum) -> {
                String content = rs.getString("content");
                String metadataText = rs.getString("metadata_text");
                Map<String, Object> metadata;
                try {
                    metadata = objectMapper.readValue(metadataText, MAP_TYPE);
                } catch (Exception e) {
                    metadata = new java.util.HashMap<>();
                }
                metadata.put("content", content);
                metadata.put("keyword_score", rs.getDouble("keyword_score"));
                metadata.put("retrieval_source", "keyword");
                return metadata;
            });
        } catch (Exception e) {
            log.warn("Keyword search failed, falling back to vector-only retrieval: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
