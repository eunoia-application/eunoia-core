package ru.eunoia.application.out;

import java.util.UUID;
import ru.eunoia.domain.model.AuthTokens;
import ru.eunoia.domain.model.User;

public interface TokenProviderPort {

    /**
     * Генерация access и refresh токенов
     */
    AuthTokens generateTokens(User user);

    /**
     * Валидация access токена
     */
    boolean validateAccessToken(String token);

    /**
     * Валидация refresh токена
     */
    boolean validateRefreshToken(String token);

    /**
     * Извлечение userId из токена
     */
    UUID extractUserId(String token);

    /**
     * Извлечение email из токена
     */
    String extractEmail(String token);

    /**
     * Извлечение username из токена
     */
    String extractUsername(String token);

}
