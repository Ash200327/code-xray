package com.codeassistant.model;

import com.codeassistant.ingestion.IngestionJobStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class IngestionJobView {
    private UUID jobId;
    private String repoUrl;
    private String branch;
    private IngestionJobStatus status;
    private int attempt;
    private int maxAttempts;
    private Instant createdAt;
    private Instant startedAt;
    private Instant completedAt;
    private String errorMessage;
    private IngestResult result;
}

