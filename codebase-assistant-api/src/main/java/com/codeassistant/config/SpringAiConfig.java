package com.codeassistant.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SpringAiConfig implements WebMvcConfigurer {

    /**
     * Comma-separated list of allowed CORS origins.
     * Defaults to local dev servers; override via ALLOWED_ORIGINS env var in production.
     * Example: ALLOWED_ORIGINS=https://code-xray.vercel.app,https://www.code-xray.com
     */
    @Value("${allowed.origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are an expert AI developer assistant.
                        Your answers must be direct, technically precise, and highly detailed.
                        Avoid high-level summaries or hand-waving explanations (e.g., do not say "likely found in" or "typically involves").
                        Use the provided <context> blocks (which contain code from the repository) to trace flows, extract DTO schemas, and identify actual database columns or service methods.
                        Cite exact files, line numbers, classes, and methods.
                        Leverage your deep knowledge of Java, Spring Boot, React, TypeScript, and SQL to explain the logic and connect the dots between files.
                        Provide clear, production-grade code snippets matching the codebase style where helpful.
                        """)
                .build();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*");
    }
}
