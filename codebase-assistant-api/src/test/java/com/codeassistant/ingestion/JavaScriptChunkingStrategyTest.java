package com.codeassistant.ingestion;

import com.codeassistant.ingestion.strategy.JavaScriptChunkingStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaScriptChunkingStrategyTest {

    @Test
    void shouldSplitByFunctionsAndClasses() {
        JavaScriptChunkingStrategy strategy = new JavaScriptChunkingStrategy();
        String content = """
                export function alpha() {
                  return 1;
                }
                                
                class Beta {
                  method() { return 2; }
                }
                                
                const gamma = () => {
                  return 3;
                };
                """;

        Map<String, Object> meta = new HashMap<>();
        meta.put("file_path", "src/sample.ts");
        List<Document> docs = strategy.chunk(content, meta);

        assertFalse(docs.isEmpty());
        assertTrue(docs.size() >= 2);
    }
}

