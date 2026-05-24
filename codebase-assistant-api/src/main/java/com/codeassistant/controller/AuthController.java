package com.codeassistant.controller;

import com.codeassistant.api.ApiResponse;
import com.codeassistant.model.*;
import com.codeassistant.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public Mono<ResponseEntity<ApiResponse<AuthResponse>>> signup(@Valid @RequestBody SignUpRequest request) {
        return Mono.fromCallable(() -> authService.signup(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok(ApiResponse.ok(res)));
    }

    @PostMapping("/login")
    public Mono<ResponseEntity<ApiResponse<AuthResponse>>> login(@Valid @RequestBody LoginRequest request) {
        return Mono.fromCallable(() -> authService.login(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok(ApiResponse.ok(res)));
    }

    @PostMapping("/refresh")
    public Mono<ResponseEntity<ApiResponse<AuthResponse>>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return Mono.fromCallable(() -> authService.refresh(request))
                .subscribeOn(Schedulers.boundedElastic())
                .map(res -> ResponseEntity.ok(ApiResponse.ok(res)));
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<ApiResponse<UserView>>> me(@RequestHeader("Authorization") String authHeader) {
        return Mono.fromCallable(() -> {
                    String token = authHeader.replace("Bearer ", "").trim();
                    return authService.currentUser(token);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .map(user -> ResponseEntity.ok(ApiResponse.ok(user)));
    }
}

