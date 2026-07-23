package ru.eunoia.application.domain.model;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Одноразовый токен email-флоу: подтверждение почты и сброс пароля (различаются Purpose).
 * Как и refresh — храним только хеш, сам токен уходит письмом. Одноразовый: после использования гасится.
 */
public record OneTimeToken(
        UUID id,
        UUID userId,
        Purpose purpose,
        String tokenHash,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        boolean used,
        LocalDateTime usedAt
) {

    public enum Purpose { EMAIL_VERIFICATION, PASSWORD_RESET }

    /** Новая запись под свежесгенерённую строку (хеширует её). */
    public static OneTimeToken issued(String rawToken, UUID userId, Purpose purpose, LocalDateTime expiresAt) {
        return new OneTimeToken(null, userId, purpose, hash(rawToken),
                LocalDateTime.now(), expiresAt, false, null);
    }

    /** Копия, помеченная использованной — токен одноразовый. */
    public OneTimeToken asUsed() {
        return new OneTimeToken(id, userId, purpose, tokenHash, createdAt, expiresAt, true, LocalDateTime.now());
    }

    public boolean isActive() {
        return !used && expiresAt.isAfter(LocalDateTime.now());
    }

    public boolean isFor(Purpose expected) {
        return purpose == expected;
    }

    /** Случайная URL-безопасная строка — её кладём в письмо, в БД идёт только её хеш. */
    public static String newRawToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
