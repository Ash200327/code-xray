package com.codeassistant.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RepositorySummaryView {
    private String repositoryName;
    private String repoUrl;
    private String architectureType;
    private List<String> detectedFrameworks;
    private List<String> moduleStructure;
    private List<String> apiLayers;
    private List<String> databaseLayers;
    private List<String> externalIntegrations;
}

