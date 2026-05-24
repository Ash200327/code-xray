package com.codeassistant.ingestion;

import com.codeassistant.api.ResourceNotFoundException;
import com.codeassistant.domain.IngestionJobEntity;
import com.codeassistant.model.IngestRequest;
import com.codeassistant.repository.IngestionJobRepository;
import com.codeassistant.repository.RepositoryEntityRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IngestionJobServiceTest {

    private IngestionJobService ingestionJobService;
    private IngestionJobRepository ingestionJobRepository;

    @BeforeEach
    void setUp() throws Exception {
        IngestionService ingestionService = mock(IngestionService.class);
        Executor sameThreadExecutor = Runnable::run;
        ingestionJobRepository = mock(IngestionJobRepository.class);
        RepositoryEntityRepository repositoryRepository = mock(RepositoryEntityRepository.class);

        when(repositoryRepository.findTopByRepoUrlAndBranchOrderByUpdatedAtDesc(any(), any())).thenReturn(Optional.empty());
        when(ingestionJobRepository.save(any(IngestionJobEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ingestionJobService = new IngestionJobService(
                ingestionService,
                sameThreadExecutor,
                ingestionJobRepository,
                repositoryRepository,
                new ObjectMapper(),
                mock(IngestionProgressPublisher.class)
        );

        Field attemptsField = IngestionJobService.class.getDeclaredField("defaultMaxAttempts");
        attemptsField.setAccessible(true);
        attemptsField.setInt(ingestionJobService, 2);
    }

    @Test
    void shouldRejectEmptyRepoUrl() {
        IngestRequest request = new IngestRequest();
        request.setRepoUrl(" ");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> ingestionJobService.createJob(request, null));
        assertTrue(error.getMessage().contains("repoUrl"));
    }

    @Test
    void shouldThrowNotFoundForUnknownJob() {
        UUID missingId = UUID.randomUUID();
        when(ingestionJobRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> ingestionJobService.getJob(missingId, null));
    }
}
