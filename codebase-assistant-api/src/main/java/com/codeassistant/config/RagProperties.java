package com.codeassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "codeassistant.rag")
public class RagProperties {

    private int topK = 8;
    private double similarityThreshold = 0.2;
    private double rankDecay = 0.08;
    private double hybridBoost = 0.15;
    private int maxContextChunks = 5;
    private int memoryTurnsCount = 8;
}
