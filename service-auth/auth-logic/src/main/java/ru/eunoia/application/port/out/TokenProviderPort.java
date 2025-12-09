package ru.eunoia.application.port.out;

import java.util.UUID;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.User;

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
