package ru.eunoia.application.domain.model;

/** Результат аутентификации: сам пользователь и выданные ему токены. */
public record Authentication(User user, AuthTokens tokens) {
}
