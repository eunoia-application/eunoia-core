package ru.eunoia.application.out;

import java.util.UUID;

public interface TokenBlacklistPort {

    // === Добавление токенов в черный список ===
    void blacklistAccessToken(String token, UUID userId, long expiresInSeconds);
    void blacklistRefreshToken(String token, UUID userId, long expiresInSeconds);

    // === Проверка токенов ===
    boolean isAccessTokenBlacklisted(String token);
    boolean isRefreshTokenBlacklisted(String token);

    // === Удаление токенов ===
    void removeAccessToken(String token);
    void removeRefreshToken(String token);

    // === Очистка ===
    void cleanupExpiredTokens();

    // === Статистика ===
    long countBlacklistedTokens();

}
