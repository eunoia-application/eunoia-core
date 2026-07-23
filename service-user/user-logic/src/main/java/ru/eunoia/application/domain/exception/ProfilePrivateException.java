package ru.eunoia.application.domain.exception;

/** Публичный профиль запрошен, но он приватный (settings.profileVisibility=PRIVATE) → 403. */
public class ProfilePrivateException extends RuntimeException {

    public ProfilePrivateException(String userId) {
        super("Профиль скрыт: " + userId);
    }
}
