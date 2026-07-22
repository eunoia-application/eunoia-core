package ru.eunoia.application.services;

import ru.eunoia.application.comand.RegisterCommand;
import ru.eunoia.application.domain.exception.UserAlreadyExistsException;
import ru.eunoia.application.domain.model.AuthEvent;
import ru.eunoia.application.domain.model.Authentication;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.RefreshToken;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.in.RegisterUseCase;
import ru.eunoia.application.port.out.PasswordEncoderPort;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.TokenProviderPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

/** Регистрация: проверяем email, хешируем пароль, сохраняем юзера, выдаём токены и пишем REGISTRATION_SUCCESS. */
public class RegisterUseCaseImpl implements RegisterUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final AuthEventRecorder recorder;

    public RegisterUseCaseImpl(UserRepositoryPort userRepository,
                               PasswordEncoderPort passwordEncoder,
                               TokenProviderPort tokenProvider,
                               RefreshTokenRepositoryPort refreshTokenRepository,
                               AuthEventRecorder recorder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
        this.recorder = recorder;
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

        return new Authentication(saved, tokens);
    }
}
