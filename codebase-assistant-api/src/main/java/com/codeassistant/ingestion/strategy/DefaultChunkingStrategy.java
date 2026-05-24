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
    private static final int MIN_CHUNK_CHARS = 50;    // minChunkSizeChars (Spring AI 1.0.0 param 2)

    @Override
    public List<Document> chunk(String content, Map<String, Object> baseMetadata) {
        // TokenTextSplitter(defaultChunkSize, minChunkSizeChars, minChunkLengthToEmbed, maxNumChunks, keepSeparator)
        TokenTextSplitter splitter = new TokenTextSplitter(CHUNK_SIZE, MIN_CHUNK_CHARS, 5, 10000, true);

        Document fullDoc = new Document(content, new HashMap<>(baseMetadata));
        List<Document> chunks = splitter.apply(List.of(fullDoc));

        // Assign approximate line numbers per chunk
        String[] allLines = content.split("\n", -1);
        int currentLine = 1;

        List<Document> result = new ArrayList<>();
        for (Document chunk : chunks) {
            String chunkContent = chunk.getText();
            int lineCount = chunkContent.split("\n", -1).length;
            int endLine = currentLine + lineCount - 1;

            Map<String, Object> meta = new HashMap<>(baseMetadata);
            meta.put("start_line", currentLine);
            meta.put("end_line", Math.min(endLine, allLines.length));
            meta.put("chunk_type", "block");

            result.add(new Document(chunkContent, meta));
            currentLine = Math.max(1, endLine - MIN_CHUNK_CHARS / 10);
        }

        return result;
    }
}
