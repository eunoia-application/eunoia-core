package ru.eunoia.application.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A freshly issued pair of access + refresh tokens plus their metadata.
 * Immutable value object; the token strings are produced by a {@code TokenProviderPort} adapter.
 */
public record AuthTokens(
        String accessToken,
        String refreshToken,
        String tokenType,
        int expiresIn,
        UUID userId,
        LocalDateTime issuedAt,
        LocalDateTime accessTokenExpiresAt,
        LocalDateTime refreshTokenExpiresAt
) {

    public static final String BEARER = "Bearer";
}
