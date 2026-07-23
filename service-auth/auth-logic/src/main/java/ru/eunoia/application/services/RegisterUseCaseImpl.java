package ru.eunoia.application.services;

import java.time.Duration;
import java.time.LocalDateTime;
import ru.eunoia.application.comand.RegisterCommand;
import ru.eunoia.application.domain.event.UserRegistered;
import ru.eunoia.application.domain.exception.UserAlreadyExistsException;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.domain.model.Authentication;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.OneTimeToken;
import ru.eunoia.application.domain.model.RefreshToken;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.in.RegisterUseCase;
import ru.eunoia.application.port.out.EmailSenderPort;
import ru.eunoia.application.port.out.EventPublisherPort;
import ru.eunoia.application.port.out.OneTimeTokenRepositoryPort;
import ru.eunoia.application.port.out.PasswordEncoderPort;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.TokenProviderPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

/**
 * Регистрация: проверяем email, хешируем пароль, сохраняем юзера, выдаём токены (REGISTRATION_SUCCESS),
 * шлём письмо подтверждения (EMAIL_VERIFICATION_SENT) и публикуем UserRegistered — по нему service-user
 * заводит профиль.
 */
public class RegisterUseCaseImpl implements RegisterUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final OneTimeTokenRepositoryPort oneTimeTokenRepository;
    private final EmailSenderPort emailSender;
    private final EventPublisherPort eventPublisher;
    private final AuthEventRecorder recorder;
    private final Duration emailVerificationTtl;

    public RegisterUseCaseImpl(UserRepositoryPort userRepository,
                               PasswordEncoderPort passwordEncoder,
                               TokenProviderPort tokenProvider,
                               RefreshTokenRepositoryPort refreshTokenRepository,
                               OneTimeTokenRepositoryPort oneTimeTokenRepository,
                               EmailSenderPort emailSender,
                               EventPublisherPort eventPublisher,
                               AuthEventRecorder recorder,
                               Duration emailVerificationTtl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.oneTimeTokenRepository = oneTimeTokenRepository;
        this.emailSender = emailSender;
        this.eventPublisher = eventPublisher;
        this.recorder = recorder;
        this.emailVerificationTtl = emailVerificationTtl;
    }

    @Override
    public Authentication register(RegisterCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new UserAlreadyExistsException(command.email());
        }

        String passwordHash = passwordEncoder.encode(command.password());
        User saved = userRepository.save(
                User.newlyRegistered(command.email(), command.username(), passwordHash));

        AuthTokens tokens = tokenProvider.generateTokens(saved);
        refreshTokenRepository.save(
                RefreshToken.issued(tokens.refreshToken(), saved.id(), tokens.refreshTokenExpiresAt()));
        recorder.record(saved.id(), AuthEvent.EventType.REGISTRATION_SUCCESS, true);

        sendEmailVerification(saved);
        eventPublisher.publish(
                new UserRegistered(saved.id(), saved.email(), saved.username(), LocalDateTime.now()));

        return new Authentication(saved, tokens);
    }

    /** Одноразовый токен + письмо для подтверждения почты. */
    private void sendEmailVerification(User user) {
        String raw = OneTimeToken.newRawToken();
        oneTimeTokenRepository.save(OneTimeToken.issued(raw, user.id(),
                OneTimeToken.Purpose.EMAIL_VERIFICATION, LocalDateTime.now().plus(emailVerificationTtl)));
        emailSender.sendEmailVerification(user.email(), raw);
        recorder.record(user.id(), AuthEvent.EventType.EMAIL_VERIFICATION_SENT, true);
    }
}
