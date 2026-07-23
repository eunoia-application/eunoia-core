package ru.eunoia.application.domain.exception;

public class ProfileNotFoundException extends RuntimeException {

    public ProfileNotFoundException(String userId) {
        super("Профиль не найден: " + userId);
    }
}
