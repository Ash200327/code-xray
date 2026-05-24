package com.codeassistant.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class RepositoryView {
    private UUID id;
    private UUID workspaceId;
    private String name;
    private String repoUrl;
    private String branch;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastIngestedAt;
}

