package com.codeassistant.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IngestResult {
    private String repoUrl;
    private int totalFiles;
    private int totalChunks;
    private long durationMs;
    private String status;
}
