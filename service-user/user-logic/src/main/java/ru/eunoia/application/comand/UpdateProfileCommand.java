package ru.eunoia.application.comand;

/** Обновление отображаемых данных профиля (PUT /users/me). Все поля опциональны. */
public record UpdateProfileCommand(String firstName, String lastName, String bio, String avatarUrl) {
}
