package ru.eunoia.application.services;

import ru.eunoia.application.comand.LoginCommand;
import ru.eunoia.application.domain.exception.InvalidCredentialsException;
import ru.eunoia.application.domain.exception.UserNotFoundException;
import ru.eunoia.application.domain.model.Authentication;
import ru.eunoia.application.domain.model.User;
import ru.eunoia.application.port.in.LoginUseCase;
import ru.eunoia.application.port.out.PasswordEncoderPort;
import ru.eunoia.application.port.out.TokenProviderPort;
import ru.eunoia.application.port.out.UserRepositoryPort;

/** Логин: находим юзера, сверяем пароль, проверяем доменные правила, выдаём токены. Без Spring. */
public class LoginUseCaseImpl implements LoginUseCase {

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final TokenProviderPort tokenProvider;

    public LoginUseCaseImpl(UserRepositoryPort userRepository,
                            PasswordEncoderPort passwordEncoder,
                            TokenProviderPort tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Authentication login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new UserNotFoundException(command.email()));

        if (!passwordEncoder.matches(command.password(), user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        user.assertCanAuthenticate();

        return new Authentication(user, tokenProvider.generateTokens(user));
    }
}
