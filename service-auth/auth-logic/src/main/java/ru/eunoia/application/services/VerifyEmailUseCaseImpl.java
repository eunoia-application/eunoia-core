package ru.eunoia.application.services;

import ru.eunoia.application.domain.exception.TokenValidationException;
import ru.eunoia.application.domain.exception.UserNotFoundException;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.domain.model.OneTimeToken;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.in.VerifyEmailUseCase;
import ru.eunoia.application.port.out.OneTimeTokenRepositoryPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

/** Подтверждение email: по токену из письма ставим emailVerified=true и гасим токен. */
public class VerifyEmailUseCaseImpl implements VerifyEmailUseCase {

    private final OneTimeTokenRepositoryPort tokens;
    private final UserRepositoryPort users;
    private final AuthEventRecorder recorder;

    public VerifyEmailUseCaseImpl(OneTimeTokenRepositoryPort tokens,
                                  UserRepositoryPort users,
                                  AuthEventRecorder recorder) {
        this.tokens = tokens;
        this.users = users;
        this.recorder = recorder;
    }

    @Override
    public void verify(String rawToken) {
        OneTimeToken token = tokens.findByTokenHash(OneTimeToken.hash(rawToken))
                .filter(t -> t.isFor(OneTimeToken.Purpose.EMAIL_VERIFICATION))
                .orElseThrow(() -> new TokenValidationException("Токен подтверждения не найден"));
        if (!token.isActive()) {
            throw new TokenValidationException("Токен подтверждения истёк или уже использован");
        }

        User user = users.findById(token.userId())
                .orElseThrow(() -> new UserNotFoundException(token.userId().toString()));
        users.save(user.withEmailVerified());
        tokens.save(token.asUsed());
        recorder.record(user.id(), AuthEvent.EventType.EMAIL_VERIFICATION_SUCCESS, true);
    }
}
