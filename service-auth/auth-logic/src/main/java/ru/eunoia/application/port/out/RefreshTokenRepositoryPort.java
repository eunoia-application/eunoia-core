package ru.eunoia.application.port.out;

import java.util.Optional;
import java.util.UUID;
import ru.eunoia.application.domain.model.RefreshToken;

/** Хранилище выданных refresh-токенов (для ротации и отзыва). */
public interface RefreshTokenRepositoryPort {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Отозвать все активные токены пользователя (logout / смена пароля). */
    void revokeByUserId(UUID userId);
}
