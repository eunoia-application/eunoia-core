package ru.eunoia.application.services;

import ru.eunoia.application.comand.LoginCommand;
import ru.eunoia.application.domain.exception.InvalidCredentialsException;
import ru.eunoia.application.domain.exception.UserNotFoundException;
import ru.eunoia.application.domain.model.Authentication;
import ru.eunoia.application.domain.model.AuthTokens;
import ru.eunoia.application.domain.model.RefreshToken;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.in.LoginUseCase;
import ru.eunoia.application.port.out.PasswordEncoderPort;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.TokenProviderPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

/** Логин: находим юзера, сверяем пароль, проверяем доменные правила, выдаём и сохраняем токены. Без Spring. */
public class LoginUseCaseImpl implements LoginUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;
    private final RefreshTokenRepositoryPort refreshTokenRepository;

    public LoginUseCaseImpl(UserRepositoryPort userRepository,
                            PasswordEncoderPort passwordEncoder,
                            TokenProviderPort tokenProvider,
                            RefreshTokenRepositoryPort refreshTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public Authentication login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new UserNotFoundException(command.email()));

        if (!passwordEncoder.matches(command.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        user.assertCanAuthenticate();

        AuthTokens tokens = tokenProvider.generateTokens(user);
        refreshTokenRepository.save(
                RefreshToken.issued(tokens.refreshToken(), user.id(), tokens.refreshTokenExpiresAt()));

        return new Authentication(user, tokens);
    }
}
