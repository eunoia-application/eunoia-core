package ru.eunoia.application.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Свежевыданная пара access + refresh токенов с метаданными. Неизменяемый value object;
 * сами строки токенов делает адаптер {@code TokenProviderPort}.
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
