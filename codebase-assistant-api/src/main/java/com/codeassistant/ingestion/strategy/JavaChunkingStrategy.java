package com.codeassistant.ingestion.strategy;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JavaChunkingStrategy implements ChunkingStrategy {

    // Matches method or class declarations
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?m)^\\s{0,4}(?:(?:public|private|protected|static|final|abstract|synchronized|default)\\s+)*" +
            "(?:[\\w<>\\[\\]]+\\s+)+([\\w]+)\\s*\\([^)]{0,500}\\)\\s*(?:throws\\s+[\\w,\\s]+)?\\s*\\{",
            Pattern.MULTILINE
    );

    @Override
    public List<Document> chunk(String content, Map<String, Object> baseMetadata) {
        List<Document> chunks = new ArrayList<>();
        String[] lines = content.split("\n", -1);

        Matcher matcher = METHOD_PATTERN.matcher(content);
        List<int[]> methodBoundaries = new ArrayList<>();

        // Collect all method start positions (character offsets)
        while (matcher.find()) {
            methodBoundaries.add(new int[]{matcher.start(), matcher.end()});
        }

        if (methodBoundaries.isEmpty()) {
            // No methods found — fall back to default chunking
            return fallback(content, baseMetadata, lines);
        }

        // Build char-offset → line-number index
        int[] charToLine = buildCharToLine(content);

        for (int i = 0; i < methodBoundaries.size(); i++) {
            int start = methodBoundaries.get(i)[0];
            int end = (i + 1 < methodBoundaries.size())
                    ? methodBoundaries.get(i + 1)[0]
                    : content.length();

            String chunkContent = content.substring(start, end).trim();
            if (chunkContent.isBlank()) continue;

            int startLine = charToLine[Math.min(start, charToLine.length - 1)] + 1;
            int endLine = charToLine[Math.min(end - 1, charToLine.length - 1)] + 1;

            Map<String, Object> meta = new HashMap<>(baseMetadata);
            meta.put("start_line", startLine);
            meta.put("end_line", endLine);
            meta.put("chunk_type", "method");

            chunks.add(new Document(chunkContent, meta));
        }

        // Also add class-level preamble (imports, class declaration)
        if (!methodBoundaries.isEmpty() && methodBoundaries.get(0)[0] > 0) {
            String preamble = content.substring(0, methodBoundaries.get(0)[0]).trim();
            if (!preamble.isBlank()) {
                Map<String, Object> meta = new HashMap<>(baseMetadata);
                meta.put("start_line", 1);
                meta.put("end_line", charToLine[methodBoundaries.get(0)[0]] + 1);
                meta.put("chunk_type", "class");
                chunks.add(0, new Document(preamble, meta));
            }
        }

        return chunks.isEmpty() ? fallback(content, baseMetadata, lines) : chunks;
    }

    private int[] buildCharToLine(String content) {
        int[] charToLine = new int[content.length()];
        int line = 0;
        for (int i = 0; i < content.length(); i++) {
            charToLine[i] = line;
            if (content.charAt(i) == '\n') line++;
        }
        return charToLine;
    }

    private List<Document> fallback(String content, Map<String, Object> baseMetadata, String[] lines) {
        Map<String, Object> meta = new HashMap<>(baseMetadata);
        meta.put("start_line", 1);
        meta.put("end_line", lines.length);
        meta.put("chunk_type", "class");
        return List.of(new Document(content, meta));
    }
}
