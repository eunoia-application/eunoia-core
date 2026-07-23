package ru.eunoia.application.domain.exception;

/** Загружаемый аватар не прошёл проверку (пустой файл или недопустимый тип) → 400. */
public class InvalidAvatarException extends RuntimeException {

    public InvalidAvatarException(String message) {
        super(message);
    }
}
