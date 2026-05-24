package com.codeassistant.controller;

import com.codeassistant.api.ApiResponse;
import com.codeassistant.api.UnauthorizedException;
import com.codeassistant.domain.UserEntity;
import com.codeassistant.model.RepositoryDocsView;
import com.codeassistant.model.RepositorySummaryView;
import com.codeassistant.security.CurrentUserResolver;
import com.codeassistant.security.SecurityMode;
import com.codeassistant.service.RepositoryInsightsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

@RestController
@RequestMapping("/api/repositories/{repositoryId}")
@RequiredArgsConstructor
public class RepositoryInsightsController {

    private final RepositoryInsightsService repositoryInsightsService;
    private final CurrentUserResolver currentUserResolver;
    private final SecurityMode securityMode;

    @GetMapping("/summary")
    public Mono<ResponseEntity<ApiResponse<RepositorySummaryView>>> summary(
            @PathVariable UUID repositoryId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return Mono.fromCallable(() -> repositoryInsightsService.generateSummary(repositoryId, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok(ApiResponse.ok(res)));
    }

    @GetMapping("/docs")
    public Mono<ResponseEntity<ApiResponse<RepositoryDocsView>>> docs(
            @PathVariable UUID repositoryId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return Mono.fromCallable(() -> repositoryInsightsService.generateDocs(repositoryId, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok(ApiResponse.ok(res)));
    }

    private UserEntity requireUserIfNeeded(UserEntity user) {
        if (securityMode.isEnabled() && user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }
}

