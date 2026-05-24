package com.codeassistant.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class IngestionProgressEvent {
    private UUID jobId;
    private String state;
    private String repoUrl;
    private String branch;
    private String currentFile;
    private int totalFilesDiscovered;
    private int filesProcessed;
    private int totalChunks;
    private int batchesStored;
    private int percentage;
    private String message;
    private String error;
}

