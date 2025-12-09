package ru.eunoia.services;

import java.util.concurrent.CompletableFuture;
import ru.eunoia.domain.model.AuthTokens;
import ru.eunoia.domain.model.User;

public interface AuthenticationService {

    /**
     * Регистрация нового пользователя
     */
    CompletableFuture<AuthTokens> register(
            String email,
            String password,
            String username,
            String firstName,
            String lastName
    );

    /**
     * Аутентификация пользователя
     */
    CompletableFuture<AuthTokens> login(String email, String password);

    /**
     * Обновление access токена
     */
    CompletableFuture<AuthTokens> refreshToken(String refreshToken);

    /**
     * Выход пользователя (отзыв токенов)
     */
    CompletableFuture<Void> logout(String refreshToken);

    /**
     * Валидация access токена
     */
    CompletableFuture<User> validateToken(String accessToken);

    /**
     * Запрос на сброс пароля
     */
    CompletableFuture<Void> requestPasswordReset(String email);

    /**
     * Сброс пароля с токеном
     */
    CompletableFuture<Void> resetPassword(String token, String newPassword);

    /**
     * Подтверждение email
     */
    CompletableFuture<Void> verifyEmail(String token);

    /**
     * Смена пароля (авторизованный пользователь)
     */
    CompletableFuture<Void> changePassword(
            String currentPassword,
            String newPassword,
            User currentUser
    );
}