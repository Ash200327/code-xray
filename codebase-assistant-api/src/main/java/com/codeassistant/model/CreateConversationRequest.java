package com.codeassistant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateConversationRequest {
    @NotNull
    private UUID repositoryId;

    @NotBlank
    @Size(max = 250)
    private String title;
}

