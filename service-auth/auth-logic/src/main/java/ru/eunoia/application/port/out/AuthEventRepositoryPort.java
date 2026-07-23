package ru.eunoia.application.port.out;

import ru.eunoia.application.domain.model.AuthEvent;

/** Хранилище событий аудита (вход, регистрация, сброс пароля и т.п.). */
public interface AuthEventRepositoryPort {

    AuthEvent save(AuthEvent event);
}
