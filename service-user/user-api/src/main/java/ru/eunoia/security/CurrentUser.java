package ru.eunoia.security;

import java.util.UUID;

/** Текущий пользователь из JWT (userId кладёт auth в claim). Реализация — в user-app. */
public interface CurrentUser {

    UUID id();
}
