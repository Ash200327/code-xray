package com.codeassistant.ingestion;

import com.codeassistant.model.IngestRequest;
import com.codeassistant.model.IngestResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final GitRepoCloner cloner;
    private final FileWalker fileWalker;
    private final ChunkingService chunkingService;
    private final VectorStore vectorStore;

    private static final int BATCH_SIZE = 100;

    public IngestResult ingest(IngestRequest request) {
        return ingest(request, null);
    }

    public IngestResult ingest(IngestRequest request, IngestionProgressListener progressListener) {
        long start = System.currentTimeMillis();
        Path repoDir = null;
        int totalFiles = 0;
        int totalChunks = 0;
        int batchesStored = 0;

        try {
            if (progressListener != null) {
                progressListener.onStarted();
            }
            repoDir = cloner.clone(request.getRepoUrl(), request.getBranch());
            List<Path> files = fileWalker.walk(repoDir);
            log.info("Found {} files to index", files.size());
            if (progressListener != null) {
                progressListener.onDiscoveredFiles(files.size());
            }
            String normalizedRepoUrl = normalizeRepoUrl(request.getRepoUrl());

            List<Document> batch = new ArrayList<>();
            for (Path file : files) {
                try {
                    String relativePath = repoDir.relativize(file).toString().replace("\\", "/");
                    if (progressListener != null) {
                        progressListener.onFileStarted(relativePath, totalFiles, files.size());
                    }

                    String content = Files.readString(file, StandardCharsets.UTF_8);
                    if (content.isBlank()) {
                        continue;
                    }

                    String language = fileWalker.detectLanguage(file.getFileName().toString());

                    Map<String, Object> baseMetadata = new HashMap<>();
                    baseMetadata.put("repo_url", normalizedRepoUrl);
                    baseMetadata.put("file_path", relativePath);
                    baseMetadata.put("file_name", file.getFileName().toString());
                    baseMetadata.put("language", language);

                    List<Document> chunks = chunkingService.chunk(content, language, baseMetadata);
                    batch.addAll(chunks);
                    totalChunks += chunks.size();
                    totalFiles++;
                    if (progressListener != null) {
                        progressListener.onFileChunked(relativePath, totalFiles, files.size(), totalChunks);
                    }

                    if (batch.size() >= BATCH_SIZE) {
                        vectorStore.add(batch);
                        log.debug("Stored batch of {} chunks", batch.size());
                        batch.clear();
                        batchesStored++;
                        if (progressListener != null) {
                            progressListener.onBatchStored(batchesStored, totalChunks);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Skipping file {}: {}", file, e.getMessage());
                }
            }

            if (!batch.isEmpty()) {
                vectorStore.add(batch);
                log.debug("Stored final batch of {} chunks", batch.size());
                batchesStored++;
                if (progressListener != null) {
                    progressListener.onBatchStored(batchesStored, totalChunks);
                }
            }

        } catch (GitAPIException | IOException e) {
            log.error("Ingestion failed: {}", e.getMessage(), e);
            if (progressListener != null) {
                progressListener.onFailed(e.getMessage(), totalFiles, totalChunks);
            }
            return IngestResult.builder()
                    .repoUrl(request.getRepoUrl())
                    .totalFiles(totalFiles)
                    .totalChunks(totalChunks)
                    .durationMs(System.currentTimeMillis() - start)
                    .status("FAILED: " + e.getMessage())
                    .build();
        } finally {
            if (repoDir != null) {
                try {
                    cloner.delete(repoDir);
                } catch (IOException e) {
                    log.warn("Could not delete temp dir: {}", repoDir);
                }
            }
        }

        if (progressListener != null) {
            progressListener.onCompleted(totalFiles, totalChunks);
        }

        return IngestResult.builder()
                .repoUrl(request.getRepoUrl())
                .totalFiles(totalFiles)
                .totalChunks(totalChunks)
                .durationMs(System.currentTimeMillis() - start)
                .status("SUCCESS")
                .build();
    }

    private String normalizeRepoUrl(String repoUrl) {
        if (repoUrl == null) {
            return "";
        }
        String normalized = repoUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }
}
