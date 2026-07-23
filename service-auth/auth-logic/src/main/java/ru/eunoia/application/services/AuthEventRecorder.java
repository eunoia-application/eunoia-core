package ru.eunoia.application.services;

import java.time.LocalDateTime;
import java.util.UUID;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.port.out.AuthEventRepositoryPort;

/**
 * Одна точка записи событий аутентификации в аудит — чтобы use case'ы не дублировали сборку AuthEvent.
 * Контекст (ip, user-agent и т.п.) пока не пишем: это M5, когда события начнём обогащать из запроса.
 */
public class AuthEventRecorder {

    private final AuthEventRepositoryPort repository;

    public AuthEventRecorder(AuthEventRepositoryPort repository) {
        this.repository = repository;
    }

    public void record(UUID userId, AuthEvent.EventType type, boolean success) {
        repository.save(AuthEvent.builder()
                .userId(userId)
                .eventType(type)
                .success(success)
                .createdAt(LocalDateTime.now())
                .build());
    }
}
