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
public class JavaScriptChunkingStrategy implements ChunkingStrategy {

    private static final Pattern DECL_PATTERN = Pattern.compile(
            "(?m)^\\s*(export\\s+)?(async\\s+)?(function\\s+[\\w$]+\\s*\\(|class\\s+[\\w$]+\\s*\\{|const\\s+[\\w$]+\\s*=\\s*\\(.*\\)\\s*=>|const\\s+[\\w$]+\\s*=\\s*async\\s*\\(.*\\)\\s*=>)"
    );

    @Override
    public List<Document> chunk(String content, Map<String, Object> baseMetadata) {
        List<Document> chunks = new ArrayList<>();
        Matcher matcher = DECL_PATTERN.matcher(content);
        List<Integer> starts = new ArrayList<>();

        while (matcher.find()) {
            starts.add(matcher.start());
        }

        if (starts.isEmpty()) {
            return fallback(content, baseMetadata);
        }

        int[] charToLine = buildCharToLine(content);
        for (int i = 0; i < starts.size(); i++) {
            int start = starts.get(i);
            int end = (i + 1 < starts.size()) ? starts.get(i + 1) : content.length();
            String chunkContent = content.substring(start, end).trim();
            if (chunkContent.isBlank()) continue;

            int startLine = charToLine[Math.min(start, charToLine.length - 1)] + 1;
            int endLine = charToLine[Math.min(end - 1, charToLine.length - 1)] + 1;

            Map<String, Object> meta = new HashMap<>(baseMetadata);
            meta.put("start_line", startLine);
            meta.put("end_line", endLine);
            meta.put("chunk_type", "function_or_class");

            chunks.add(new Document(chunkContent, meta));
        }

        return chunks.isEmpty() ? fallback(content, baseMetadata) : chunks;
    }

    private List<Document> fallback(String content, Map<String, Object> baseMetadata) {
        String[] lines = content.split("\n", -1);
        Map<String, Object> meta = new HashMap<>(baseMetadata);
        meta.put("start_line", 1);
        meta.put("end_line", lines.length);
        meta.put("chunk_type", "file");
        return List.of(new Document(content, meta));
    }

    private int[] buildCharToLine(String content) {
        if (content.isEmpty()) return new int[]{0};
        int[] map = new int[content.length()];
        int line = 0;
        for (int i = 0; i < content.length(); i++) {
            map[i] = line;
            if (content.charAt(i) == '\n') line++;
        }
        return map;
    }
}

