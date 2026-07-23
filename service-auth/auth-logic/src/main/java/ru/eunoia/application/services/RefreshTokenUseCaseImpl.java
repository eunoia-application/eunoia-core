package ru.eunoia.application.services;

import java.util.UUID;
import ru.eunoia.application.domain.exception.TokenValidationException;
import ru.eunoia.application.domain.exception.UserNotFoundException;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.domain.model.Authentication;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.RefreshToken;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.in.RefreshTokenUseCase;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.TokenProviderPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

/** Обновление токенов с ротацией: старый refresh гасим, выдаём и записываем новую пару, пишем TOKEN_REFRESH. */
public class RefreshTokenUseCaseImpl implements RefreshTokenUseCase {

    private final UserRepositoryPort userRepository;
    private final TokenProviderPort tokenProvider;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final AuthEventRecorder recorder;

    public RefreshTokenUseCaseImpl(UserRepositoryPort userRepository,
                                   TokenProviderPort tokenProvider,
                                   RefreshTokenRepositoryPort refreshTokenRepository,
                                   AuthEventRecorder recorder) {
        this.userRepository = userRepository;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.recorder = recorder;
    }

    @Override
    public Authentication refresh(String refreshToken) {
        if (!tokenProvider.validateRefreshToken(refreshToken)) {
            throw new TokenValidationException("Невалидный refresh-токен");
        }

        RefreshToken stored = refreshTokenRepository.findByTokenHash(RefreshToken.hash(refreshToken))
                .orElseThrow(() -> new TokenValidationException("Refresh-токен не найден"));
        if (!stored.isActive()) {
            throw new TokenValidationException("Refresh-токен отозван или истёк");
        }

        UUID userId = tokenProvider.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        user.assertCanAuthenticate();

        // ротация: гасим старый, выдаём новую пару и сохраняем новый refresh
        refreshTokenRepository.save(stored.asRevoked());
        AuthTokens tokens = tokenProvider.generateTokens(user);
        refreshTokenRepository.save(
                RefreshToken.issued(tokens.refreshToken(), user.id(), tokens.refreshTokenExpiresAt()));
        recorder.record(user.id(), AuthEvent.EventType.TOKEN_REFRESH, true);

        return new Authentication(user, tokens);
    }
}
