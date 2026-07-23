package ru.eunoia.application.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/** Аккаунт удалён — по этому событию сервисы удаляют данные пользователя. */
public record UserDeleted(UUID userId, LocalDateTime occurredAt) implements DomainEvent {
}
