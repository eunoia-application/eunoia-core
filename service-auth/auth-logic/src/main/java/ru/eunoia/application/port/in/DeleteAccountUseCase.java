package ru.eunoia.application.port.in;

import java.util.UUID;

public interface DeleteAccountUseCase {

    /** Удалить свой аккаунт: гасим токены, убираем identity, шлём UserDeleted для каскада на профиль. */
    void delete(UUID userId);
}
