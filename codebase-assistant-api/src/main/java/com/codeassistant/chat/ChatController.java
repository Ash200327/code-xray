package com.codeassistant.chat;

import com.codeassistant.api.UnauthorizedException;
import com.codeassistant.model.CreateMessageRequest;
import com.codeassistant.model.MessageView;
import com.codeassistant.domain.UserEntity;
import com.codeassistant.security.CurrentUserResolver;
import com.codeassistant.security.SecurityMode;
import com.codeassistant.service.ConversationService;
import com.codeassistant.service.RepositoryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final ConversationService conversationService;
    private final RepositoryService repositoryService;
    private final CurrentUserResolver currentUserResolver;
    private final SecurityMode securityMode;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(
            @RequestParam String question,
            @RequestParam(required = false) String repoUrl,
            @RequestParam(required = false) UUID conversationId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        log.info("Stream chat: question={}, conversationId={}", question, conversationId);
        UserEntity currentUser = currentUserResolver.resolve(authHeader).orElse(null);
        if (securityMode.isEnabled() && currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }
        String effectiveRepoUrl = resolveRepoUrl(repoUrl, conversationId, currentUser);
        List<String> memoryTurns = loadMemoryTurns(conversationId, currentUser);
        AtomicReference<StringBuilder> answerBuffer = new AtomicReference<>(new StringBuilder());
        AtomicReference<List<Map<String, Object>>> citationsBuffer = new AtomicReference<>(List.of());

        // Stream answer tokens
        Flux<ServerSentEvent<String>> answerStream = chatService.streamAnswer(question, effectiveRepoUrl, memoryTurns)
                .doOnNext(token -> answerBuffer.get().append(token))
                .map(token -> ServerSentEvent.<String>builder()
                        .event("message")
                        .data(token)
                        .build());

        // After answer completes, emit citations as a final event
        Flux<ServerSentEvent<String>> citationsEvent = Mono
                .fromCallable(() -> chatService.retrieveCitations(question, effectiveRepoUrl))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(citationsBuffer::set)
                .map(citations -> {
                    try {
                        String json = objectMapper.writeValueAsString(citations);
                        return ServerSentEvent.<String>builder()
                                .event("citations")
                                .data(json)
                                .build();
                    } catch (JsonProcessingException e) {
                        return ServerSentEvent.<String>builder()
                                .event("citations")
                                .data("[]")
                                .build();
                    }
                })
                .flux();

        // Done sentinel
        Flux<ServerSentEvent<String>> doneEvent = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build()
        );

        Mono<Void> persistMessages = Mono.fromRunnable(() ->
                persistConversationMessages(conversationId, question, answerBuffer.get().toString(), citationsBuffer.get(), currentUser)
        ).subscribeOn(Schedulers.boundedElastic()).then();

        return answerStream
                .concatWith(citationsEvent)
                .concatWith(persistMessages.thenMany(Flux.empty()))
                .concatWith(doneEvent)
                .onErrorResume(e -> {
                    log.error("Streaming error: {}", e.getMessage(), e);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data("An error occurred: " + e.getMessage())
                            .build());
                });
    }

    private String resolveRepoUrl(String repoUrl, UUID conversationId, UserEntity currentUser) {
        if (repoUrl != null && !repoUrl.isBlank()) {
            return repoUrl;
        }
        if (conversationId == null) {
            return null;
        }
        return conversationService.findConversation(conversationId, currentUser)
                .map(conversation -> repositoryService.get(conversation.getRepositoryId(), currentUser).getRepoUrl())
                .orElse(null);
    }

    private List<String> loadMemoryTurns(UUID conversationId, UserEntity currentUser) {
        if (conversationId == null) {
            return List.of();
        }
        return conversationService.getRecentMessages(conversationId, 8, currentUser).stream()
                .map(msg -> msg.getRole() + ": " + msg.getContent())
                .collect(Collectors.toList());
    }

    private void persistConversationMessages(UUID conversationId, String question, String answer, List<Map<String, Object>> citations, UserEntity currentUser) {
        if (conversationId == null) {
            return;
        }

        CreateMessageRequest userMsg = new CreateMessageRequest();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(question);
        conversationService.createMessage(userMsg, currentUser);

        CreateMessageRequest assistantMsg = new CreateMessageRequest();
        assistantMsg.setConversationId(conversationId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(answer.isBlank() ? "I could not generate a response." : answer);
        try {
            assistantMsg.setCitationsJson(objectMapper.writeValueAsString(citations));
        } catch (JsonProcessingException e) {
            assistantMsg.setCitationsJson("[]");
        }
        conversationService.createMessage(assistantMsg, currentUser);
    }
}
