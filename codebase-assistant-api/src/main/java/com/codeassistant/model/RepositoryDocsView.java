package com.codeassistant.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepositoryDocsView {
    private String readmeSummary;
    private String onboardingGuide;
    private String architectureSummary;
    private String apiSummary;
}

