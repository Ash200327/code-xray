package com.codeassistant.ingestion;

import com.codeassistant.ingestion.strategy.ChunkingStrategy;
import com.codeassistant.ingestion.strategy.DefaultChunkingStrategy;
import com.codeassistant.ingestion.strategy.JavaChunkingStrategy;
import com.codeassistant.ingestion.strategy.JavaScriptChunkingStrategy;
import com.codeassistant.ingestion.strategy.MarkdownChunkingStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChunkingService {

    private final JavaChunkingStrategy javaChunkingStrategy;
    private final JavaScriptChunkingStrategy javaScriptChunkingStrategy;
    private final MarkdownChunkingStrategy markdownChunkingStrategy;
    private final DefaultChunkingStrategy defaultChunkingStrategy;

    public List<Document> chunk(String content, String language, Map<String, Object> baseMetadata) {
        ChunkingStrategy strategy = switch (language) {
            case "java", "kotlin" -> javaChunkingStrategy;
            case "typescript", "javascript" -> javaScriptChunkingStrategy;
            case "markdown" -> markdownChunkingStrategy;
            default -> defaultChunkingStrategy;
        };
        return strategy.chunk(content, baseMetadata);
    }
}
