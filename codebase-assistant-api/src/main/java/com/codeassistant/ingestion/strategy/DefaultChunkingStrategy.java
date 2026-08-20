package com.codeassistant.ingestion.strategy;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DefaultChunkingStrategy implements ChunkingStrategy {

    private static final int CHUNK_SIZE = 300;       // defaultChunkSize in tokens
    private static final int MIN_CHUNK_CHARS = 50;   // minChunkSizeChars

    @Override
    public List<Document> chunk(String content, Map<String, Object> baseMetadata) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        TokenTextSplitter splitter = new TokenTextSplitter(CHUNK_SIZE, MIN_CHUNK_CHARS, 5, 10000, true);
        Document fullDoc = new Document(content, new HashMap<>(baseMetadata));
        List<Document> rawChunks = splitter.apply(List.of(fullDoc));

        List<Document> result = new ArrayList<>();
        int searchOffset = 0;

        for (Document chunk : rawChunks) {
            String chunkContent = chunk.getText();
            if (chunkContent == null || chunkContent.isBlank()) {
                continue;
            }

            int foundIndex = content.indexOf(chunkContent.trim(), searchOffset);
            int startLine;
            int endLine;

            if (foundIndex >= 0) {
                startLine = countNewlines(content, 0, foundIndex) + 1;
                endLine = startLine + countNewlines(chunkContent, 0, chunkContent.length());
                searchOffset = foundIndex + chunkContent.length();
            } else {
                startLine = countNewlines(content, 0, Math.min(searchOffset, content.length())) + 1;
                endLine = startLine + countNewlines(chunkContent, 0, chunkContent.length());
            }

            Map<String, Object> meta = new HashMap<>(baseMetadata);
            meta.put("start_line", startLine);
            meta.put("end_line", Math.max(startLine, endLine));
            meta.put("chunk_type", "block");

            result.add(new Document(chunkContent, meta));
        }

        return result;
    }

    private int countNewlines(String str, int start, int end) {
        int count = 0;
        int max = Math.min(end, str.length());
        for (int i = start; i < max; i++) {
            if (str.charAt(i) == '\n') {
                count++;
            }
        }
        return count;
    }
}
