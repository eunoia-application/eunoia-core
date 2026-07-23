package ru.eunoia.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

/** Событие из auth (топик user.deleted). */
public record UserDeletedEvent(UUID userId, LocalDateTime occurredAt) {
}
