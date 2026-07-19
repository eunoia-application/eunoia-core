package ru.eunoia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.eunoia.application.port.in.LoginUseCase;
import ru.eunoia.application.port.in.LogoutUseCase;
import ru.eunoia.application.port.in.RefreshTokenUseCase;
import ru.eunoia.application.port.in.RegisterUseCase;
import ru.eunoia.application.port.out.PasswordEncoderPort;
import ru.eunoia.application.port.out.RefreshTokenRepositoryPort;
import ru.eunoia.application.port.out.TokenProviderPort;
import ru.eunoia.application.port.out.UserRepositoryPort;
import ru.eunoia.application.services.LoginUseCaseImpl;
import ru.eunoia.application.services.LogoutUseCaseImpl;
import ru.eunoia.application.services.RefreshTokenUseCaseImpl;
import ru.eunoia.application.services.RegisterUseCaseImpl;

/**
 * Composition root: ядро (auth-logic) без Spring, поэтому его use case'ы собираем здесь из портов.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public LoginUseCase loginUseCase(UserRepositoryPort userRepository,
                                     PasswordEncoderPort passwordEncoder,
                                     TokenProviderPort tokenProvider,
                                     RefreshTokenRepositoryPort refreshTokenRepository) {
        return new LoginUseCaseImpl(userRepository, passwordEncoder, tokenProvider, refreshTokenRepository);
    }

    @Bean
    public RegisterUseCase registerUseCase(UserRepositoryPort userRepository,
                                           PasswordEncoderPort passwordEncoder,
                                           TokenProviderPort tokenProvider,
                                           RefreshTokenRepositoryPort refreshTokenRepository) {
        return new RegisterUseCaseImpl(userRepository, passwordEncoder, tokenProvider, refreshTokenRepository);
    }

    @Bean
    public RefreshTokenUseCase refreshTokenUseCase(UserRepositoryPort userRepository,
                                                   TokenProviderPort tokenProvider,
                                                   RefreshTokenRepositoryPort refreshTokenRepository) {
        return new RefreshTokenUseCaseImpl(userRepository, tokenProvider, refreshTokenRepository);
    }

    @Bean
    public LogoutUseCase logoutUseCase(RefreshTokenRepositoryPort refreshTokenRepository) {
        return new LogoutUseCaseImpl(refreshTokenRepository);
    }
}
