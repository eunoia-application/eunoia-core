package ru.eunoia.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

/** Событие из auth (топик user.registered). Поля совпадают с auth-стороной по JSON. */
public record UserRegisteredEvent(UUID userId, String email, String username, LocalDateTime occurredAt) {
}
