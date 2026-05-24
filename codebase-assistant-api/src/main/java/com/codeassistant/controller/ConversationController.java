package com.codeassistant.controller;

import com.codeassistant.api.ApiResponse;
import com.codeassistant.api.UnauthorizedException;
import com.codeassistant.domain.UserEntity;
import com.codeassistant.model.*;
import com.codeassistant.security.CurrentUserResolver;
import com.codeassistant.security.SecurityMode;
import com.codeassistant.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final CurrentUserResolver currentUserResolver;
    private final SecurityMode securityMode;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<ConversationView>>> create(@Valid @RequestBody CreateConversationRequest request,
                                                                      @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return Mono.fromCallable(() -> conversationService.createConversation(request, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(view -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(view)));
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<ConversationView>>>> list(@RequestParam(required = false) UUID repositoryId,
                                                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return Mono.fromCallable(() -> conversationService.listConversations(repositoryId, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(list -> ResponseEntity.ok(ApiResponse.ok(list)));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<ConversationView>>> rename(@PathVariable UUID id,
                                                                      @Valid @RequestBody CreateConversationRequest request,
                                                                      @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return Mono.fromCallable(() -> conversationService.renameConversation(id, request, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(view -> ResponseEntity.ok(ApiResponse.ok(view)));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> delete(@PathVariable UUID id,
                                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return Mono.fromRunnable(() -> conversationService.deleteConversation(id, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(ResponseEntity.ok(ApiResponse.ok(null)));
    }

    @PostMapping("/{id}/messages")
    public Mono<ResponseEntity<ApiResponse<MessageView>>> createMessage(@PathVariable UUID id,
                                                                        @Valid @RequestBody CreateMessageRequest request,
                                                                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        request.setConversationId(id);
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return Mono.fromCallable(() -> conversationService.createMessage(request, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(view -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(view)));
    }

    @GetMapping("/{id}/messages")
    public Mono<ResponseEntity<ApiResponse<List<MessageView>>>> listMessages(@PathVariable UUID id,
                                                                             @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return Mono.fromCallable(() -> conversationService.listMessages(id, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(list -> ResponseEntity.ok(ApiResponse.ok(list)));
    }

    private UserEntity requireUserIfNeeded(UserEntity user) {
        if (securityMode.isEnabled() && user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }
}
