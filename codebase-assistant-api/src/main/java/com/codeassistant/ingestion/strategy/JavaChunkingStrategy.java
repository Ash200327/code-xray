package com.codeassistant.ingestion.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class JavaChunkingStrategy implements ChunkingStrategy {

    private final DefaultChunkingStrategy defaultChunkingStrategy;

    // Matches method or constructor declarations including preceding annotations
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "(?m)(?:^[ \\t]*@[\\w]+(?:\\([^)]*\\))?[ \\t]*\\r?\\n)*" +
            "^[ \\t]{0,4}(?:(?:public|private|protected|static|final|abstract|synchronized|default|native)\\s+)*" +
            "(?:[\\w<>\\[\\],\\s]+\\s+)+([\\w]+)\\s*\\([^)]{0,500}\\)\\s*(?:throws\\s+[\\w,\\s]+)?\\s*\\{",
            Pattern.MULTILINE
    );

    @Override
    public List<Document> chunk(String content, Map<String, Object> baseMetadata) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        Matcher matcher = METHOD_PATTERN.matcher(content);
        List<int[]> methodBoundaries = new ArrayList<>();

        while (matcher.find()) {
            methodBoundaries.add(new int[]{matcher.start(), matcher.end()});
        }

        if (methodBoundaries.isEmpty()) {
            return defaultChunkingStrategy.chunk(content, baseMetadata);
        }

        List<Document> chunks = new ArrayList<>();
        int[] charToLine = buildCharToLine(content);

        // Class-level preamble (imports, package, class header)
        if (methodBoundaries.get(0)[0] > 0) {
            String preamble = content.substring(0, methodBoundaries.get(0)[0]).trim();
            if (!preamble.isBlank()) {
                Map<String, Object> meta = new HashMap<>(baseMetadata);
                meta.put("start_line", 1);
                meta.put("end_line", charToLine[methodBoundaries.get(0)[0]] + 1);
                meta.put("chunk_type", "class_header");
                chunks.add(new Document(preamble, meta));
            }
        }

        for (int i = 0; i < methodBoundaries.size(); i++) {
            int start = methodBoundaries.get(i)[0];
            int end = (i + 1 < methodBoundaries.size())
                    ? methodBoundaries.get(i + 1)[0]
                    : content.length();

            String chunkContent = content.substring(start, end).trim();
            if (chunkContent.isBlank()) continue;

            // If a single method chunk is exceedingly huge (> 3500 chars), split with default token splitter
            if (chunkContent.length() > 3500) {
                Map<String, Object> methodMeta = new HashMap<>(baseMetadata);
                methodMeta.put("chunk_type", "method_part");
                chunks.addAll(defaultChunkingStrategy.chunk(chunkContent, methodMeta));
                continue;
            }

            int startLine = charToLine[Math.min(start, charToLine.length - 1)] + 1;
            int endLine = charToLine[Math.min(end - 1, charToLine.length - 1)] + 1;

            Map<String, Object> meta = new HashMap<>(baseMetadata);
            meta.put("start_line", startLine);
            meta.put("end_line", endLine);
            meta.put("chunk_type", "method");

            chunks.add(new Document(chunkContent, meta));
        }

        return chunks.isEmpty() ? defaultChunkingStrategy.chunk(content, baseMetadata) : chunks;
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
}
