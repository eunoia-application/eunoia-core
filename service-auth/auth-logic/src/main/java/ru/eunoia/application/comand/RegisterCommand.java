package ru.eunoia.application.comand;

/** Данные для регистрации. username хранит auth (это часть логина), профиль — в service-user. */
public record RegisterCommand(
        String email,
        String password,
        String username
) {
}
