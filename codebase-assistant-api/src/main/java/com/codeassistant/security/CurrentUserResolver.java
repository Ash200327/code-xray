package com.codeassistant.security;

import com.codeassistant.domain.UserEntity;
import com.codeassistant.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    public Optional<UserEntity> resolve(String authHeader) {
        if (authHeader == null || authHeader.isBlank() || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        try {
            String token = authHeader.substring("Bearer ".length()).trim();
            Jwt jwt = jwtTokenService.decode(token);
            UUID userId = UUID.fromString(jwt.getSubject());
            return userRepository.findById(userId);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}

