package com.codeassistant.ingestion;

import com.codeassistant.api.ResourceNotFoundException;
import com.codeassistant.domain.IngestionJobEntity;
import com.codeassistant.domain.RepositoryEntity;
import com.codeassistant.model.IngestRequest;
import com.codeassistant.model.IngestResult;
import com.codeassistant.model.IngestionJobView;
import com.codeassistant.model.IngestionProgressEvent;
import com.codeassistant.domain.UserEntity;
import com.codeassistant.repository.IngestionJobRepository;
import com.codeassistant.repository.RepositoryEntityRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionJobService {

    private final IngestionService ingestionService;
    @Qualifier("ingestionExecutor")
    private final Executor ingestionExecutor;
    private final IngestionJobRepository ingestionJobRepository;
    private final RepositoryEntityRepository repositoryRepository;
    private final ObjectMapper objectMapper;
    private final IngestionProgressPublisher progressPublisher;

    @Value("${codeassistant.ingestion.jobs.max-attempts:3}")
    private int defaultMaxAttempts;

    public IngestionJobView createJob(IngestRequest request, UserEntity currentUser) {
        validateRequest(request);

        IngestionJobEntity entity = new IngestionJobEntity();
        entity.setId(UUID.randomUUID());
        entity.setRepoUrl(normalizeRepoUrl(request.getRepoUrl()));
        entity.setBranch(defaultBranch(request.getBranch()));
        entity.setStatus(IngestionJobStatus.QUEUED.name());
        entity.setAttempt(0);
        entity.setMaxAttempts(defaultMaxAttempts);
        entity.setRepository(resolveOrCreateRepository(entity.getRepoUrl(), entity.getBranch(), currentUser));
        entity.setUser(currentUser);

        IngestionJobEntity saved = ingestionJobRepository.save(entity);
        progressPublisher.publish(saved.getId(), IngestionProgressEvent.builder()
                .jobId(saved.getId())
                .state("QUEUED")
                .repoUrl(saved.getRepoUrl())
                .branch(saved.getBranch())
                .message("Job queued")
                .percentage(0)
                .build());
        submit(saved.getId());
        return toView(saved);
    }

    public IngestionJobView getJob(UUID jobId, UserEntity currentUser) {
        return toView(load(jobId, currentUser));
    }

    public List<IngestionJobView> listJobs(UserEntity currentUser) {
        if (currentUser != null) {
            return ingestionJobRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()).stream()
                    .map(this::toView)
                    .toList();
        }
        return ingestionJobRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toView)
                .toList();
    }

    public IngestionJobView retry(UUID jobId, UserEntity currentUser) {
        IngestionJobEntity entity = load(jobId, currentUser);
        if (IngestionJobStatus.RUNNING.name().equals(entity.getStatus()) || IngestionJobStatus.QUEUED.name().equals(entity.getStatus())) {
            throw new IllegalStateException("Cannot retry a job that is still in progress");
        }
        if (entity.getAttempt() >= entity.getMaxAttempts()) {
            throw new IllegalStateException("Retry limit reached for job " + jobId);
        }

        entity.setStatus(IngestionJobStatus.QUEUED.name());
        entity.setStartedAt(null);
        entity.setCompletedAt(null);
        entity.setErrorMessage(null);
        entity.setResultJson(null);
        ingestionJobRepository.save(entity);
        progressPublisher.publish(entity.getId(), IngestionProgressEvent.builder()
                .jobId(entity.getId())
                .state("QUEUED")
                .repoUrl(entity.getRepoUrl())
                .branch(entity.getBranch())
                .message("Job re-queued")
                .percentage(0)
                .build());

        submit(jobId);
        return toView(entity);
    }

    private void submit(UUID jobId) {
        ingestionExecutor.execute(() -> run(jobId));
    }

    private void run(UUID jobId) {
        IngestionJobEntity entity = load(jobId, null);

        entity.setStatus(IngestionJobStatus.RUNNING.name());
        entity.setStartedAt(Instant.now());
        entity.setAttempt(entity.getAttempt() + 1);
        ingestionJobRepository.save(entity);

        log.info("Ingestion job {} started (attempt {}/{})", entity.getId(), entity.getAttempt(), entity.getMaxAttempts());
        progressPublisher.publish(entity.getId(), IngestionProgressEvent.builder()
                .jobId(entity.getId())
                .state("RUNNING")
                .repoUrl(entity.getRepoUrl())
                .branch(entity.getBranch())
                .message("Ingestion started")
                .percentage(0)
                .build());

        IngestRequest request = new IngestRequest();
        request.setRepoUrl(entity.getRepoUrl());
        request.setBranch(entity.getBranch());

        IngestResult result = ingestionService.ingest(request, createProgressListener(entity));

        entity = load(jobId, null);
        entity.setCompletedAt(Instant.now());
        entity.setResultJson(serializeResult(result));

        if ("SUCCESS".equals(result.getStatus())) {
            entity.setStatus(IngestionJobStatus.COMPLETED.name());
            entity.setErrorMessage(null);
            repositoryRepository.findByRepoUrlAndBranch(entity.getRepoUrl(), entity.getBranch())
                    .ifPresent(repository -> {
                        repository.setLastIngestedAt(Instant.now());
                        repositoryRepository.save(repository);
                    });
        } else {
            entity.setStatus(IngestionJobStatus.FAILED.name());
            entity.setErrorMessage(result.getStatus());
        }

        ingestionJobRepository.save(entity);
        log.info("Ingestion job {} finished with status={}", entity.getId(), entity.getStatus());
    }

    private IngestionProgressListener createProgressListener(IngestionJobEntity job) {
        return new IngestionProgressListener() {
            private int totalFilesDiscovered;
            private int filesProcessed;
            private int totalChunks;
            private int batchesStored;
            private String currentFile;

            @Override
            public void onDiscoveredFiles(int totalFiles) {
                totalFilesDiscovered = totalFiles;
                publish("RUNNING", "Discovered files for indexing", null);
            }

            @Override
            public void onFileStarted(String relativePath, int processedCount, int totalFiles) {
                currentFile = relativePath;
                filesProcessed = processedCount;
                totalFilesDiscovered = totalFiles;
                publish("RUNNING", "Processing file", null);
            }

            @Override
            public void onFileChunked(String relativePath, int processedCount, int totalFiles, int chunks) {
                currentFile = relativePath;
                filesProcessed = processedCount;
                totalFilesDiscovered = totalFiles;
                totalChunks = chunks;
                publish("RUNNING", "File chunked", null);
            }

            @Override
            public void onBatchStored(int batchCount, int chunks) {
                batchesStored = batchCount;
                totalChunks = chunks;
                publish("RUNNING", "Stored embeddings batch", null);
            }

            @Override
            public void onCompleted(int totalFilesProcessed, int chunks) {
                filesProcessed = totalFilesProcessed;
                totalChunks = chunks;
                publish("COMPLETED", "Ingestion completed", null);
            }

            @Override
            public void onFailed(String errorMessage, int totalFilesProcessed, int chunks) {
                filesProcessed = totalFilesProcessed;
                totalChunks = chunks;
                publish("FAILED", "Ingestion failed", errorMessage);
            }

            private void publish(String state, String message, String error) {
                progressPublisher.publish(job.getId(), IngestionProgressEvent.builder()
                        .jobId(job.getId())
                        .state(state)
                        .repoUrl(job.getRepoUrl())
                        .branch(job.getBranch())
                        .currentFile(currentFile)
                        .totalFilesDiscovered(totalFilesDiscovered)
                        .filesProcessed(filesProcessed)
                        .totalChunks(totalChunks)
                        .batchesStored(batchesStored)
                        .percentage(computePercentage(filesProcessed, totalFilesDiscovered, state))
                        .message(message)
                        .error(error)
                        .build());
            }
        };
    }

    private int computePercentage(int processed, int total, String state) {
        if ("COMPLETED".equals(state)) {
            return 100;
        }
        if (total <= 0) {
            return 0;
        }
        return Math.min(99, (int) ((processed * 100.0) / total));
    }

    private IngestionJobEntity load(UUID id, UserEntity currentUser) {
        IngestionJobEntity entity = ingestionJobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingestion job not found: " + id));
        if (currentUser != null && entity.getUser() != null && !entity.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Ingestion job not found: " + id);
        }
        return entity;
    }

    private String serializeResult(IngestResult result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private IngestResult deserializeResult(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(resultJson, IngestResult.class);
        } catch (IOException e) {
            return null;
        }
    }

    private IngestionJobView toView(IngestionJobEntity entity) {
        return IngestionJobView.builder()
                .jobId(entity.getId())
                .repoUrl(entity.getRepoUrl())
                .branch(entity.getBranch())
                .status(IngestionJobStatus.valueOf(entity.getStatus()))
                .attempt(entity.getAttempt())
                .maxAttempts(entity.getMaxAttempts())
                .createdAt(entity.getCreatedAt())
                .startedAt(entity.getStartedAt())
                .completedAt(entity.getCompletedAt())
                .errorMessage(entity.getErrorMessage())
                .result(deserializeResult(entity.getResultJson()))
                .build();
    }

    private RepositoryEntity resolveOrCreateRepository(String repoUrl, String branch, UserEntity currentUser) {
        if (currentUser != null) {
            return repositoryRepository.findByRepoUrlAndBranchAndUserId(repoUrl, branch, currentUser.getId())
                    .orElseGet(() -> createRepository(repoUrl, branch, currentUser));
        }
        return repositoryRepository.findByRepoUrlAndBranch(repoUrl, branch)
                .orElseGet(() -> {
                    RepositoryEntity repository = new RepositoryEntity();
                    repository.setId(UUID.randomUUID());
                    repository.setRepoUrl(repoUrl);
                    repository.setBranch(branch);
                    repository.setName(deriveRepoName(repoUrl));
                    repository.setWorkspace(null);
                    return repositoryRepository.save(repository);
                });
    }

    private RepositoryEntity createRepository(String repoUrl, String branch, UserEntity currentUser) {
        RepositoryEntity repository = new RepositoryEntity();
        repository.setId(UUID.randomUUID());
        repository.setRepoUrl(repoUrl);
        repository.setBranch(branch);
        repository.setName(deriveRepoName(repoUrl));
        repository.setWorkspace(null);
        repository.setUser(currentUser);
        return repositoryRepository.save(repository);
    }

    private String deriveRepoName(String repoUrl) {
        if (repoUrl == null || repoUrl.isBlank()) {
            return "repository";
        }
        String[] parts = repoUrl.split("/");
        return parts[parts.length - 1];
    }

    private void validateRequest(IngestRequest request) {
        if (request == null || request.getRepoUrl() == null || request.getRepoUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("repoUrl is required");
        }
    }

    private String normalizeRepoUrl(String repoUrl) {
        String normalized = repoUrl.trim();
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.endsWith(".git")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }
        return normalized;
    }

    private String defaultBranch(String branch) {
        if (branch == null || branch.isBlank()) {
            return "main";
        }
        return branch.trim();
    }
}
