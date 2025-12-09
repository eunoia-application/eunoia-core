package ru.eunoia.application.port.out;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import ru.eunoia.application.domain.model.User;

public interface UserServicePort {

    // === Поиск пользователей ===
    CompletableFuture<Optional<User>> findUserById(UUID userId);
    CompletableFuture<Optional<User>> findUserByEmail(String email);
    CompletableFuture<Optional<User>> findUserByUsername(String username);

    // === Проверка существования ===
    CompletableFuture<Boolean> userExistsByEmail(String email);
    CompletableFuture<Boolean> userExistsByUsername(String username);

    // === Создание и обновление ===
    CompletableFuture<User> createUser(User user);
    CompletableFuture<User> updateUser(User user);

    // === Работа с паролями ===
    CompletableFuture<Boolean> validatePassword(UUID userId, String rawPassword);
    CompletableFuture<Void> updatePassword(UUID userId, String newPasswordHash);

    // === Блокировка/активация ===
    CompletableFuture<Void> lockUser(UUID userId);
    CompletableFuture<Void> unlockUser(UUID userId);
    CompletableFuture<Void> activateUser(UUID userId);
    CompletableFuture<Void> deactivateUser(UUID userId);

    // === Статистика ===
    CompletableFuture<Void> updateLastLogin(UUID userId);
    CompletableFuture<Void> incrementFailedLoginAttempts(UUID userId);
    CompletableFuture<Void> resetFailedLoginAttempts(UUID userId);

}
