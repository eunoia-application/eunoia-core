package ru.eunoia.application.services;

import java.util.UUID;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.port.in.LogoutUseCase;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;

/** Разлогин: гасим refresh-токены пользователя (access истечёт сам), пишем LOGOUT. Без Spring. */
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final AuthEventRecorder recorder;

    public LogoutUseCaseImpl(RefreshTokenRepositoryPort refreshTokenRepository,
                             AuthEventRecorder recorder) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.recorder = recorder;
    }

    @Override
    public void logout(UUID userId) {
        refreshTokenRepository.revokeByUserId(userId);
        recorder.record(userId, AuthEvent.EventType.LOGOUT, true);
    }
}
