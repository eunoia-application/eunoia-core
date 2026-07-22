package ru.eunoia.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.eunoia.application.port.in.LoginUseCase;
import ru.eunoia.application.port.in.LogoutUseCase;
import ru.eunoia.application.port.in.RefreshTokenUseCase;
import ru.eunoia.application.port.in.RegisterUseCase;
import ru.eunoia.application.port.out.AuthEventRepositoryPort;
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
                                     RefreshTokenRepositoryPort refreshTokenRepository,
                                     AuthEventRepositoryPort authEventRepository,
                                     @Value("${auth.lockout.max-attempts:5}") int maxAttempts,
                                     @Value("${auth.lockout.lock-duration-minutes:15}") long lockMinutes) {
        return new LoginUseCaseImpl(userRepository, passwordEncoder, tokenProvider, refreshTokenRepository,
                authEventRepository, maxAttempts, Duration.ofMinutes(lockMinutes));
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
