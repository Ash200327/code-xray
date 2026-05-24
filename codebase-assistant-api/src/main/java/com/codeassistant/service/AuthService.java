package com.codeassistant.service;

import com.codeassistant.domain.RefreshTokenEntity;
import com.codeassistant.domain.UserEntity;
import com.codeassistant.model.*;
import com.codeassistant.repository.RefreshTokenRepository;
import com.codeassistant.repository.UserRepository;
import com.codeassistant.security.JwtTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    private static final long REFRESH_TOKEN_SECONDS = 60L * 60 * 24 * 14;

    public AuthResponse signup(SignUpRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setEmail(email);
        user.setDisplayName(request.getDisplayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user = userRepository.save(user);

        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return issueTokens(user);
    }

    public AuthResponse refresh(RefreshTokenRequest request) {
        String rawToken = request.getRefreshToken().trim();
        String tokenHash = hashToken(rawToken);

        RefreshTokenEntity refresh = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (refresh.isRevoked() || refresh.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        refresh.setRevoked(true);
        refreshTokenRepository.save(refresh);
        return issueTokens(refresh.getUser());
    }

    public UserView currentUser(String accessToken) {
        Jwt jwt = jwtTokenService.decode(accessToken);
        UUID userId = UUID.fromString(jwt.getSubject());
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toUserView(user);
    }

    private AuthResponse issueTokens(UserEntity user) {
        String accessToken = jwtTokenService.createAccessToken(user);
        String refreshRaw = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshTokenEntity refresh = new RefreshTokenEntity();
        refresh.setId(UUID.randomUUID());
        refresh.setUser(user);
        refresh.setTokenHash(hashToken(refreshRaw));
        refresh.setExpiresAt(Instant.now().plusSeconds(REFRESH_TOKEN_SECONDS));
        refresh.setRevoked(false);
        refreshTokenRepository.save(refresh);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshRaw)
                .tokenType("Bearer")
                .expiresInSeconds(jwtTokenService.getAccessTokenSeconds())
                .user(toUserView(user))
                .build();
    }

    private UserView toUserView(UserEntity user) {
        return UserView.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .build();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Could not hash token", e);
        }
    }
}

