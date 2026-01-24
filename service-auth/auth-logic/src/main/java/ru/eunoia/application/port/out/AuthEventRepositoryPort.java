package ru.eunoia.application.port.out;

import java.util.List;
import java.util.UUID;
import ru.eunoia.application.domain.model.AuthEvent;

public interface AuthEventRepositoryPort {

    // === CRUD операции ===
    AuthEvent save(AuthEvent event);
    void deleteById(UUID id);

    // === Поиск по пользователю ===
    List<AuthEvent> findByUserId(UUID userId);
    List<AuthEvent> findByUserId(UUID userId, int limit);
    List<AuthEvent> findByUserIdAndEventType(UUID userId, AuthEvent.EventType eventType);
    List<AuthEvent> findByUserIdAndSuccess(UUID userId, boolean success);

    // === Поиск по типу события ===
    List<AuthEvent> findByEventType(AuthEvent.EventType eventType);
    List<AuthEvent> findByEventType(AuthEvent.EventType eventType, int limit);
    List<AuthEvent> findByEventTypeAndSuccess(AuthEvent.EventType eventType, boolean success);

    // === Поиск по времени ===
    List<AuthEvent> findRecentEvents(int limit);
    List<AuthEvent> findEventsBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
    List<AuthEvent> findEventsBefore(java.time.LocalDateTime dateTime);
    List<AuthEvent> findEventsAfter(java.time.LocalDateTime dateTime);

    // === Бизнес-операции ===
    void deleteOldEvents(int days);
    void deleteEventsByUserId(UUID userId);

    // === Статистика ===
    long countByUserId(UUID userId);
    long countByEventType(AuthEvent.EventType eventType);
    long countByUserIdAndEventType(UUID userId, AuthEvent.EventType eventType);
    long countByUserIdAndSuccess(UUID userId, boolean success);

}
