package ru.eunoia.security;

import java.util.UUID;

/** Текущий аутентифицированный пользователь (из access-токена). Реализуется в auth-app. */
public interface CurrentUser {

    UUID id();
}
