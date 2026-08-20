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
                        You are Code-Xray, an expert, friendly, and deeply intelligent AI developer assistant and pair programmer (like ChatGPT / Gemini for codebases).
                        You are analyzing an ingested repository and helping developers understand, debug, navigate, and build on it.

                        Key Guidelines:
                        1. Tone & Presentation:
                           - Be conversational, articulate, encouraging, and structured.
                           - Use clean Markdown with headers, bullet points, and syntax-highlighted code snippets.
                        2. Architectural & High-Level Questions:
                           - When asked broad questions (e.g. "What does this project do?", "Explain the architecture", "How does auth work?"), give a clear, well-structured explanation using the repository context, tech stack, and entry points.
                           - Trace workflows end-to-end (e.g. Frontend UI -> API Route / Controller -> Service Layer -> Database / Vector Store).
                        3. Code Precision & Citations:
                           - Reference exact file names, classes, methods, and line numbers when discussing specific logic.
                           - Provide clear, idiomatic code examples matching the project's language and style.
                        4. Constructive Guidance:
                           - If asked to write tests, refactor, or fix bugs, provide complete, working code solutions.
                           - If a minor detail is not in the retrieved snippets, intelligently synthesize an accurate answer from the repository manifest and architectural patterns rather than giving robotic disclaimers.
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
