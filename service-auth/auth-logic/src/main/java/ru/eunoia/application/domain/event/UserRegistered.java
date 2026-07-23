package ru.eunoia.application.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/** Пользователь зарегистрировался — по этому событию service-user заводит профиль. */
public record UserRegistered(UUID userId, String email, String username, LocalDateTime occurredAt)
        implements DomainEvent {
}
