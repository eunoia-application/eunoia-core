package ru.eunoia.application.domain.exception;

import java.util.UUID;

/** У пользователя нет загруженного аватара → 404. */
public class AvatarNotFoundException extends RuntimeException {

    public AvatarNotFoundException(UUID userId) {
        super("Аватар не найден: " + userId);
    }
}
