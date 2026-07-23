package ru.eunoia.application.services;

import java.time.Duration;
import ru.eunoia.application.comand.LoginCommand;
import ru.eunoia.application.domain.exception.InvalidCredentialsException;
import ru.eunoia.application.domain.exception.UserNotFoundException;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.domain.model.Authentication;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.RefreshToken;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.in.LoginUseCase;
import ru.eunoia.application.port.out.PasswordEncoderPort;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.TokenProviderPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

/**
 * Логин: сначала доменные правила (активен/не заблокирован), потом пароль. Неверный пароль
 * инкрементит счётчик (при лимите — блок) и пишет LOGIN_FAILED; успешный — сброс и LOGIN_SUCCESS.
 */
public class LoginUseCaseImpl implements LoginUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final AuthEventRecorder recorder;
    private final int maxLoginAttempts;
    private final Duration lockDuration;

    public LoginUseCaseImpl(UserRepositoryPort userRepository,
                            PasswordEncoderPort passwordEncoder,
                            TokenProviderPort tokenProvider,
                            RefreshTokenRepositoryPort refreshTokenRepository,
                            AuthEventRecorder recorder,
                            int maxLoginAttempts,
                            Duration lockDuration) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.recorder = recorder;
        this.maxLoginAttempts = maxLoginAttempts;
        this.lockDuration = lockDuration;
    }

    @Override
    public Authentication login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new UserNotFoundException(command.email()));

        user.assertCanAuthenticate();

        if (!passwordEncoder.matches(command.password(), user.passwordHash())) {
            userRepository.save(user.withFailedLoginAttempt(maxLoginAttempts, lockDuration));
            recorder.record(user.id(), AuthEvent.EventType.LOGIN_FAILED, false);
            throw new InvalidCredentialsException();
        }

        User loggedIn = userRepository.save(user.withSuccessfulLogin());
        AuthTokens tokens = tokenProvider.generateTokens(loggedIn);
        refreshTokenRepository.save(
                RefreshToken.issued(tokens.refreshToken(), loggedIn.id(), tokens.refreshTokenExpiresAt()));
        recorder.record(loggedIn.id(), AuthEvent.EventType.LOGIN_SUCCESS, true);

        return new Authentication(loggedIn, tokens);
    }
}
