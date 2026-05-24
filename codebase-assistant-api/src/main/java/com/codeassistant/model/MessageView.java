package com.codeassistant.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class MessageView {
    private UUID id;
    private UUID conversationId;
    private String role;
    private String content;
    private String citationsJson;
    private Instant createdAt;
}

