package com.codeassistant.ingestion.strategy;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MarkdownChunkingStrategy implements ChunkingStrategy {

    @Override
    public List<Document> chunk(String content, Map<String, Object> baseMetadata) {
        List<Document> chunks = new ArrayList<>();
        String[] lines = content.split("\n", -1);

        int sectionStart = 0;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            boolean isHeading = line.startsWith("## ") || line.startsWith("### ");

            if (isHeading && current.length() > 0) {
                // Flush current section
                String sectionContent = current.toString().trim();
                if (!sectionContent.isBlank()) {
                    Map<String, Object> meta = new HashMap<>(baseMetadata);
                    meta.put("start_line", sectionStart + 1);
                    meta.put("end_line", i);
                    meta.put("chunk_type", "section");
                    chunks.add(new Document(sectionContent, meta));
                }
                current = new StringBuilder();
                sectionStart = i;
            }

            current.append(line).append("\n");
        }

        // Flush last section
        String remaining = current.toString().trim();
        if (!remaining.isBlank()) {
            Map<String, Object> meta = new HashMap<>(baseMetadata);
            meta.put("start_line", sectionStart + 1);
            meta.put("end_line", lines.length);
            meta.put("chunk_type", "section");
            chunks.add(new Document(remaining, meta));
        }

        return chunks;
    }
}
