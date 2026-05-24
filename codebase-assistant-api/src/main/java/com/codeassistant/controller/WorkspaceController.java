package com.codeassistant.controller;

import com.codeassistant.api.ApiResponse;
import com.codeassistant.api.UnauthorizedException;
import com.codeassistant.domain.UserEntity;
import com.codeassistant.model.CreateWorkspaceRequest;
import com.codeassistant.model.WorkspaceView;
import com.codeassistant.security.CurrentUserResolver;
import com.codeassistant.security.SecurityMode;
import com.codeassistant.service.WorkspaceService;
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
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final CurrentUserResolver currentUserResolver;
    private final SecurityMode securityMode;

    @PostMapping
    public Mono<ResponseEntity<ApiResponse<WorkspaceView>>> create(@Valid @RequestBody CreateWorkspaceRequest request,
                                                                   @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return Mono.fromCallable(() -> workspaceService.create(request, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(view -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(view)));
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<List<WorkspaceView>>>> list(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return Mono.fromCallable(() -> workspaceService.list(currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(list -> ResponseEntity.ok(ApiResponse.ok(list)));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<WorkspaceView>>> get(@PathVariable UUID id,
                                                                @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return Mono.fromCallable(() -> workspaceService.get(id, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(view -> ResponseEntity.ok(ApiResponse.ok(view)));
    }

    @PutMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<WorkspaceView>>> update(@PathVariable UUID id,
                                                                   @Valid @RequestBody CreateWorkspaceRequest request,
                                                                   @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return Mono.fromCallable(() -> workspaceService.update(id, request, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .map(view -> ResponseEntity.ok(ApiResponse.ok(view)));
    }

    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<ApiResponse<Void>>> delete(@PathVariable UUID id,
                                                          @RequestHeader(value = "Authorization", required = false) String authHeader) {
        final UserEntity currentUser = requireUserIfNeeded(currentUserResolver.resolve(authHeader).orElse(null));
        return Mono.fromRunnable(() -> workspaceService.delete(id, currentUser))
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(ResponseEntity.ok(ApiResponse.ok(null)));
    }

    private UserEntity requireUserIfNeeded(UserEntity user) {
        if (securityMode.isEnabled() && user == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return user;
    }
}
