package com.codeassistant.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ConversationView {
    private UUID id;
    private UUID repositoryId;
    private String title;
    private Instant createdAt;
    private Instant updatedAt;
}

