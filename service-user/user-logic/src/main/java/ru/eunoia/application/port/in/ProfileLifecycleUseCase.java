package ru.eunoia.application.port.in;

import java.util.UUID;

/**
 * Профиль следует за жизненным циклом аккаунта в auth: создаём при регистрации,
 * удаляем при удалении аккаунта. Вызывается из Kafka-консюмера событий auth.
 */
public interface ProfileLifecycleUseCase {

    void onUserRegistered(UUID userId, String email, String username);

    void onUserDeleted(UUID userId);
}
