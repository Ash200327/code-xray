package com.codeassistant.ingestion;

public interface IngestionProgressListener {
    default void onQueued() {}
    default void onStarted() {}
    default void onDiscoveredFiles(int totalFiles) {}
    default void onFileStarted(String relativePath, int filesProcessed, int totalFiles) {}
    default void onFileChunked(String relativePath, int filesProcessed, int totalFiles, int totalChunks) {}
    default void onBatchStored(int batchesStored, int totalChunks) {}
    default void onCompleted(int totalFilesProcessed, int totalChunks) {}
    default void onFailed(String errorMessage, int totalFilesProcessed, int totalChunks) {}
}

