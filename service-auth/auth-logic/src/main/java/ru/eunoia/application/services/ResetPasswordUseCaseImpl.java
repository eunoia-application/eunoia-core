package ru.eunoia.application.services;

import ru.eunoia.application.domain.exception.TokenValidationException;
import ru.eunoia.application.domain.exception.UserNotFoundException;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.domain.model.OneTimeToken;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.in.ResetPasswordUseCase;
import ru.eunoia.application.port.out.OneTimeTokenRepositoryPort;
import ru.eunoia.application.port.out.PasswordEncoderPort;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

/** Сброс пароля по токену: меняем хеш, гасим токен и все refresh-сессии пользователя. */
public class ResetPasswordUseCaseImpl implements ResetPasswordUseCase {

    private final OneTimeTokenRepositoryPort tokens;
    private final UserRepositoryPort users;
    private final PasswordEncoderPort passwordEncoder;
    private final RefreshTokenRepositoryPort refreshTokens;
    private final AuthEventRecorder recorder;

    public ResetPasswordUseCaseImpl(OneTimeTokenRepositoryPort tokens,
                                    UserRepositoryPort users,
                                    PasswordEncoderPort passwordEncoder,
                                    RefreshTokenRepositoryPort refreshTokens,
                                    AuthEventRecorder recorder) {
        this.tokens = tokens;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokens = refreshTokens;
        this.recorder = recorder;
    }

    @Override
    public void reset(String rawToken, String newPassword) {
        OneTimeToken token = tokens.findByTokenHash(OneTimeToken.hash(rawToken))
                .filter(t -> t.isFor(OneTimeToken.Purpose.PASSWORD_RESET))
                .orElseThrow(() -> new TokenValidationException("Токен сброса не найден"));
        if (!token.isActive()) {
            throw new TokenValidationException("Токен сброса истёк или уже использован");
        }

        User user = users.findById(token.userId())
                .orElseThrow(() -> new UserNotFoundException(token.userId().toString()));
        users.save(user.withPasswordHash(passwordEncoder.encode(newPassword)));
        tokens.save(token.asUsed());
        refreshTokens.revokeByUserId(user.id()); // сменили пароль — рвём все активные сессии
        recorder.record(user.id(), AuthEvent.EventType.PASSWORD_RESET_SUCCESS, true);
    }
}
