package com.codeassistant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateRepositoryRequest {
    @NotNull
    private UUID workspaceId;

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    @Size(max = 500)
    private String repoUrl;

    @Size(max = 120)
    private String branch = "main";
}

