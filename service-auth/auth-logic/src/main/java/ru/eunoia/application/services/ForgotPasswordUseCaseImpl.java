package ru.eunoia.application.services;

import java.time.Duration;
import java.time.LocalDateTime;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.domain.model.OneTimeToken;
import ru.eunoia.application.port.in.ForgotPasswordUseCase;
import ru.eunoia.application.port.out.EmailSenderPort;
import ru.eunoia.application.port.out.OneTimeTokenRepositoryPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

/**
 * Запрос сброса пароля: если email есть — генерим одноразовый токен и «шлём» письмо.
 * Наличие аккаунта наружу не раскрываем — контроллер в любом случае отвечает 204.
 */
public class ForgotPasswordUseCaseImpl implements ForgotPasswordUseCase {

    private final UserRepositoryPort users;
    private final OneTimeTokenRepositoryPort tokens;
    private final EmailSenderPort emailSender;
    private final AuthEventRecorder recorder;
    private final Duration tokenTtl;

    public ForgotPasswordUseCaseImpl(UserRepositoryPort users,
                                     OneTimeTokenRepositoryPort tokens,
                                     EmailSenderPort emailSender,
                                     AuthEventRecorder recorder,
                                     Duration tokenTtl) {
        this.users = users;
        this.tokens = tokens;
        this.emailSender = emailSender;
        this.recorder = recorder;
        this.tokenTtl = tokenTtl;
    }

    @Override
    public void requestReset(String email) {
        users.findByEmail(email).ifPresent(user -> {
            String raw = OneTimeToken.newRawToken();
            tokens.save(OneTimeToken.issued(raw, user.id(),
                    OneTimeToken.Purpose.PASSWORD_RESET, LocalDateTime.now().plus(tokenTtl)));
            emailSender.sendPasswordReset(user.email(), raw);
            recorder.record(user.id(), AuthEvent.EventType.PASSWORD_RESET_REQUESTED, true);
        });
    }
}
