package com.codeassistant.model;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class IngestRequest {
    @NotBlank
    private String repoUrl;
    private String branch = "main";
}
