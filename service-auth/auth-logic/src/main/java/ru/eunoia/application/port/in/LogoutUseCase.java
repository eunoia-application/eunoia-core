package ru.eunoia.application.port.in;

import java.util.UUID;

public interface LogoutUseCase {

    /** Разлогин: отзываем все активные refresh-токены пользователя. */
    void logout(UUID userId);
}
