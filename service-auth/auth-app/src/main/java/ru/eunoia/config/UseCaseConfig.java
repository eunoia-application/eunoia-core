package ru.eunoia.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.eunoia.application.port.in.LoginUseCase;
import ru.eunoia.application.port.in.RegisterUseCase;
import ru.eunoia.application.port.out.PasswordEncoderPort;
import ru.eunoia.application.port.out.TokenProviderPort;
import ru.eunoia.application.port.out.UserRepositoryPort;
import ru.eunoia.application.services.LoginUseCaseImpl;
import ru.eunoia.application.services.RegisterUseCaseImpl;

/**
 * Composition root for the framework-free application services.
 * auth-logic has no Spring; its use cases are wired here as beans from their ports.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public LoginUseCase loginUseCase(UserRepositoryPort userRepository,
                                     PasswordEncoderPort passwordEncoder,
                                     TokenProviderPort tokenProvider) {
        return new LoginUseCaseImpl(userRepository, passwordEncoder, tokenProvider);
    }

    @Bean
    public RegisterUseCase registerUseCase(UserRepositoryPort userRepository,
                                           PasswordEncoderPort passwordEncoder,
                                           TokenProviderPort tokenProvider) {
        return new RegisterUseCaseImpl(userRepository, passwordEncoder, tokenProvider);
    }
}
