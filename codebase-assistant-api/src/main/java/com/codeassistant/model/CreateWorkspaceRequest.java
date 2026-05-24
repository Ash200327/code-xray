package com.codeassistant.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateWorkspaceRequest {
    @NotBlank
    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String description;
}

