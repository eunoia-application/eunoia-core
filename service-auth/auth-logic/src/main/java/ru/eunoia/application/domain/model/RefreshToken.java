package ru.eunoia.application.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/** Запись о выданном refresh-токене. Храним только хеш токена, не сам токен. */
public record RefreshToken(
        UUID id,
        UUID userId,
        String tokenHash,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean revoked,
        LocalDateTime revokedAt
) {

    /** Новая запись под свежевыданный токен (хеширует сырой токен). */
    public static RefreshToken issued(String rawToken, UUID userId, LocalDateTime expiresAt) {
        return new RefreshToken(null, userId, hash(rawToken), LocalDateTime.now(), expiresAt, false, null);
    }

    /** Отозванная копия — для ротации и logout. */
    public RefreshToken asRevoked() {
        return new RefreshToken(id, userId, tokenHash, createdAt, expiresAt, true, LocalDateTime.now());
    }

    public boolean isActive() {
        return !revoked && expiresAt.isAfter(LocalDateTime.now());
    }

    /** SHA-256 сырого токена в hex — по нему ищем запись в БД. */
    public static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 недоступен", e);
        }
    }
}
