package com.codeassistant.ingestion.strategy;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

public interface ChunkingStrategy {
    List<Document> chunk(String content, Map<String, Object> baseMetadata);
}
