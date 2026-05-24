package com.codeassistant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateMessageRequest {
    @NotNull
    private UUID conversationId;

    @Pattern(regexp = "user|assistant")
    private String role;

    @NotBlank
    private String content;

    private String citationsJson;
}

