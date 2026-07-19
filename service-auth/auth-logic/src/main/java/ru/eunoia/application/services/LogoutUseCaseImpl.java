package ru.eunoia.application.services;

import java.util.UUID;
import ru.eunoia.application.port.in.LogoutUseCase;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;

/** Разлогин: гасим refresh-токены пользователя (access истечёт сам). Без Spring. */
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepository;

    public LogoutUseCaseImpl(RefreshTokenRepositoryPort refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public void logout(UUID userId) {
        refreshTokenRepository.revokeByUserId(userId);
    }
}
